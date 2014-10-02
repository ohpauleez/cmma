(ns cmma.git
  (:require [clojure.java.shell :as shell :refer [sh]]))

(defn git!
  [repo-dir & args]
  (if (seq repo-dir)
    (shell/with-sh-dir repo-dir
      (apply sh (conj args "git")))
    (apply sh (conj args "git"))))

(defn clone!
  ""
  ([parent-dir url]
   (git! parent-dir "clone" url))
  ([parent-dir url dest-dir]
   (git! parent-dir "clone" url dest-dir)))

(defn branches
  ""
  [repo-dir]
  (git! repo-dir "branch" "-a" "-vv"))

(defn delete-branches!
  ""
  ([repo-dir branch-names]
   (delete-branches! repo-dir branch-names false))
  ([repo-dir branch-names force?]
   (apply git! repo-dir (into ["branch" (if force? "-D" "-d")]
                              (if (string? branch-names)
                                [branch-names]
                                branch-names)))))

(defn checkout!
  ""
  ([repo-dir branch-name]
   (checkout! repo-dir branch-name false false ""))
  ([repo-dir branch-name create-branch?]
   (checkout! repo-dir branch-name create-branch? false ""))
  ([repo-dir branch-name create-branch? force?]
   (checkout! repo-dir branch-name create-branch? force? ""))
  ([repo-dir branch-name create-branch? force? start-point]
   (apply git! repo-dir (cond-> ["checkout"]
                          create-branch? (conj "-B")
                          force? (conj "-f")
                          true (conj branch-name)
                          (seq start-point) (conj start-point)))))

(defn reset-cmma-mark!
  ""
  [repo-dir start-point]
  (checkout! repo-dir "master")
  (git! repo-dir "fetch")
  (git! repo-dir "pull" "origin" "master")
  (checkout! repo-dir "cmma-mark" true false start-point))

(comment
  (branches "/Users/paul/scratch/boot")
  ;(delete-branches! "/Users/paul/scratch/boot" "cmma-mark")
  (reset-cmma-mark! "/Users/paul/scratch/boot" "HEAD")
  (reset-cmma-mark! "/Users/paul/scratch/boot" "e9a2064aacc93d6cb32895ff132e5fb28ca935e3")
  )

