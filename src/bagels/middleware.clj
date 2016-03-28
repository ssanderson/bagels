(ns bagels.middleware
  (:require [cheshire.core :as json]
            [clj-http.client :as client]
            [clojure.string :refer [join split]]
            [crypto.random :as random]
            [environ.core :refer [env]]
            [ring.util.codec :refer [base64-decode form-encode]]
            [ring.util.response :as response]))

(def google-oauth-redirect-url "https://accounts.google.com/o/oauth2/v2/auth")
(def google-token-exchange-url "https://www.googleapis.com/oauth2/v4/token")
(def default-oauth-callback-path "/oauth_callback")

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

(defn- set-user
  [response user]
  (assoc-in response [:session ::user] user))

(defn get-user
  "Get the user associated with the request session."
  [request]
  (get-in request [:session ::user]))

(defn- forbidden
  "Helper for making a 403 FORBIDDEN response"
  [body]
  {:status 403 :headers {} :body body})

(defn- b64-decode
  "Base64 decode a string and convert it from bytes[] back to a Java string."
  [s]
  (String. (base64-decode s) "UTF-8"))

(defn- decode-json-web-token
  "Decode a JSON Web Token. Does not validate the token signature.

  See https://jwt.io/ for details."
  [token]
  (let [[header payload signature] (map b64-decode (split token #"\."))]
    (json/decode payload)))

(defn- oauth-callback-url
  [request callback-path]
  (let [{:keys [scheme server-name server-port]} request]
    (format "%1$s://%2$s:%3$d%4$s"
            (name scheme) server-name server-port callback-path)))

(defn- exchange-oauth-code-for-user-email
  "Exchange a code received in an OAuth callback for the user's email."
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

(defn- default-oauth-callback-handler
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

(defn oauth-redirect
  "Redirect the user to Google's OAuth login page.

  Upon successful login, the user's browser will
  load callback-url with state as a query parameter."
  [client-id callback-url state]
  (let [params (oauth-params client-id callback-url state)
        response (response/redirect
                  (join "?" [google-oauth-redirect-url (form-encode params)]))]
    (set-oauth-session-state response state)))

(defn wrap-oauth-login
  "Ring middleware that performs logins with Google OAuth."
  ([app-handler]
   (wrap-oauth-login app-handler
                     default-oauth-callback-path
                     default-oauth-callback-handler))
  ([app-handler callback-path callback-handler]
   (fn [request]
     (let [user (get-user request)]
       (cond (= (:uri request) callback-path) (callback-handler request)
             user (app-handler request)
             :else (oauth-redirect (:oauth-client-id env)
                                   (oauth-callback-url request callback-path)
                                   (new-random-token)))))))

