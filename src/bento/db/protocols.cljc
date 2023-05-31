(ns bento.db.protocols)

(defprotocol Adapter

  "Provides methods for various backends including
  IndexedDB, Memory, and HTTP."


  (transact

    [self store txs]
    
    "Applies the list of transactions to the database.

    The transactions are applied to the in-memory cache first, without
    passing through spec validations.

    The transactions are then sent to the transactor for processing
    asynchronously. The transactor will validate specs and persist to durable
    storage. If any transactions fail validation, the transaction fails and the
    in-memory database is rolled back.")


  (get-entity
    
    [self entity-id]
    
    "Finds the given entity in the database, or nil if not found.")
    

  (get-db
    [self]
    
    "Returns the database value atom.")


  (sync-to
    [self target-conn stores]
    
    "Injects new facts from self to target connection once, then injects
    additional facts after each new transaction is applied to self.")


  (inject-facts
    [self new-facts-by-store]
    
    "Takes a list of facts keyed by store and injects them into the database
    value. Used during replication and seeding from a dump.")



  (new-facts-as-of [self store t])


  (latest-tx-id [self store])


  (find-db [self name])
  (make-db [self config]))
