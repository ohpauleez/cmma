(ns cmma.deps)

(defprotocol Dependency
  (resolve-dep [t project-path])
  (classpath-strs [t project]))

