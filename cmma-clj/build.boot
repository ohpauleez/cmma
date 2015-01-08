(require '[clojure.edn :as edn])

(def proj (edn/read-string (slurp "project.edn")))

(set-env!
  :source-paths (set (:source-paths proj))
  :resource-path (set (:resource-paths proj))
  :repositories (vec (:repositories proj))
  :dependencies (:dependencies proj))

