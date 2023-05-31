(ns bento.db.adapters.indexeddb
  (:require
    [goog.object        :as    gobj]
    [bento.db.protocols :as    p
                        :refer [Adapter]]
    [bento.db.utils     :as    utils]
    [clojure.set        :as    set]
    [cljs.core.async    :as    async]
    [cognitect.transit  :as    transit]))


(def reader (transit/reader :json))
(def writer (transit/writer :json))




(defrecord IndexedDB [name db]

  Adapter


  (get-entity [self entity-id]
    (let [result-chan (async/chan 0)]
      (async/go
        (let [idb          (async/<! (:db self))
              _            (async/>! (:db self) idb)
              entity-store (-> idb (.transaction "entities" "readwrite")
                                   (.objectStore "entities"))
              req          (.get entity-store (transit/write writer entity-id))]

          (gobj/set req "onerror"
            #(throw %))
          (gobj/set req "onsuccess"
            #(async/put! result-chan (if (exists? (.-result req))
                                       (transit/read reader (.-result req))
                                       :not-found)))))

      result-chan))



  (transact [self store txs]

    (let [report-chan   (async/chan 0)
          tx-id         (utils/generate-tx-id)
          temp-id->uuid (utils/temp-ids->uuids txs)
          reified-txs   (->> txs (utils/with-ids temp-id->uuid)
                                 (utils/with-tx-id tx-id))
          txs-by-e      (group-by (fn [[_ e _ _ _]] e) reified-txs)]

      (async/go
        (let [idb          (async/<! (:db self))
              _            (async/>! (:db self) idb)

              tx-log-store (-> idb (.transaction "transaction-log" "readwrite")
                                   (.objectStore "transaction-log"))]

          (.add tx-log-store (transit/write writer reified-txs) tx-id)
            

          (doseq [[e txs] txs-by-e]
            (let [entity (async/<! (p/get-entity self e))
                  entity-store (-> idb (.transaction "entities" "readwrite")
                                       (.objectStore "entities"))
                  new-entity (reduce
                               (fn [doc [op e a v t]]
                                 (case op
                                   :bento.db/add     (assoc doc a v)
                                   ; :bento.db/retract (dissoc entity a v)
                                                 doc))
                               (if (= entity :not-found) {:id e} entity)
                               txs)]

              (if (= entity :not-found)
                (let [req (.add entity-store
                                (transit/write writer new-entity)
                                (transit/write writer e))]
                  (gobj/set req "oncomplete"
                    (fn [e]
                      (async/put! report-chan {:tempids temp-id->uuid}))))
                  
                (let [req (.put entity-store
                                (transit/write writer new-entity)
                                (transit/write writer e))]
                  (gobj/set req "oncomplete"
                    (fn [e]
                      (async/put! report-chan {:tempids temp-id->uuid})))))))))

          ; (doseq [[e txs] txs-by-e]
          ;   (let [req (.get entity-store (transit/write writer e))]
          ;     (gobj/set req "onerror" #(js/console.log %))
          ;     (gobj/set req "onsuccess"
          ;       #(let [entity     (if (exists? (.-result req))
          ;                           (transit/read reader (.-result req))
          ;                           {:id e})
          ;              new-entity (reduce
          ;                           (fn [doc [op e a v t]]
          ;                             (case op
          ;                               :bento.db/add     (assoc doc a v)
          ;                               ; :bento.db/retract (dissoc entity a v)
          ;                                             doc))
          ;                           entity txs)]

          ;           (if (exists? (.-result req))
          ;             (.put entity-store
          ;                   (transit/write writer new-entity)
          ;                   (transit/write writer e))
          ;             (.add entity-store
          ;                   (transit/write writer new-entity)
          ;                   (transit/write writer e)))))))))

      (async/put! report-chan {:tempids temp-id->uuid})
      report-chan)))

        ; (doseq [[op e a v t] (->> txs utils/with-ids (utils/with-tx-id tx-id))]
        ;   (case op
        ;     :bento.db/add
        ;     (let [req (.get object-store (transit/write writer e))]
        ;       (gobj/set req "onsuccess"
        ;         #(let [entity     (or (.-result req) {:id e})
        ;                new-entity (assoc entity a v)]
        ;            (if (exists? (.-result req))
        ;              (.put object-store
        ;                    (transit/write writer new-entity)
        ;                    (transit/write writer e))
        ;              (.add object-store
        ;                    (transit/write writer new-entity)
        ;                    (transit/write writer e)))))
        ;       (gobj/set req "onerror"
        ;                 (fn [e] (js/console.log "No " (.-result req)))))))))))


(defn connect
  [{:keys [name]}]

  (let [connection-channel (async/chan 0)
        req (.open js/indexedDB name 1)]

    (gobj/set req "onerror" 
      (fn [e] (js/console.error "Couldn't open indexeddb database.")))

    (gobj/set req "onsuccess"
      (fn [e]
        (let [db (-> e .-target .-result)]
          (async/put! connection-channel db))))

    (gobj/set req "onupgradeneeded"
      (fn [e]
        (js/console.log "upgrade needed")
        (let [db (-> e .-target .-result)]
          (.createObjectStore db "entities")
          (.createObjectStore db "transaction-log"))))
        

    (IndexedDB. name connection-channel)))
