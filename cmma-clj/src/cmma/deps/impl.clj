(ns cmma.deps.impl
  (:require [cmma.deps :as deps]
            [cmma.literals])
  (:import (cmma.literals Git)))

(extend-protocol deps/Dependency

  Git
  (resolve-dep [t] t) ;; for now, just no-op this
  (classpath-strs [t]
    (map #(str "REPO-PATH-HERE/" %) (:on-classpath))))

