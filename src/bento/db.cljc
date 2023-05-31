(ns bento.db
  (:require
    [clojure.set                 :as    set]
    [clojure.spec.alpha          :as    s]
    [cljs.core.async             :as    async]
    [bento.db.protocols          :as    p]
    [bento.db.adapters.memory    :as    bento.memory]
    [bento.db.adapters.indexeddb :as    bento.indexeddb]))


;; TX-id are a timestamp.
;; When syncing, the target db will never import a transaction *newer* than its own clock.
;; This way its impossible for a user to create a datom in the future locally,
;; and then try to sync it to the remote db.
;; It *is* possible to create a datom in the past and sync that.
;; How can we prevent users from re-writing the past in their local-db?
;; Use case, posting a tweet to twitter.
;; The timestamp should be in a centralized remote db
;; The tweet itself exists in the user's personal db
;; Once the tweet is "published" it issues a request to the server to
;; create a new "global tweet" with a timestamp that links to the user's tweet.

(defn get-store [db store]
  (get db store #{}))



;; FIXME: This won't work both ways.
;; E.g. If I sync from A to B, when A changes, B is updated.
;; This will then trigger an update to A if I'm also sync from B to A.
(defn sync [{:keys [from to stores]}]
  (p/sync-to from to stores))


;; ----------------------------------------------------------------------------


(def conn1 (bento.memory/connect {:name "bento-talk"}))
; (def conn1 (connect {::adapter memory
;                      ::name    "bento-talk"}))

(p/transact conn1 :person [[:bento.db/add "alice" :person/name "before-sync"]
                           [:bento.db/add "alice" :person/age 38]])


(def conn2 (bento.memory/connect {:name "bento-talk-2"}))


; (transact conn1 :person [[:bento.db/add "alice" :person/name "before-sync"]
;                          [:bento.db/add "alice" :person/age 38]])
      

(sync {:from   conn1
       :to     conn2
       :stores [:person]})


(p/transact conn1 :person [[:bento.db/add "alice" :person/name "Alice"]
                           [:bento.db/add "alice" :person/age 38]])


; (p/transact conn1 :person [[:bento.db/add "bob" :person/name "Bob"]
;                            [:bento.db/add "bob" :person/age 42]])

(js/console.log @(p/get-db conn1))
(js/console.log @(p/get-db conn2))


(def conn3 (bento.indexeddb/connect {:name "bento-talk"}))

;(async/go)
(let [report
      (p/transact conn3 :person [[:bento.db/add "alice" :person/name "Alice"]
                                 [:bento.db/add "alice" :person/age 38]])
      alice-id (get-in report [:tempids "alice"])]
  (p/transact conn3 :person [[:bento.db/add alice-id :person/name "Bob"]
                             [:bento.db/add alice-id :person/age 42]]))
