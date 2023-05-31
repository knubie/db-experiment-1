(ns bento.test

  "This is a docstring"

  (:require 
    [goog.object                 :as    gobj]
    [cljs.test                   :as    test
                                 :refer [deftest is use-fixtures]]
    [cljs.core.async             :as    async]
    [bento.core                  :refer [transact get-entity]]
    [bento.db.adapters.indexeddb :as    bento.indexeddb]))


(def db-name)
                            

(use-fixtures :each
  {:after #(test/async done
             (let [req (.deleteDatabase js/indexedDB db-name)]
               (gobj/set req "onerror"   (fn [e] (throw e)))
               (gobj/set req "onsuccess" done)))})


(deftest insert
  (test/async done
    (async/go
      (let [conn     (bento.indexeddb/connect {:name "bento-test"})
            report   (async/<! (transact conn :person
                                 [[:bento.db/add "alice" :person/name "Alice"]
                                  [:bento.db/add "alice" :person/age   38]]))
            alice-id (get-in report [:tempids "alice"])
            alice    (async/<! (get-entity conn alice-id))]

        (is (= alice {:id          alice-id
                      :person/name "Alice"
                      :person/age   38}))
        (done)))))
