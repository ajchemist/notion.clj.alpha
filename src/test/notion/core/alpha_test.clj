(ns notion.core.alpha-test
  (:require
   [clojure.test :as test :refer [deftest is are testing]]
   [notion.core.alpha :as notion]
   ))


(def NOTION_TOKEN (or (System/getenv "NOTION_TOKEN") (read-line)))


(deftest query-database
  (is
    (== (-> (notion/query-database
              {:notion/token NOTION_TOKEN}
              "4d70fcd9dbe84051a7e33749374524d6"
              {:filter {:property  "id"
                        :rich_text {:equals "a01"}}})
          (get "results")
          (count)) 1))

  (is (empty?
        (-> (notion/query-database
              {:notion/token NOTION_TOKEN}
              "4d70fcd9dbe84051a7e33749374524d6"
              {:filter {:property  "id"
                        :rich_text {:equals "z01"}}})
          (get "results")))))
