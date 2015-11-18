(ns semperos.otto.coerce
  "Coercion of domain model keys and values."
  (:require [clojure.set :refer [rename-keys]]
            [clojure.string :as str]
            [schema.core :as s]
            [semperos.otto.util :refer [full-name refashion-keys]])
  (:import java.util.UUID))

(def ^:dynamic *warn-on-coerce*
  "Tell Otto to warn when a default coercion is used. Is a function that takes a 2-item vector of the [from to] values used by the main `coerce` multimethod. If the function returns _truthy_, a warning will be produced. Default is `coll?`"
  coll?)

(defn key-map-out->in
  "Return a mapping of `:out` keys to `:in` keys. These represent possibilities for key transformations for incoming data."
  [m k v]
  (if-let [out (get-in v [:key :out])]
    (assoc m out k)
    ;; TODO Consider limiting this to specific formats, not using for :out default
    ;; For serialization/deserialization,
    ;; the relationship of Clojure keywords to strings is such that
    ;; in my opinion this is a good default.
    (if (keyword? k)
      (assoc m (full-name k) k)
      m)))

(defn key-map-in->out
  "Return a mapping of `:in` keys to `:out` keys."
  [m k v]
  (if-let [out (get-in v [:key :out])]
    (assoc m k out)
    m))

(defn key-map-in->json
  "Return a mapping of `:in` keys to `:out` keys, customized for JSON."
  [m k v]
  (if-let [out (or (get-in v [:key :json])
                   (get-in v [:key :out]))]
    (assoc m k out)
    (assoc m k (full-name k))))

(defmulti key-map
  "Rename keys in the map based on the `:out` entry for a domain field. If a non-function, `clojure.set/rename-keys` semantics apply. If a function, the key will be renamed by passing the old key value to the function.

  Two directions are supported: [:out :in] and [:in :out]. The former is for deserializing things into the domain model, the latter for serializing a legal domain model.

  NOTE: Originally hoped to do this as an advanced coercer, but it appears that validation happens top-down, such that we're not able to update lower-level schemas before the top-level schema already fails."
  (fn [direction _] direction))

(defmethod key-map [:out :in]
  [_ dm]
  (reduce-kv key-map-out->in {} dm))

(defmethod key-map [:in :out]
  [_ dm]
  (reduce-kv key-map-in->out {} dm))

(defmethod key-map [:in :json]
  [_ dm]
  (reduce-kv key-map-in->json {} dm))

(defmulti coerce
  "Arbitrary coercion from one thing to another. Defined by `direction` provided, which should be a vector of keywords."
  (fn [[from to] _] [from to]))

(defmethod coerce :default [from-to x]
  (when (*warn-on-coerce* x)
    (binding [*out* *err*]
      (println "WARNING: Using default coercion on a collection, which usually means you've forgotten to set up an appropriate coercion. The [from to] value is: " (pr-str from-to))))
  x)

(defmethod coerce [s/Uuid s/Str]
  [_ uuid]
  (str uuid))

(defmethod coerce [s/Str s/Uuid]
  [_ s]
  (UUID/fromString s))

(defmethod coerce [s/Keyword s/Str]
  [_ kw]
  (full-name kw))

(defmethod coerce [s/Str s/Keyword]
  [_ s]
  (let [[ns name] (str/split s #"/")]
    (if-not name
      (keyword ns)
      (keyword ns name))))

(defmethod coerce [s/Int s/Str]
  [_ i]
  (str i))

(defmethod coerce [s/Str s/Int]
  [_ s]
  (Integer/parseInt s))

(defmethod coerce [Long s/Str]
  [_ l]
  (str l))

(defmethod coerce [s/Str Long]
  [_ s]
  (Long/parseLong s))

(defmethod coerce [s/Num s/Str]
  [_ n]
  (str n))

(defmethod coerce [s/Str s/Num]
  [direction value]
  (throw (ex-info "Cannot cast from a string to a generic Number. Please specify a concrete numeric type."
                  {:direction direction
                   :value value})))

(defmethod coerce [s/Symbol s/Str]
  [_ sym]
  (str sym))

(defmethod coerce [s/Str s/Symbol]
  [_ s]
  (let [[ns name] (str/split s #"/")]
    (if-not name
      (symbol ns)
      (symbol ns name))))

(defn transform-keys
  "Parse the domain model to transform keys as necessary given the `direction` of the data."
  [m dm direction]
  (let [kms (group-by (fn [[k v]] (fn? v)) (key-map direction dm))]
    (-> m
        (rename-keys (into {} (get kms false)))
        (refashion-keys (into {} (get kms true))))))

(defn determine-value
  "Determine value used for transformations to/from domain model."
  [key m]
  (or (key m)
      (when (not= key :in)
        (or (:out m)
            (:in m)))))

(defn transform-values*
  "The `k` and `v` are from the domain model, the `m` is the map being transformed."
  [direction]
  (fn [m k v]
    (let [[from to] direction
          from-value (determine-value from v)
          to-value (determine-value to v)]
      (if (or (= to-value :otto/omit)
              (not (contains? m k)))
        m
        (update m k #(coerce [from-value to-value] %))))))

(defn transform-values
  "Parse the domain model to transform values as necessary given the `direction` of the data."
  [m dm direction]
  (reduce-kv (transform-values* direction) m dm))
