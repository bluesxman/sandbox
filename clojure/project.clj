(defproject sandbox "0.1.0-SNAPSHOT"
  :description "Catch-all project "
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [compojure "1.1.6"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [org.apache.commons/commons-math3 "3.0"]
                 [stopwatch "0.1.0"]]
  :source-paths ["src"]
  :java-source-paths ["../java/src"]
  :javac-options ["-target" "1.8" "-source" "1.8"])
