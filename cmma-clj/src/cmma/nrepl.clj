(ns cmma.nrepl
  (:require [clojure.java.io :as io]
            [clojure.tools.nrepl.ack :as nrepl-ack]
            [clojure.tools.nrepl.server :as nrepl-server]
            [reply.main :as reply]
            [cmma.project]
            [cmma.dev :as dev]))

(defn port*
  ([]
   (+ 5000 (rand-int 4999)))
  ([repl-options port-kw]
   (if (integer? (repl-options port-kw))
     (repl-options port-kw)
     (do
       (println (str port-kw " was not an integer.  Falling back..."))
       (port*)))))

(defn bind
  ([]
   "127.0.0.1")
  ([repl-options]
   (if (string? (:bind repl-options))
     (:bind repl-options)
     (bind))))

(defn repl-settings
  ([]
   {:port (port*)
    :bind (bind)})
  ([project]
   (let [{:keys [nrepl-options]} project]
     (merge
       (dissoc nrepl-options :reply)
       {:port (port* nrepl-options :port)
        :bind (bind nrepl-options)}))))

(defn server
  [repl-settings]
  (let [{:keys  [port bind transport-fn handler ack-port greeting-fn]} repl-settings
        nrepl (nrepl-server/start-server :port port
                                         :bind bind
                                         :transport-fn transport-fn
                                         :handler handler
                                         :ack-port ack-port
                                         :greeting-fn greeting-fn)]
    (spit ".nrepl-port" (:port nrepl))
    (println "Repl'd on port:" (:port nrepl))
    nrepl))

(defn -main [& args]
  (let [prj (cmma.project/project (first args)) ;; `project` is nil-safe; defaults to "project.edn"
        nrepl (server (repl-settings prj))]
    (if (get-in prj [:nrepl-options :reply])
      (reply/-main)
      (do (println "Server running; Hit a key to shutdown:")
          (read-line)))
    (println "Shutting the nrepl server down...")
    (try
      (io/delete-file ".nrepl-port")
      (catch Throwable t
        (println "Error: could not delete .nrepl-port")))
    (nrepl-server/stop-server nrepl)
    (shutdown-agents)))

