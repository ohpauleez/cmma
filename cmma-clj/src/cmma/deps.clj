(ns cmma.deps)

(defprotocol Dependency
  (resolve-dep [t])
  (classpath-strs [t]))

