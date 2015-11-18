(ns semperos.otto.test
  (:require [schema.experimental.generators :as sgen]
            [semperos.otto :refer [domain-schema representation-schema]]))

(defn domain-generator
  "Given a domain model `dm`, return a generator for the Prismatic schema definition that represents it."
  ([dm] (domain-schema-generator dm {}))
  ([dm leaf-generators] (domain-schema-generator dm leaf-generators {}))
  ([dm leaf-generators wrappers]
   (sgen/generator (domain-schema dm) leaf-generators wrappers)))

(defn representation-generator
  "Given a domain model `dm`, return a generator for the given `representation` as defined by that domain model."
  ([representation dm] (representation-generator representation dm {}))
  ([representation dm leaf-generators] (representation-generator representation dm leaf-generators {}))
  ([representation dm leaf-generators wrappers]
   (sgen/generator (representation-schema representation dm) leaf-generators wrappers)))
