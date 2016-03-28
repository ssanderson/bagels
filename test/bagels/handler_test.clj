;; (ns bagels.handler-test
;;   (:require [clojure.test :refer [deftest testing is]]
;;             [ring.mock.request :as mock]
;;             [bagels.handler :refer [app wrap-oauth-login]]))

;; ;; (defn- oauth-callback
;; ;;   [state code]
;; ;;   (mock/request :get "/oauth_callback" {:state state :code code}))

;; (deftest test-app
;;   (testing "login"
;;     (let [response (app (oauth-callback "mock_state" "mock_code"))]
;;       (is (= (keys response) [:as]))
;;       (is (= (:status response) 200)))))

;;   ;; (testing "main route"
;;   ;;   (let [response (app (mock/request :get "/"))]
;;   ;;     (is (= (:status response) 200))
;;   ;;     (is (= (:body response) "<h1>Hello World</h1>"))))

;;   ;; (testing "not-found route"
;;   ;;   (let [response (app (mock/request :get "/invalid"))]
;;   ;;     (is (= (:status response) 404)))))
