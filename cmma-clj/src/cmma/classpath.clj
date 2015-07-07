(ns cmma.classpath
  (:require [clojure.set :as cset]
            [clojure.string :as cstr]
            [cemerick.pomegranate.aether :as aether]
            [cemerick.pomegranate :as pomegranate]
            [pedantic.core :as pedantic]
            [cmma.deps :as deps]
            [cmma.deps.impl]
            [cmma.project])
  (:import (java.util.jar JarFile)
           (org.sonatype.aether.graph Exclusion)
           (org.sonatype.aether.resolution DependencyResolutionException)))

(defn id-map [sym]
  {:group-id (or (namespace sym)
                     (name sym))
   :artifact-id (name sym)})

(defn maven-vec->map [v]
  (let [[coordinates version exclude? exclusions] v
        result (id-map coordinates)]
    (cond-> result
      true (assoc :version version)
      (and (= :exclusions exclude?) exclusions) (assoc :exclusions (mapv id-map exclusions)))))

(defn split-maven-deps [deps]
  (cset/rename-keys (group-by vector? deps) {true :maven false :other}))

(defn resolve-deps!
  ([deps]
   (resolve-deps! deps {}))
  ([deps options]
  (let [{:keys [maven other]} (split-maven-deps deps)
        {:keys [repositories local-repo offline? mirrors path]} options
        ranges (atom [])
        overrides (atom [])]
    (try
      {:maven (aether/resolve-dependencies
               :coordinates maven
               :repositories repositories
               :local-repo local-repo
               :offline? offline?
               :mirrors mirrors
               :repository-session-fn #(-> %
                                           aether/repository-session
                                           (pedantic/use-transformer ranges
                                                                     overrides)))
       :ranges @ranges
       :overrides @overrides
       :other (mapv #(deps/resolve-dep % path) other)}
      (catch DependencyResolutionException e
        (println "There is most likely an error/typo in your
                 dependencies/dev-dependencies.\n")
        (.printStackTrace e)
        (throw (ex-info "Could not resolve dependencies" {:suppress-msg true
                                                           :exit-code 1} e)))))))

;; Pedantic stuff - from Leiningen
(defn- group-artifact [artifact]
  (if (= (.getGroupId artifact)
         (.getArtifactId artifact))
    (.getGroupId artifact)
    (str (.getGroupId artifact)
         "/"
         (.getArtifactId artifact))))

(defn- dependency-str [dependency & [version]]
  (if-let [artifact (and dependency (.getArtifact dependency))]
    (str "["
         (group-artifact artifact)
         " \"" (or version (.getVersion artifact)) "\""
         (if-let [classifier (.getClassifier artifact)]
           (if (not (empty? classifier))
             (str " :classifier \"" classifier "\"")))
         (if-let [extension (.getExtension artifact)]
           (if (not= extension "jar")
             (str " :extension \"" extension "\"")))
         (if-let [exclusions (seq (.getExclusions dependency))]
           (str " :exclusions " (mapv (comp symbol group-artifact)
                                      exclusions)))
         "]")))

(defn- message-for [path & [show-constraint?]]
  (->> path
       (map #(dependency-str (.getDependency %) (.getVersionConstraint %)))
       (remove nil?)
       (interpose " -> ")
       (apply str)))

(defn- message-for-version [{:keys [node parents]}]
  (message-for (conj parents node)))

(defn- exclusion-for-range [node parents]
  (if-let [top-level (second parents)]
    (let [excluded-artifact (.getArtifact (.getDependency node))
          exclusion (Exclusion. (.getGroupId excluded-artifact)
                      (.getArtifactId excluded-artifact) "*" "*")
          exclusion-set (into #{exclusion} (.getExclusions
                                             (.getDependency top-level)))
          with-exclusion (.setExclusions (.getDependency top-level) exclusion-set)]
      (dependency-str with-exclusion))
    ""))

(defn- message-for-range [{:keys [node parents]}]
  (str (message-for (conj parents node) :constraints) "\n"
       "Consider using "
       (exclusion-for-range node parents) "."))

(defn- exclusion-for-override [{:keys [node parents]}]
  (exclusion-for-range node parents))

(defn- message-for-override [{:keys [accepted ignoreds ranges]}]
  {:accepted (message-for-version accepted)
   :ignoreds (map message-for-version ignoreds)
   :ranges (map message-for-range ranges)
   :exclusions (map exclusion-for-override ignoreds)})

(defn graph->dependency-paths [deps-graph]
  (->> deps-graph
       (aether/dependency-files)
       (filter #(re-find #"\.(jar|zip)$" (.getName %)))
       (map #(.getAbsolutePath %))))

;; Project entry

(defn all-dependencies [project]
  (into (:dependencies project)
        (:dev-dependencies project)))

(defn resolve-project-deps! [project]
  (let [{:keys [maven ranges overrides other]
         :as resolved-deps}  (resolve-deps!
                               (all-dependencies project)
                               (merge {}
                                      (:deps-settings project)
                                      (select-keys project [:repositories :path])))]
    (when (seq ranges)
      (throw (ex-info (apply str "Version ranges detected:\n"
                             (map message-for-range ranges))
                      {:ranges ranges})))
    (when (seq overrides)
      (throw (ex-info (apply str "Version overrides detected:\n"
                             (map message-for-override overrides))
                      {:overrides overrides})))
    resolved-deps))

(defn prepend-proj-path [project k-coll]
  (let [{:keys [path]} project]
    (map #(str path "/" %) (get project k-coll []))))

(defn classpath [project]
  ;; Technically we could just comb through the edn file and make the .m2 strings,
  ;;  but then we'd miss the chance to bail on version ranges/overrides
  (let [initial-deps (resolve-project-deps! project)
        other-deps (filter #(satisfies? deps/Dependency %) (:other initial-deps))
        ;; Group all viable :dependencies and :repositories of transitive deps (ala Git deps)
        transitive-projects (reduce (fn [acc-map proj-map]
                                      (let [tdeps (vec (distinct (into (:dependencies acc-map)
                                                                       (:dependencies proj-map))))
                                            treps (vec (distinct (into (:repositories acc-map)
                                                                       (:repositories proj-map))))]
                                        (assoc acc-map
                                               :dependencies tdeps
                                               :repositories treps)))
                                    (map (fn [path]
                                           (select-keys (cmma.project/project path)
                                                        [:dependencies :repositories]))
                                         (keep #(deps/transitive-path % project) other-deps)))
        adjusted-proj (assoc project
                             :dependencies (into (:dependencies project) (:dependencies transitive-projects []))
                             :repositories (into (:repositories project) (:repositories transitive-projects [])))
        deps (resolve-project-deps! adjusted-proj)
        dep-paths (graph->dependency-paths (:maven deps))]
    (concat (prepend-proj-path project :test-paths )
            (prepend-proj-path project :source-paths)
            (prepend-proj-path project :resource-paths)
            (mapcat #(deps/classpath-strs % project) other-deps)
            dep-paths
            ["."])))

(defn classpath-str
  ([project] (classpath-str project ":"))
  ([project separator]
   (if (empty? project)
     ;(println "You're trying to create a classpath, but no project file was found;")
     ;(println "Defaulting to just the Clojure 1.7 jar...")
     "$HOME/.m2/repository/org/clojure/clojure/1.7.0/clojure-1.7.0.jar:."
     (cstr/join separator (classpath project)))))

(defn -main [& args]
  (println (classpath-str (cmma.project/project))))

