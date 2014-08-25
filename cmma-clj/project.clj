(defproject ohpauleez/cmma "0.1.0-SNAPSHOT"
  :description "Clojure + Make Managed Applications"
  :url "https://github.com/ohpauleez/cmma"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [reply "0.3.4" :exclusions [[ring/ring-core]]]
                 [com.cemerick/pomegranate "0.3.0" :exclusions [[org.apache.httpcomponents/httpclient]
                                                                [org.codehaus.plexus/plexus-utils]
                                                                [commons-codec]]]
                 [pedantic "0.2.0"]
                 [org.eclipse.jgit/org.eclipse.jgit.java7 "3.4.1.201406201815-r"]

                 ;; Patch up deps
                 [org.apache.httpcomponents/httpclient "4.1.3"]
                 [org.codehaus.plexus/plexus-utils "3.0"]
                 [commons-codec "1.5"]]
  :global-vars  {*warn-on-reflection* true
                 *assert* true}
  :pedantic? :abort)

