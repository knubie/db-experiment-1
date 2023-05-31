(ns bento.db.adapters.memory
  (:require
    [bento.db.protocols :as    p
                        :refer [Adapter]]
    [bento.db.utils     :as    utils]
    [clojure.set        :as    set]))


(defonce dbs (atom {}))






(defn transaction-log [store]
  (->> store
       (map (fn [[_ _ _ t]] t))
       (into (sorted-set))))




(defn get-store [db store]
  (get db store #{}))


(defn get-new-facts [db-before db-after store]
  (set/difference (get-store db-after store)
                  (get-store db-before store)))


(def add-fact conj)
  

(defn apply-tx

  "Applies a transaction to a database value."

  [store [op & fact]]

  (case op
    :bento.db/add (add-fact store fact)
                  store))


(defrecord Memory [name db]

  Adapter

  (transact [self store txs]
    (let [tx-id         (utils/generate-tx-id)
          temp-id->uuid (utils/temp-ids->uuids txs)]
      (swap! (:db self)
             #(assoc % store
                (reduce apply-tx
                        (get-store % store)
                        (->> txs (utils/with-ids temp-id->uuid)
                                 (utils/with-tx-id tx-id)))))))

  (get-db [self] (:db self))


  (sync-to [self target-conn stores]
    (let [new-facts (->> stores
                         (map #(p/new-facts-as-of self %
                                 (p/latest-tx-id target-conn %)))
                         (zipmap stores))]
        (p/inject-facts target-conn new-facts))

    (add-watch
      (:db self)
      :sync
      (fn [key ref db-before db-after]
        ;; For each store:
        ;;   find the latest transaction id in target db
        ;;   find all facts in source db greater than latest target db tx id
        (let [new-facts
              (->> stores (map #(get-new-facts db-before db-after %))
                          (zipmap stores))]

          (p/inject-facts target-conn new-facts)))))


  (new-facts-as-of [self store t]
    (let [facts-in-store (-> @(p/get-db self) (get-store store))
          _ (js/console.log facts-in-store)
          _ (js/console.log t)]

      (filter (fn [[_ _ _ t']] (> t' t))
              facts-in-store)))


  (latest-tx-id [self store]
    (-> @(p/get-db self)
        (get-store store)
        transaction-log
        last (or -1)))


  (inject-facts [self new-facts-by-store]
    (swap! (:db self)
           #(reduce (fn [db [store facts]]
                      (assoc % store
                             (reduce add-fact (get-store db store) facts)))
                    %
                    new-facts-by-store)))

  (find-db [self name]
    (@dbs name))

  (make-db [self {:bento.db/keys [name id]}]
    (swap! dbs assoc name {})

    {:adapter self
     :name    name
     :id      id}))


; (def memory (Memory.))


(defn connect
  [{:keys [name]}]

  (Memory. name (atom {})))
