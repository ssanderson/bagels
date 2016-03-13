(defproject bagels "0.1.0"
  :description "A web app for expressing bagel preferences."
  :url "https://github.com/quantopian/bagels"
  :min-lein-version "2.0.0"
  :dependencies [[com.h2database/h2 "1.4.191"]
                 [compojure "1.5.0"]
                 [environ "1.0.2"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [ring/ring-defaults "0.2.0"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.0.2"]]
  :ring {:handler bagels.handler/app}
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [hiccup "1.0.5"]
                                  [ring/ring-mock "0.3.0"]]
                   :env {:bagel-db-path "~/.bagels/dev.db"}}})

