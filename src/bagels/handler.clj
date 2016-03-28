(ns bagels.handler
  (:require [bagels.middleware :refer [get-user wrap-oauth-login]]
            [bagels.util :refer [format-map]]
            [compojure.core :refer [defroutes GET ANY]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defn home
  [request]
  (format "Hello %s!" (get-user request)))

(defroutes app-routes
  (GET "/" request (home request))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-oauth-login)
      (wrap-defaults site-defaults)))
