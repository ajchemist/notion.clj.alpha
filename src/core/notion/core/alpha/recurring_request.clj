(ns notion.core.alpha.recurring-request
  (:require
   [notion.core.alpha :as notion]
   [ajchemist.clj-http.transduce :as t]
   ))


(def ^:dynamic ^:private *notion-token*)


(defn query-database
  [http-params database-id form-params callback]
  (t/recurring-request
    (fn [params]
      (notion/query-database http-params database-id params))
    (merge {:page_size 25} form-params)
    (fn
      [_idx params {:strs [next_cursor] :as response}]
      (assoc params :start_cursor next_cursor))
    (comp
      notion/xf-has-more
      notion/xf-results
      cat)
    conj
    callback))


(defn retrieve-page-property
  [http-params page-id property-id query-params callback]
  (t/recurring-request
    (fn [params]
      (notion/retrieve-page-property http-params page-id property-id params))
    (merge {:page_size 25} query-params)
    (fn
      [_idx params {:strs [next_cursor] :as _response}]
      (assoc params :start_cursor next_cursor))
    (comp
      notion/xf-has-more
      notion/xf-results
      cat)
    conj
    callback))


(comment
  (defn retrieve-children
    [params block-id]
    (t/recurring-request
      #(notion/retrieve-children % block-id)
      (merge params {:page_size 100})
      (fn
        [_idx params {:strs [next_cursor] :as response}]
        (assoc params :start_cursor next_cursor))
      (comp
        notion/xf-has-more
        notion/xf-results
        cat)
      conj
      (fn [results]
        (reduce
          (fn self
            [ret {:strs [has_children id] :as block}]
            (let [block' (select-keys block ["object" "type" (get block "type")])]
              (if has_children
                (conj ret (assoc block' (get block "type") ))
                (conj ret block'))))
          []
          results))))
  )
