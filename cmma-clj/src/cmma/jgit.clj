(ns cmma.jgit
  (:require [clojure.java.io :as io])
  (:import (org.eclipse.jgit.api Git
                                 CloneCommand)
           (org.eclipse.jgit.lib Repository)
           (org.eclipse.jgit.storage.file FileRepositoryBuilder)
           (org.eclipse.jgit.transport URIish)
           (org.eclipse.jgit.api.errors GitAPIException
                                        InvalidRemoteException
                                        TransportException)
           (java.io File)))

;; NOTE:
;; ------------------
;;       This isn't used internally in CMMA; Git is shelled to directly.
;;       It's currently sitting here as an artifact if I want to come back to it.
;;       Feel free to steal any of this.

(defn repo-name
  "Given a Git URL,
  return the string of the repo name.
  For example: http://some/path/thisrepo.git => thisrepo"
  [url]
  (-> (URIish. url)
      (.getHumanishName)))

(defn path->repo
  "Given a file path of a local git repo,
  return its Repository object."
  [^File git-path]
  (-> (FileRepositoryBuilder.)
      (.setGitDir git-path)
      (.readEnvironment)
      (.findGitDir)
      (.build)))

(defn git
  "Given a Repository object,
  return its Git Command object"
  [^Repository git-repo]
  (Git. git-repo))

(defn repository
  "Given a Git Command object,
  return its Repository object"
  [^Git git-obj]
  (.getRespository git-obj))

(defn clone
  "Given a URL string of a Git Repo, and optionally a local directory destination,
  clone the repo in the destination, and return an activated Git Command object.
  If no destination is specified, the repo name from the URL is used."
  ([url]
   (clone url (repo-name url)))
  ([url dest-dir]
   (-> (Git/cloneRepository)
       (.setURI url)
       (.setDirectory (io/as-file dest-dir))
       (.call))
   (git (path->repo dest-dir))))


;(defn pull
;  ""
;  ([git-repo]
;   (pull git-repo "origin" "master"))
;  ([git-repo remote branch]
;   (let [])))

;(defn create-branch
;  [git-repo branch-name])

(defn delete-branches
  ""
  ([^Git repo branch-names]
   (delete-branches repo branch-names false))
  ([^Git repo branch-names force?]
   (-> repo
       (.branchDelete)
       (.setBranchNames (into-array String (if (string? branch-names)
                                             [branch-names]
                                             branch-names)))
       (.setForce force?)
       (.call))))

(defn checkout
  ""
  ([^Git repo branch-name]
   (checkout repo branch-name false false ""))
  ([^Git repo branch-name create-branch?]
   (checkout repo branch-name create-branch? false ""))
  ([^Git repo branch-name create-branch? force?]
   (checkout repo branch-name create-branch? force? ""))
  ([^Git repo branch-name create-branch? force? ^String start-point]
   (cond-> repo
     true (.checkout)
     true (.setName branch-name)
     true (.setCreateBranch create-branch?)
     true (.setForce force?)
     (seq start-point) (.setStartPoint start-point)
     true (.call))))

(defn reset-cmma-mark
  ""
  [^Git git-repo git-hash]
  (checkout git-repo "master")
  (delete-branches git-repo "cmma-mark")
  (checkout git-repo "cmma-mark" true false git-hash))

