(ns semperos.otto.coerce-test
  (:require [clojure.test :refer :all]
            [semperos.otto.coerce :refer :all]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [com.gfredericks.test.chuck.clojure-test :refer [checking]]
            [schema.core :as s]
            [semperos.otto.util :refer [full-name]])
  (:import clojure.lang.ExceptionInfo))

(alter-var-root #'*warn-on-coercion* (constantly (constantly false)))

(def iterations 20)

(deftest test-key-map-out->in
  (checking "The relationship of :in and :out for key-map-out->in" iterations
            [in-key gen/keyword
             out-key gen/string
             in-value gen/any
             acc (gen/map gen/any gen/any)]
            (let [result1 (key-map-out->in acc in-key {:in in-value :key {:out out-key}})
                  result2 (key-map-out->in acc in-key {:in in-value})]
              (is (= {out-key in-key}
                     (select-keys result1 [out-key]))
                  "should return a map with an explicit :out to the :in value.")
              (is (= {(full-name in-key) in-key}
                     (select-keys result2 [(full-name in-key)]))
                  "if the key is a keyword, should return a stringified version of the keyword if no :out is specified."))))

(deftest test-key-map-in->out
  (checking "The relationship of :in and :out for key-map-in->out" iterations
            [in-key gen/keyword
             out-key gen/string
             in-value gen/any
             acc (gen/map gen/any gen/any)]
            (let [result1 (key-map-in->out acc in-key {:in in-value :key {:out out-key}})
                  result2 (key-map-in->out acc in-key {:in in-value})]
              (is (= {in-key out-key}
                     (select-keys result1 [in-key]))
                  "should return a map with an explicit :in to the :out value.")
              (is (= acc result2)
                  "should return the original map if no :key spec is provided."))))

(deftest test-key-map-in->json
  (checking "The relationship of :in and :json for key-map-in->json" iterations
            [in-key gen/keyword
             json-key gen/string
             in-value gen/any
             acc (gen/map gen/any gen/any)]
            (let [result1 (key-map-in->json acc in-key {:in in-value :key {:json json-key}})
                  result2 (key-map-in->json acc in-key {:in in-value :key {:out json-key}})
                  result3 (key-map-in->json acc in-key {:in in-value})]
              (is (= {in-key json-key}
                     (select-keys result1 [in-key]))
                  "should return a map with an explicit :in to the :json value.")
              (is (= {in-key json-key}
                     (select-keys result2 [in-key]))
                  "should return a map with an :in to :out value if the specific representation is not defined.")
              (is (= {in-key (full-name in-key)}
                     (select-keys result3 [in-key]))
                  "should return a map with a default entry of the stringified version of the original key."))))

(deftest test-key-map
  (testing "When translating into the domain model"
    (checking "the [:out :in] direction" iterations
              [in-key1 gen/keyword
               in-key2 gen/keyword
               in-value1 (gen/elements [s/Uuid s/Str])
               in-value2 (gen/elements [s/Int s/Bool])
               out-key1 gen/string
               out-key2 gen/int]
              (let [dir [:out :in]
                    dm1 {in-key1 {:in in-value1 :key {:out out-key1}}
                         in-key2 {:in in-value2 :key {:out out-key2}}}
                    dm2 {in-key1 {:in in-value1}
                         in-key2 {:in in-value2 :key {:out out-key2}}}
                    result-dm1 (key-map dir dm1)
                    result-dm2 (key-map dir dm2)]
                (is (= {out-key1 in-key1 out-key2 in-key2}
                       result-dm1)
                    "should return a map with all explicit :out to :in mappings.")
                (is (= {(full-name in-key1) in-key1 out-key2 in-key2}
                       result-dm2)
                    "should return a map with all explicit :out to :in mappings as well as stringified defaults for those not specified.")))
    (checking "the [:in :out] direction" iterations
              [in-key1 gen/keyword
               in-key2 gen/keyword
               in-value1 (gen/elements [s/Int s/Bool])
               in-value2 (gen/elements [s/Uuid s/Str])
               out-key1 gen/string
               out-key2 gen/int]
              (let [dir [:in :out]
                    dm1 {in-key1 {:in in-value1 :key {:out out-key1}}
                         in-key2 {:in in-value2 :key {:out out-key2}}}
                    dm2 {in-key1 {:in in-value1}
                         in-key2 {:in in-value2 :key {:out out-key2}}}
                    result-dm1 (key-map dir dm1)
                    result-dm2 (key-map dir dm2)]
                (is (= {in-key1 out-key1 in-key2 out-key2}
                       result-dm1)
                    "should return a map with all explicit :in to :out mappings.")
                (is (= {in-key2 out-key2}
                       result-dm2)
                    "should exclude entries without explicit :out")))
    (checking "the [:in :json] direction" iterations
              [in-key1 gen/keyword
               in-key2 gen/keyword
               in-value1 (gen/elements [s/Int s/Bool])
               in-value2 (gen/elements [s/Uuid s/Str])
               out-key1 gen/string
               out-key2 gen/int]
              (let [dir [:in :json]
                    dm1 {in-key1 {:in in-value1 :key {:out out-key1}}
                         in-key2 {:in in-value2 :key {:out out-key2}}}
                    dm2 {in-key1 {:in in-value1 :key {:out out-key1}}
                         in-key2 {:in in-value2 :key {:json out-key2}}}
                    dm3 {in-key1 {:in in-value1}
                         in-key2 {:in in-value2 :key {:out out-key2}}}
                    result-dm1 (key-map dir dm1)
                    result-dm2 (key-map dir dm2)
                    result-dm3 (key-map dir dm3)]
                (is (= {in-key1 out-key1 in-key2 out-key2}
                       result-dm1)
                    "should return a map with all explicit :in to :json mappings.")
                (is (= {in-key1 out-key1 in-key2 out-key2}
                       result-dm2)
                    "should return a map with all explicit :in to :json mappings, falling back to :out mappings.")
                (is (= {in-key1 (full-name in-key1) in-key2 out-key2}
                       result-dm3)
                    "should fall back to stringified entries if neither :json nor :out is specified.")))))

(deftest test-coerce
  (testing "Generic coercion from one value to another"
    (let [uuid (java.util.UUID/randomUUID)
          obj (Object.)]
     (are [from-to x y] (= y (coerce from-to x))
       [::a ::b]         obj obj
       [s/Uuid s/Str]    uuid (str uuid)
       [s/Str  s/Uuid]   (str uuid) uuid
       [s/Keyword s/Str] :foo "foo"
       [s/Keyword s/Str] :foo/bar "foo/bar"
       [s/Str s/Keyword] "alpha" :alpha
       [s/Str s/Keyword] "alpha/beta" :alpha/beta
       [s/Symbol s/Str]  'foo "foo"
       [s/Symbol s/Str]  'foo/bar "foo/bar"
       [s/Str s/Symbol]  "alpha" 'alpha
       [s/Str s/Symbol]  "alpha/beta" 'alpha/beta
       [s/Int s/Str]     (Integer. 42) "42"
       [s/Str s/Int]     "42" (Integer. 42)
       [Long s/Str]      (Long. 42) "42"
       [s/Str Long]      "42" (Long. 42)
       [s/Num s/Str]     42 "42"))))

(deftest test-cant-coerce-to-generic-number
  (is (thrown? ExceptionInfo (coerce [s/Str s/Num] "42"))))

(defspec spec-coerce-identity iterations
  (prop/for-all [x gen/any]
                (= x (coerce [::b ::a] (coerce [::a ::b] x)))))

(defspec spec-coerce-uuid<->str iterations
  (prop/for-all [uuid (gen/fmap (fn [bytes] (java.util.UUID/nameUUIDFromBytes bytes)) gen/bytes)]
                (= uuid (coerce [s/Str s/Uuid] (coerce [s/Uuid s/Str] uuid)))))

(defspec spec-coerce-keyword<->str iterations
  (prop/for-all [keyword gen/keyword]
                (= keyword (coerce [s/Str s/Keyword] (coerce [s/Keyword s/Str] keyword)))))

(defspec spec-coerce-symbol<->str iterations
  (prop/for-all [symbol gen/symbol]
                (= symbol (coerce [s/Str s/Symbol] (coerce [s/Symbol s/Str] symbol)))))

(defspec spec-coerce-integer<->str iterations
  (prop/for-all [int gen/int]
                (= int (coerce [s/Str s/Int] (coerce [s/Int s/Str] int)))))

(defspec spec-coerce-long<->str iterations
  (prop/for-all [int gen/int]
                (= int (coerce [s/Str Long] (coerce [Long s/Str] int)))))
