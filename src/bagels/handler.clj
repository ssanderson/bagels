(ns bagels.handler
  (:require [bagels.util :refer [format-map]]
            [cheshire.core :as json]
            [clj-http.client :as client]
            [clojure.pprint :refer :all]
            [clojure.string :refer [join split]]
            [compojure.core :refer [defroutes GET ANY]]
            [compojure.route :as route]
            [crypto.random :as random]
            [environ.core :refer [env]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.codec :refer [base64-decode form-encode]]
            [ring.util.response :as response]))

(defn url-join
  [& path-parts]
  (join "/" path-parts))

(defn- oauth-callback-url
  [request callback-path]
  (let [{:keys [scheme server-name server-port]} request]
    (format "%1$s://%2$s:%3$d%4$s"
            (name scheme) server-name server-port callback-path)))

(defn- oauth-params
  [client-id callback-url state]
  {:response_type "code"
   :client_id client-id
   :redirect_uri callback-url
   :scope "email"
   :state state})

(defn- new-random-token []
  (random/base64 60))

(defn- set-oauth-session-state
  [response state]
  (assoc-in response [:session ::oauth-state] state))

(defn- get-oauth-session-state
  [session]
  (::oauth-state session))

(defn get-user
  [request]
  (get-in request [:session ::user]))

(defn set-user
  [response user]
  (assoc-in response [:session ::user] user))

(def google-oauth-redirect-url "https://accounts.google.com/o/oauth2/v2/auth")
(def google-token-exchange-url "https://www.googleapis.com/oauth2/v4/token")
(def google-user-email-url "https://www.googleapis.com/plus/v1/people/me")

(defn oauth-redirect
  "Redirect the user to Google's OAuth login page.

  Upon successful login, the user's browser will
  load callback-url with state as a query parameter."
  [client-id callback-url state]
  (let [params (oauth-params client-id callback-url state)
        response (response/redirect
                  (join "?" [google-oauth-redirect-url (form-encode params)]))]
    (set-oauth-session-state response state)))

(defn- forbidden
  "Helper for making a 403 FORBIDDEN response"
  [body]
  {:status 403 :headers {} :body body})

(defn b64-decode
  "Base64 decode a string and convert it from bytes[] back to a Java string."
  [s]
  (String. (base64-decode s) "UTF-8"))

(defn decode-json-web-token
  "Decode a JSON Web Token. Does not validate the token signature.

  See https://jwt.io/ for details."
  [token]
  (let [[header payload signature] (map b64-decode (split token #"\."))]
    (json/decode payload)))

(defn exchange-oauth-code-for-user-email
  [code client-id client-secret redirect-uri]
  (let [response (client/post google-token-exchange-url
                              {:form-params
                               {:code code
                                :client_id client-id
                                :client_secret client-secret
                                :redirect_uri redirect-uri
                                :grant_type "authorization_code"}})
        response-body (json/parse-string (:body response))
        decoded-token (decode-json-web-token (response-body "id_token"))]
    (decoded-token "email")))

(defn oauth-callback
  [request]
  (let [{session :session {state :state code :code} :params} request]
    (if (not= (get-oauth-session-state session) state)
      (forbidden "Invalid Session State")
      (let [{:keys [oauth-client-id oauth-client-secret]} env
            callback-url (oauth-callback-url request (:uri request))
            user-email (exchange-oauth-code-for-user-email code
                                                           oauth-client-id
                                                           oauth-client-secret
                                                           callback-url)]
        (set-user (response/redirect "/") user-email)))))

(defn wrap-oauth-login
  "Ring middleware that performs logins with Google OAuth."
  [app-handler callback-handler callback-path]
  (fn [request]
    (let [user (get-user request)]
      (cond (= (:uri request) callback-path) (callback-handler request)
            user (app-handler request)
            :else (oauth-redirect (:oauth-client-id env)
                                  (oauth-callback-url request callback-path)
                                  (new-random-token))))))
(defn home
  [request]
  (format "Hello %s!" (get-user request)))

(defroutes app-routes
  (GET "/" request (home request))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-oauth-login oauth-callback "/oauth_callback")
      (wrap-defaults site-defaults)))
