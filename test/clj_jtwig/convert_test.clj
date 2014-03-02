(ns clj-jtwig.convert-test
  (:require [clojure.test :refer :all]
            [clj-jtwig.convert :refer :all]))

(deftest java-to-clojure
  (testing "Converting Java values to Clojure values"
    (is (= (byte 64)
           (java->clojure (new Byte (byte 64))))
        "Byte")
    (is (= (short 1337)
           (java->clojure (new Short (short 1337))))
        "Short")
    (is (= (int 1048576)
           (java->clojure (new Integer (int 1048576))))
        "Integer")
    (is (= (new Long 1099511627776)
           (java->clojure (new Long 1099511627776)))
        "Long")
    (is (= (float 3.14159)
           (java->clojure (new Float (float 3.14159))))
        "Float")
    (is (= (new Double 3.14159)
           (java->clojure (new Double 3.14159)))
        "Double")
    (is (= (new Boolean true)
           (java->clojure (new Boolean true)))
        "Boolean")
    (is (= (new Character \a)
           (java->clojure (new Character \a)))
        "Character")
    (is (= (new String "foobar")
           (java->clojure (new String "foobar")))
        "String")

    (is (= '(1 2 3 4 5)
           (java->clojure (new java.util.ArrayList [1 2 3 4 5])))
        "ArrayList")
    (is (= '(1 2 3 4 5)
           (java->clojure (new java.util.HashSet #{1 2 3 4 5})))
        "HashSet")
    (is (= '(1 2 3 4 5)
           (java->clojure (new java.util.LinkedList '(1 2 3 4 5))))
        "LinkedList")
    (is (= {:a 1 :b 2 :c 3 :d 4 :e 5}
           (java->clojure (new java.util.HashMap {:a 1 :b 2 :c 3 :d 4 :e 5})))
        "HashMap")

    (is (= [1 2 3 4 [\a \b \c \d \e]]
           (java->clojure (new java.util.ArrayList [1 2 3 4 [\a \b \c \d \e]])))
        "nested ArrayList")
    (is (= {:a 1 :b 2 :c 3 :d 4 :e 5 :f {:foo "foo" "bar" nil :baz {:lol "hello"}}}
           (java->clojure (new java.util.HashMap {:a 1 :b 2 :c 3 :d 4 :e 5 :f {:foo "foo" "bar" nil :baz {:lol "hello"}}})))
        "nested HashMap")

    (is (= {:seconds 45, :date 2, :minutes 15, :hours 9, :year 114, :timezoneOffset 300, :month 2, :day 0, :time 1393769745745}
           (java->clojure (new java.util.Date 1393769745745)))
        "Object")

    (is (= nil
           (java->clojure nil))
        "null")))
