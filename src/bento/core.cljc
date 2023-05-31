(ns bento.core
  (:require
    [bento.db.protocols :as p]))

(def transact   p/transact)
(def get-entity p/get-entity)
