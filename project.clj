(defproject org.nfrac/nab-comportex "0.1.1-SNAPSHOT"
  :description "Support for running Comportex on Numenta Anomaly Benchmark"
  :url "https://github.com/floybix/nab-comportex"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.nfrac/comportex "0.0.15-SNAPSHOT"]
                 [org.clojure/data.priority-map "0.0.7"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.csv "0.1.3"]]

  :repl-options {;:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                 :init-ns org.numenta.sanity.comportex.launchpad}

  :profiles {:dev {:dependencies [[org.numenta/sanity "0.0.14-SNAPSHOT"]]}})
