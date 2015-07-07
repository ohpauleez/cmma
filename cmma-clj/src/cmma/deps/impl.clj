(ns cmma.deps.impl
  (:require [cmma.deps :as deps]
            [cmma.literals]
            [cmma.jgit :as jgit]
            [cmma.git :as git]
            [clojure.java.io :as io])
  (:import (cmma.literals Git)))

(def git-deps-path ".cmma/git-deps/")

(extend-protocol deps/Dependency

  Git
  (resolve-dep [t project-path]
    (let [git-dir-path (str project-path "/" git-deps-path)
          git-dir (io/file git-dir-path)
          repo-dir-path (str git-dir-path (jgit/repo-name (:repo t)))
          repo-dir (io/file repo-dir-path)]
      (when-not (.exists repo-dir)
        (.mkdirs git-dir)
        (git/clone! git-dir-path (:repo t)))
      (git/reset-cmma-mark! repo-dir-path (:point t "HEAD"))
      t))

  (classpath-strs [t project]
    (mapv #(str (:path project) "/" git-deps-path (jgit/repo-name (:repo t)) "/" %)
          (:on-classpath t)))

  (transitive-path [t project]
    (if (:recursive-deps t)
      (str (:path project) "/" git-deps-path (jgit/repo-name (:repo t)) "/")
      nil)))

