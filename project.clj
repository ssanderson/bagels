(defproject bagels "0.1.0"
  :description "A web app for expressing bagel preferences."
  :url "https://github.com/quantopian/bagels"
  :min-lein-version "2.0.0"
  :dependencies [[cheshire "5.5.0"] ;; JSON
                 [clj-http "2.1.0"] ;; HTTP
                 [com.h2database/h2 "1.4.191"] ;; Database
                 [compojure "1.5.0"] ;; URL Routing
                 [crypto-random "1.2.0"] ;; Crytographically-secure RNG.
                 [environ "1.0.2"] ;; OS Environment with Niceties for Dev
                 [hiccup "1.0.5"] ;; HTML
                 [javax.servlet/servlet-api "2.5"] ;; Web Server
                 [org.clojure/clojure "1.7.0"] ;; Clojure
                 [org.clojure/java.jdbc "0.4.2"]  ;; Database Drivers
                 [ring/ring-core "1.4.0"]  ;; Request/Response Format
                 [ring/ring-defaults "0.2.0"]] ;; Helpers for Ring
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.0.2"]]
  :ring {:handler bagels.handler/app}
  :profiles {:dev {:dependencies [[ring/ring-mock "0.3.0"]]
                   :env {:bagel-db-path "~/.bagels/dbs/dev"
                         :oauth-client-id
                         "375163386725-spv53h3vdnid8v66ga1e8ooimluhcbdd.apps.googleusercontent.com"
                         :oauth-client-secret "lqw2Mp7Ld3jfk947zDf1BjWH"}}})
