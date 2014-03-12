(defproject sandbox "0.1.0-SNAPSHOT"
  :description "Catch-all project "
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [compojure "1.1.6"]
                 [ring/ring-jetty-adapter "1.2.1"]]
  :source-paths ["src"]
  :java-source-paths ["../java/src"]
  :javac-options ["-target" "1.8" "-source" "1.8"])
