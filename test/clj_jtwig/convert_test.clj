(ns clj-jtwig.convert-test
  (:require [clojure.test :refer :all]
            [clj-jtwig.core :refer :all]
            [clj-jtwig.convert :refer :all]
            [clj-jtwig.functions :refer :all]))

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
    (do
      (set-options! :auto-convert-map-keywords false)
      (is (= {:a 1 :b 2 :c 3 :d 4 :e 5
              :f {:foo "foo" "bar" nil :baz {:lol "hello"}}}
             (java->clojure (new java.util.HashMap {:a 1 :b 2 :c 3 :d 4 :e 5
                                                    :f {:foo "foo" "bar" nil :baz {:lol "hello"}}})))
          "nested HashMap without auto keyword->string conversion")
      (set-options! :auto-convert-map-keywords true))
    (do
      (set-options! :auto-convert-map-keywords true)
      (is (= {:a 1 :b 2 :c 3 :d 4 :e 5
              :f {:foo "foo" :bar nil :baz {:lol "hello"}}}
             (java->clojure (new java.util.HashMap {"a" 1 "b" 2 "c" 3 "d" 4 "e" 5
                                                    "f" {"foo" "foo" "bar" nil "baz" {"lol" "hello"}}})))
          "nested HashMap with auto keyword->string conversion")
      (set-options! :auto-convert-map-keywords true))

    (is (= (new java.util.Date 1393769745745)
           (java->clojure (new java.util.Date 1393769745745)))
        "Object")

    (is (= nil
           (java->clojure nil))
        "null")))

(deftest clojure-to-java
  (testing "Converting Clojure values to Java values"
    (is (= (new Long 42)
           (clojure->java 42))
        "integer-type number")
    (is (= (new Double 3.14159)
           (clojure->java 3.14159))
        "floating-point-type number")
    (is (= (new Boolean true)
           (clojure->java true))
        "boolean")
    (is (= (new Character \a)
           (clojure->java \a))
        "character")
    (is (= (new String "foobar")
           (clojure->java "foobar"))
        "string")

    (is (= java.util.ArrayList
           (class (clojure->java [1 2 3 4 5])))
        "vector type")
    (is (= (new java.util.ArrayList [1 2 3 4 5])
           (clojure->java [1 2 3 4 5]))
        "vector contents")
    (is (= java.util.ArrayList
           (class (clojure->java #{1 2 3 4 5})))
        "set type")
    (is (= (new java.util.ArrayList #{1 2 3 4 5})
           (clojure->java #{1 2 3 4 5}))
        "set contents")
    (is (= java.util.ArrayList
           (class (clojure->java '(1 2 3 4 5))))
        "list type")
    (is (= (new java.util.ArrayList '(1 2 3 4 5))
           (clojure->java '(1 2 3 4 5)))
        "list contents")
    (is (= java.util.HashMap
           (class (clojure->java {:a 1 :b 2 :c 3 :d 4 :e 5})))
        "map type")
    (do
      (set-options! :auto-convert-map-keywords false)
      (is (= (new java.util.HashMap {:a 1 :b 2 :c 3 :d 4 :e 5})
             (clojure->java {:a 1 :b 2 :c 3 :d 4 :e 5}))
          "map contents without auto keyword->string conversion")
      (set-options! :auto-convert-map-keywords true))
    (do
      (set-options! :auto-convert-map-keywords true)
      (is (= (new java.util.HashMap {"a" 1 "b" 2 "c" 3 "d" 4 "e" 5})
             (clojure->java {:a 1 :b 2 :c 3 :d 4 :e 5}))
          "map contents with auto keyword->string conversion")
      (set-options! :auto-convert-map-keywords true))

    (is (= (new java.util.ArrayList [1 2 3 4 (new java.util.ArrayList [\a \b \c \d \e])])
           (clojure->java [1 2 3 4 [\a \b \c \d \e]]))
        "nested vector contents")
    (do
      (set-options! :auto-convert-map-keywords false)
      (is (= (new java.util.HashMap {:a 1 :b 2 :c 3 :d 4 :e 5
                                     :f (new java.util.HashMap {:foo "foo" "bar" nil :baz (new java.util.HashMap {:lol "hello"})})})
             (clojure->java {:a 1 :b 2 :c 3 :d 4 :e 5
                             :f {:foo "foo" "bar" nil :baz {:lol "hello"}}}))
          "nested map contents without auto keyword->string conversion")
      (set-options! :auto-convert-map-keywords true))
    (do
      (set-options! :auto-convert-map-keywords true)
      (is (= (new java.util.HashMap {"a" 1 "b" 2 "c" 3 "d" 4 "e" 5
                                     "f" (new java.util.HashMap {"foo" "foo" "bar" nil "baz" (new java.util.HashMap {"lol" "hello"})})})
             (clojure->java {:a 1 :b 2 :c 3 :d 4 :e 5
                             :f {:foo "foo" "bar" nil :baz {:lol "hello"}}}))
          "nested map contents with auto keyword->string conversion")
      (set-options! :auto-convert-map-keywords true))

    (is (= (new java.util.Date 1393769745745)
           (clojure->java (new java.util.Date 1393769745745)))
        "object")

    (is (= nil
           (clojure->java nil))
        "nil")))

(deftest function-map-param-conversion
  (testing "Jtwig function map parameter conversion (keyword keys)"
    (do
      (reset-functions!)

      (deftwigfn "keys_are_all_keywords" [x]
        (every? keyword? (keys x)))
      (deftwigfn "keys_are_all_strings" [x]
        (every? string? (keys x)))

      (set-options! :auto-convert-map-keywords true)
      (is (= (render "{{keys_are_all_keywords(x)}}" {:x {:a "foo" :b "bar" :c "baz"}})
             "true"))
      (is (= (render "{{keys_are_all_keywords(x)}}" {:x {"a" "foo" "b" "bar" "c" "baz"}})
             "true"))

      (set-options! :auto-convert-map-keywords false)
      (is (= (render "{{keys_are_all_keywords(x)}}" {"x" {:a "foo" :b "bar" :c "baz"}})
             "true"))
      (is (= (render "{{keys_are_all_strings(x)}}" {"x" {"a" "foo" "b" "bar" "c" "baz"}})
             "true"))

      (set-options! :auto-convert-map-keywords true)
      (reset-functions!))))

(deftest function-map-return-conversion
  (testing "Jtwig function map return value conversion (keyword keys)"
    (do
      (reset-functions!)

      (deftwigfn "get_map_with_keywords" [x]
        {:a "foo" :b "bar" :c "baz"})
      (deftwigfn "get_map_with_strings" [x]
        {"a" "foo" "b" "bar" "c" "baz"})
      (deftwigfn "keys_are_all_keywords" [x]
        (every? keyword? (keys x)))
      (deftwigfn "keys_are_all_strings" [x]
        (every? string? (keys x)))

      (set-options! :auto-convert-map-keywords true)
      (is (= (render "{{keys_are_all_keywords(get_map_with_keywords(null))}}" {})
             "true"))
      (is (= (render "{{keys_are_all_keywords(get_map_with_strings(null))}}" {})
             "true"))

      (set-options! :auto-convert-map-keywords false)
      (is (= (render "{{keys_are_all_keywords(get_map_with_keywords(null))}}" {})
             "true"))
      (is (= (render "{{keys_are_all_strings(get_map_with_strings(null))}}" {})
             "true"))

      (set-options! :auto-convert-map-keywords true)
      (reset-functions!))))
