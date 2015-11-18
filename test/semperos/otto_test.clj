(ns semperos.otto-test
  (:require [clojure.test :refer :all]
            [semperos.otto :refer :all]
            [clojure.java.io :as io]
            [clojure.test.check.generators :as gen]
            [schema.core :as s]
            [schema.experimental.generators :as sgen]
            [cheshire.core :as json]
            [semperos.otto.coerce :refer [coerce]])
  (:import java.sql.Timestamp
           java.util.UUID))

(defn valid-uuid? [s] (try (UUID/fromString s) (catch Throwable _ false)))

(def NonEmptyString
  (s/conditional not-empty s/Str))

(def UuidStr
  (s/pred valid-uuid? 'valid-uuid))

(defmethod coerce [s/Uuid UuidStr]
  [_ uuid]
  (str uuid))

(def schema-leaf-generators
  {NonEmptyString (gen/not-empty gen/string)
   UuidStr (gen/fmap (fn [bytes] (str (java.util.UUID/nameUUIDFromBytes bytes))) gen/bytes)})

#_(def UuidStr s/Str)

#_(def TimestampStr {:schema (s/pred timestamp? 'TimestampStr)
                     :name "TimestampStr"
                     :example "2015-10-14T13:43:43.000Z"
                     :type "string"})

;; TODO Consider case of keys that are required for the domain model, but optional or even forbidden in representations
;; TODO Consider selectors for :out (e.g., [:attributes :email :address])
;; TODO Support arbitrary pre-xforms
;; TODO Support arbitrary metadata (e.g., Swagger config)
;; TODO Consider pre-xform functions for :out (and possibly :in) for fully custom coercions
;; :out (fn format-date-time [dt] )

(def Datum
  {:id {:key {:out "id"}
        :in s/Uuid
        :out UuidStr
        :db s/Uuid}
   :name {:key {:out "name"}
          :in s/Keyword
          :out s/Str}})

(def Student
  {:id {:in s/Uuid
        :out UuidStr}
   :school-id {:key {:out "schoolId"}
               :in s/Int}
   :email {:in (s/maybe NonEmptyString)
           :out (s/maybe s/Str)}
   :first-name {:key {:out "firstName"}
                :in NonEmptyString
                :out s/Str}
   :last-name {:key {:out "lastName"}
               :in (s/maybe NonEmptyString)
               :out s/Str}
   :image {:in NonEmptyString}
   :created-at {:optional? true
                :json :otto/omit
                :in (s/maybe Timestamp)}
   :updated-at {:json :otto/omit
                :optional? true
                :in (s/maybe Timestamp)}})

(defn datum-json []
  (json/parse-string
   "
{\"id\":   \"abe60b2a-1403-4692-be97-b226dd0f242d\",
 \"name\": \"test-datum\"}
"))

(defn student-json []
  (json/parse-string (slurp (io/resource "student.json"))))

(deftest test-schema
  (testing "A domain specification map"
    (testing "can be used to generate a Prismatic schema"
      (is (= {:id s/Uuid
              :school-id s/Int
              :email (s/maybe NonEmptyString)
              :first-name NonEmptyString
              :last-name (s/maybe NonEmptyString)
              :image NonEmptyString
              (s/optional-key :created-at) (s/maybe Timestamp)
              (s/optional-key :updated-at) (s/maybe Timestamp)}
             (domain-schema Student))))))

(deftest test-deserialize
  (testing "A domain specification map"
    (testing "can be used to deserialize a JSON-compatible Clojure map to a domain model"
      (is (= {:id #uuid "43d97569-fdc5-4786-89b6-7d0c8d81e846"
              :school-id 42
              :email "test@test.org"
              :first-name "Test"
              :last-name "Tester"
              :image "test.png"}
             (deserialize Student (student-json)))))))

(deftest test-serialize
  (testing "A domain specification map"
    (testing "can be used to serialize a domain model to a JSON-compatible Clojure map"
      (let [m {:id #uuid "43d97569-fdc5-4786-89b6-7d0c8d81e846"
               :school-id 42
               :email "test@test.org"
               :first-name "Test"
               :last-name "Tester"
               :image "test.png"}]
        (is (nil? (s/check (domain-schema Student) m)))
        (is (= (student-json)
               (serialize :json Student m)))))))

(deftest test-round-trip
  (testing "A domain specification map"
    (testing "can survive a round-trip through serialization and deserialization"
      (let [m {:id #uuid "43d97569-fdc5-4786-89b6-7d0c8d81e846"
               :school-id 42
               :email "test@test.org"
               :first-name "Test"
               :last-name "Tester"
               :image "test.png"}]
        (is (nil? (s/check (domain-schema Student) m)))
        (is (= m (deserialize Student (serialize :out Student m))))))))
