(ns cmma.literals)

(defrecord Git [^String repo ^String point on-classpath recursive-deps])

(defmethod print-method Git [t ^java.io.Writer w]
  (.write w (str "#cmma/git" (into {} t))))

(defn deps-git
  ""
  [form]
  {:pre [(map? form)
         (= 3 (count (select-keys form [:repo :point :on-classpath])))
         (vector? (:on-classpath form))
         (every? string? (:on-classpath form))
         (if (contains? form :recursive-deps)
           (instance? java.lang.Boolean (:recursive-deps form))
           true)]}
  (map->Git (merge {:recursive-deps false} form)))

