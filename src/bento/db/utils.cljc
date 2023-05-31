(ns bento.db.utils)
  ; (:require))

(def generate-tx-id js/Date.now)


(def temp-id? string?)


(defn tx->e [[_ e _ _]] e)


(defn temp-ids->uuids

  "Takes a list of transactions, extracts the
  unique temp-ids, and maps them to actual uuids."

  [txs]

  (let [temp-ids (->> txs (map tx->e) (filter temp-id?) (set))]
    (zipmap temp-ids (repeatedly random-uuid))))


(defn with-tx-id
  "Takes a list of transactions and adds a transaction id"
  
  [tx-id txs]
  
  (map (fn [[op e a v t]] [op e a v (or t tx-id)]) txs))


(defn with-ids
  "Takes a list of transactions and replaces all of the temp-ids with UUIDs."
  
  [temp-id->uuid txs] ;; => txs

  (let [temp-id->uuid (or temp-id->uuid (temp-ids->uuids txs))]
    (map (fn [[op e a v]] [op (if (temp-id? e) (temp-id->uuid e) e) a v])
         txs)))
