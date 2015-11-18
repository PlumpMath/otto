(ns semperos.otto.util-test
  (:require [clojure.test :refer :all]
            [semperos.otto.util :refer :all]
            [clojure.string :as str]))

(deftest test-full-name
  (testing "Simple keywords and symbols"
    (are [name named] (= name (full-name named))
      "foo" :foo
      "foo" 'foo
      "a"   :a
      "a"   'a))
  (testing "Namespace-qualified keywords and symbols"
    (are [name named] (= name (full-name named))
      "foo/bar" :foo/bar
      "foo/bar" 'foo/bar
      "a/b"     :a/b
      "a/b"     'a/b)))

(deftest test-refashion-keys
  (let [f (fn [k] (str/upper-case (str k)))
        m {:alpha "beta" :gamma "delta" :epsilon "zeta"}
        kmap (zipmap (keys m) (repeat f))]
    (= (map f (keys m))
       (keys (refashion-keys m kmap)))
    (= (vals m)
       (vals (refashion-keys m kmap)))))
