(ns notion.core.alpha
  (:require
   [clj-http.client :as http]
   [user.ring.alpha :as user.ring]
   )
  (:import
   java.time.Instant
   ))


(set! *warn-on-reflection* true)


(def ^:const +origin+ "https://api.notion.com")


(def ^{:arglists '([request] [request respond raise])}
  client
  (-> http/request
    (user.ring/wrap-meta-response)
    (user.ring/wrap-transform-request
      (fn [params]
        (cond-> params
          (map? (:query-params params)) (assoc-in [::saved-params :query-params] (:query-params params))
          (map? (:form-params params)) (assoc-in [::saved-params :form-params] (:form-params params)))))
    (user.ring/wrap-transform-request
      (fn [params]
        (-> params
          (update :content-type
            #(or % :json))
          (update :cookie-policy
            #(or % :none)))))
    (user.ring/wrap-transform-request
      (fn [params]
        (cond-> params
          (find params :notion/token)
          (->
            (update-in [:headers "Authorization"] #(or % (str "Bearer " (:notion/token params))))
            (update-in [:headers "Notion-Version"] #(or % (:notion/version params "2022-06-28")))))))))


;; * xform


(def xf-has-more
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result {:strs [has_more] :as response}]
       (let [result (rf result response)]
         (if has_more
           result
           (ensure-reduced result)))))))


(def xf-results
  (fn [rf]
    (fn
      ([] (rf))
      ([acc] (rf acc))
      ([acc {:strs [results] :as response}]
       (rf acc results)))))


(defn xf-take-while-since-updated
  [^Instant since]
  (fn [rf]
    (fn
      ([] (rf))
      ([acc] (rf acc))
      ([acc response]
       (let [response' (update response "results"
                         #(take-while
                            (fn [{:strs [last_edited_time]}]
                              (.isBefore since (Instant/parse last_edited_time)))
                            %))
             acc'      (rf acc response')]
         (if (< (count (get response' "results")) (count (get response "results")))
           (ensure-reduced acc')
           acc'))))))


(defn xf-since-updated
  [^Instant since]
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result {:strs [results] :as response}]
       (let [result (rf result response)]
         (if (some
               (fn [{:strs [last_edited_time]}]
                 (.isAfter since (Instant/parse last_edited_time)))
               results)
           (ensure-reduced result)
           result))))))


;; * Users


(defn list-users
  [params]
  (client
    (merge
      params
      {:url    (str +origin+ "/v1/users")
       :method :get
       :as     :json-string-keys})))


(defn retrieve-user
  [params user-id]
  (client
    (merge
      params
      {:url    (str +origin+ "/v1/users/" user-id)
       :method :get
       :as     :json-string-keys})))


(defn retrieve-bot-user
  [params]
  (client
    (merge
      params
      {:url    (str +origin+ "/v1/users/me")
       :method :get
       :as     :json-string-keys})))


;; * Database


(defn query-database
  [params database-id form-params]
  (client
    (-> params
      (merge
        {:url    (str +origin+ "/v1/databases/" database-id "/query")
         :method :post
         :as     :json-string-keys})
      (update :form-params merge form-params))))


(defn create-database
  [params]
  (client
    (-> params
      (assoc
        :url    (str +origin+ "/v1/databases")
        :method :post
        :as     :json-string-keys))))


(defn update-database
  [params database-id form-params]
  (client
    (-> params
      (assoc
        :url    (str +origin+ "/v1/databases/" database-id)
        :method :patch
        :as     :json-string-keys)
      (update :form-params merge form-params))))


(defn retrieve-database
  [params database-id]
  (client
    (merge
      params
      {:url    (str +origin+ "/v1/databases/" database-id)
       :method :get
       :as     :json-string-keys})))


;; * Page


(defn retrieve-page
  [params page-id]
  (client
    (merge
      params
      {:url    (str +origin+ "/v1/pages/" page-id)
       :method :get
       :as     :json-string-keys})))


(defn retrieve-page-property
  [params page-id property-id]
  (client
    (merge
      params
      {:url    (str +origin+ "/v1/pages/" page-id "/properties/" property-id)
       :method :get
       :as     :json-string-keys})))


(defn create-page
  [params parent properties & {:keys [children icon cover]}]
  (client
    (-> (merge
          params
          {:url    (str +origin+ "/v1/pages")
           :method :post
           :as     :json-string-keys})
      (update-in [:form-params :parent] #(or % parent))
      (update-in [:form-params :properties] #(or % properties))
      (cond->
        (seq children) (update-in [:form-params :children] #(or % children))
        (some? icon) (update-in [:form-params :icon] #(or % icon))
        (some? cover) (update-in [:form-params :cover] #(or % cover))))))


(defn update-page
  [params page-id form-params]
  (client
    (-> params
      (assoc
        :url    (str +origin+ "/v1/pages/" page-id)
        :method :patch
        :as     :json-string-keys)
      (update :form-params merge form-params))))


;; * Block


(defn retreive-children
  [params block-id]
  (client
    (merge
      params
      {:url    (str +origin+ "/v1/blocks/" block-id "/children")
       :method :get
       :as     :json-string-keys})))


(set! *warn-on-reflection* false)
