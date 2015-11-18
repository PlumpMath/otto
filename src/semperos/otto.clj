(ns semperos.otto
  (:require [clojure.set :refer [rename-keys]]
            [schema.core :as s]
            [schema.coerce :as c]
            [schema.spec.core :as spec]
            [schema.macros :as smacros]
            [schema.utils :as sutils]
            [semperos.otto.coerce :refer [transform-keys transform-values]]))

(defn domain-kv
  "Given a map `value-map` that is a top-level value in a domain model, return a 2-tuple of `[key value]` with appropriate Schema values applied."
  [k value-map]
  [(if (:optional? value-map)
     (s/optional-key k)
     k)
   (:in value-map)])

(defn representation-kv
  [representation k value-map]
  [(cond
     (= (representation value-map) :otto/omit) :otto/omit
     (:optional? value-map) (s/optional-key k)
     :else (s/required-key k))
   ;; :out is a library-level default
   ;; :in is assume if neither a specification repr or :out is specified
   (or (representation value-map)
       (:out value-map)
       (:in value-map))])

(defn domain-schema*
  "A `reduce-kv` compatible fn for creating a Prismatic schema from an Otto domain model for the domain model, one entry at a time."
  [schema k v]
  (let [[key value] (domain-kv k v)]
    (assoc schema key value)))

(defn domain-schema
  "Given a domain model `dm` generate a Prismatic schema definition for it."
  [dm]
  (reduce-kv domain-schema* {} dm))

(defn representation-schema*
  "A `reduce-kv` compatible fn for creating a Prismatic schema from an Otto domain model for , one entry at a time."
  [representation]
  (fn [schema k v]
    (let [[key value] (representation-kv representation k v)]
      (if (= key :otto/omit)
        schema
        (assoc schema key value)))))

(defn representation-schema
  "Schema for a representation of a domain model. This is captured as a simple function that cannot be extended like `key-map`, because whereas the end-user needs to be able to change the default handling of key coercions, this function is married to the internal representation of the domain model and is representation-agnostic."
  [representation dm]
  (let [m (transform-keys dm dm [:in representation])]
    (reduce-kv (representation-schema* representation) {} m)))

(defn deserialize
  "Given a domain model `dm`, coerce the map `m` to a map that adheres to the domain model's specification. The default coercion matcher is Schema's built-in `json-coercion-matcher`.

  This function DOES NOT parse payloads of any kind. It expects a Clojure map of one shape that it will coerce to another."
  ([dm m] (deserialize dm m c/json-coercion-matcher))
  ([dm m coercion-matcher]
   (let [s (or (::schema dm)
               (domain-schema dm))
         ;; TODO This can be expressed with coercers, but after spending some time I had trouble getting it right.
         m (transform-keys m dm [:out :in])]
     ((c/coercer s coercion-matcher) m))))

(defn serialize
  "The `:format` may be either `:out` (if only one output format is supported) or the specific keyword used in the domain model, e.g. `:json`. If a field does not have `:json` data, then `:out` will be used by default."
  ([dm m] (serialize :out dm m))
  ([format dm m] (serialize format dm m c/json-coercion-matcher))
  ([format dm m coercion-matcher]
   (let [s (or (::schema dm)
               (representation-schema format dm))
         m (-> m
               (transform-values dm [:in format])
               (transform-keys dm [:in format]))]
     (if-let [error (s/check s m)]
       (ex-info "Serialization failed due to a validation error."
                {:validation error})
       m))))
