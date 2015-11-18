(ns semperos.otto.util
  (:require [schema.core :as s]))

(defn full-name
  ":foo => \"foo\" and :bar/foo => \"bar/foo\". Let's non-keywords pass through."
  [kw]
  (if (instance? clojure.lang.Named kw)
    (str (when-let [ns (namespace kw)] (str ns "/"))
         (name kw))
    kw))

(defn refashion-keys
  "Returns the map with the keys in kmap renamed by passing to the functions in kmap. Modified version of `clojure.set/rename-keys`."
  [map kmap]
    (reduce
     (fn [m [old f]]
       (if (contains? map old)
         (assoc m (f old) (get map old))
         m))
     (apply dissoc map (keys kmap)) kmap))
