(defproject braveclojure "0.1.0-SNAPSHOT"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.490"]]
  :plugins [[lein-cljfmt "0.6.3"]]
  :main ^:skip-aot playsync.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
