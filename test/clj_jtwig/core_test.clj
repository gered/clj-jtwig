(ns clj-jtwig.core-test
  (:import (java.io FileNotFoundException)
           (clojure.lang ArityException))
  (:require [clojure.test :refer :all]
            [clj-jtwig.core :refer :all]))

; The purpose of these tests is to establish that our wrapper around JTwig works. That is,
; we will be focusing on stuff like making sure that passing Clojure data structures
; (e.g. maps, vectors, lists) over to JTwig works fine.
; JTwig includes its own test suite which tests actual template parsing and evaluation
; functionality, so there's no point in duplicating that kind of testing here once we
; establish that the above mentioned stuff works fine.
;
; Some of the variable passing and return / iteration verification tests might be a bit
; overkill, but better safe than sorry. :)

(deftest string-template
  (testing "Evaluating templates in string vars"
    (is (= (render "Hello {{ name }}!"
                   {:name "Bob"})
           "Hello Bob!")
        "passing a model-map")
    (is (= (render "Hello {{ name }}!"
                   nil)
           "Hello null!")
        "not passing a model-map")
    (is (= (render "Hello {{ name }}!"
                   {"name" "Bob"})
           "Hello Bob!")
        "passing a model-map where the keys are strings already")
    (is (= (render "Hello {{ name }}!"
                   {"name" "Bob"}
                   {:skip-model-map-stringify? true})
           "Hello Bob!")
        "passing a model-map where the keys are strings already and we want to skip auto stringifying bbb")
    (is (thrown?
          ClassCastException
          (render "Hello {{ name }}!"
                  {:name "Bob"}
                  {:skip-model-map-stringify? true}))
        "passing a model-map where the keys are keywords and try skipping auto stringifying the keys")))

(deftest passing-model-map-data
  (testing "Passing Clojure data structures to JTwigContext's"
    (is (= (render "float {{ x }}"
                   {:x 3.14})
           "float 3.14")
        "passing a float")
    (is (= (render "integer {{ x }}"
                   {:x 42})
           "integer 42")
        "passing an integer")
    (is (= (render "null {{ x }}"
                   {:x nil})
           "null null")
        "passing a nil value")
    (is (= (render "char {{ x }}"
                   {:x \a})
           "char a")
        "passing a character")
    (is (= (render "string {{ x }}"
                   {:x "string"})
           "string string")
        "passing a string")
    (is (= (render "{% for n in x %}{{n}} {% endfor %}"
                   {:x [1 2 3 4 5]})
           "1 2 3 4 5 ")
        "passing a vector")
    (is (= (render "{% for n in x %}{{n}} {% endfor %}"
                   {:x '(\a \b \c \d \e)})
           "a b c d e ")
        "passing a list")
    (is (= (render "{% for n in x %}{{n}} {% endfor %}"
                   {:x #{1 2 3 4 5}})
           "1 2 3 4 5 ")
        "passing a set")
    (is (= (render "{% for k, v in x %}{{k}}: {{v}} {% endfor %}"
                   {:x {:a 1 :b 2 :c 3 :d 4 :e 5}})
           "a: 1 c: 3 b: 2 d: 4 e: 5 ")
        "passing a map")
    (is (= (render "{{root.foo}}, {{root.bar.baz}}, {% for n in root.v %}{{n}} {% endfor %}"
                   {:root {:foo "abc"
                           :bar {:baz 1337}
                           :v [10 100 1000]}})
           "abc, 1337, 10 100 1000 ")
        "passing a map with primitives / nested maps / sequences")))

(deftest file-templates
  (testing "Evaluating templates from files"
    (let [test-filename    "test/templates/file-template-test.twig"
          invalid-filename "test/templates/nonexistant.twig"]
      (is (= (render-file test-filename
                          {:name "Bob"})
             "Hello Bob from a file!")
          "passing a model-map")
      (is (thrown?
            FileNotFoundException
            (render-file invalid-filename
                         {:name "Bob"}))
          "trying to render a file that doesn't exist")
      (is (= (render-file test-filename
                          nil)
             "Hello null from a file!")
          "not passing a model-map")
      (is (= (render-file test-filename
                          {"name" "Bob"})
             "Hello Bob from a file!")
          "passing a model-map where the keys are strings already")
      (is (= (render-file test-filename
                     {"name" "Bob"}
                     {:skip-model-map-stringify? true})
             "Hello Bob from a file!")
          "passing a model-map where the keys are strings already and we want to skip auto stringifying bbb")
      (is (thrown?
            ClassCastException
            (render-file test-filename
                         {:name "Bob"}
                         {:skip-model-map-stringify? true}))
          "passing a model-map where the keys are keywords and try skipping auto stringifying the keys"))))

(deftest template-functions
  (testing "Adding custom template functions"
    (do
      (reset-functions!)

      (is (nil? (deftwigfn "add" [a b]
                  (+ a b))))

      (is (true? (function-exists? "add")))
      (is (false? (function-exists? "foobar")))

      (is (thrown?
            Exception
            (deftwigfn "add" [a b]
              (+ a b))))

      (is (= (render "{{add(1, 2)}}" nil)
             "3")
          "calling a custom function")
      (is (= (render "{{add(a, b)}}" {:a 1 :b 2})
             "3")
          "calling a custom function, passing in variables from the model-map as arguments")
      (is (= (render "{{x|add(1)}}" {:x 1})
             "2")
          "calling a custom function using the 'filter' syntax")

      (reset-functions!)))

  (testing "Fixed and variable number of template function arguments"
    (do
      (reset-functions!)

      (is (nil? (deftwigfn "add2" [a b]
                  (+ a b))))
      (is (true? (function-exists? "add2")))
      (is (nil? (deftwigfn "addAll" [& numbers]
                  (apply + numbers))))
      (is (true? (function-exists? "addAll")))

      (is (= (render "{{add2(1, 2)}}" nil)
             "3")
          "fixed number of arguments (correct amount)")
      (is (thrown?
            ArityException
            (render "{{add2(1)}}" nil)))
      (is (= (render "{{addAll(1, 2, 3, 4, 5)}}" nil)
             "15")
          "variable number of arguments (non-zero)")
      (is (= (render "{{addAll}}" nil)
             "null")
          "variable number of arguments (zero)")

      (reset-functions!)))

  (testing "Passing different data structures to template functions"
    (do
      (reset-functions!)

      (is (nil? (deftwigfn "identity" [x]
                  x)))
      (is (true? (function-exists? "identity")))
      (is (nil? (deftwigfn "typename" [x]
                  (.getName (type x)))))
      (is (true? (function-exists? "typename")))

      ; verify that the clojure function recognizes the correct types when the variable is passed via the model-map
      (is (= (render "{{typename(x)}}" {:x 42})
             "java.lang.Long")
          "integer typename via model-map")
      (is (= (render "{{typename(x)}}" {:x 3.14})
             "java.lang.Double")
          "float typename via model-map")
      (is (= (render "{{typename(x)}}" {:x "foobar"})
             "java.lang.String")
          "string typename via model-map")
      (is (= (render "{{typename(x)}}" {:x \a})
             "java.lang.Character")
          "char typename via model-map")
      (is (= (render "{{typename(x)}}" {:x true})
             "java.lang.Boolean")
          "boolean typename via model-map")
      (is (= (render "{{typename(x)}}" {:x '(1 2 3 4 5)})
             "clojure.lang.PersistentList")
          "list typename via model-map")
      (is (= (render "{{typename(x)}}" {:x [1 2 3 4 5]})
             "clojure.lang.PersistentVector")
          "vector typename via model-map")
      (is (= (render "{{typename(x)}}" {:x {:a 1 :b "foo" :c nil}})
             "clojure.lang.PersistentArrayMap")
          "map typename via model-map")
      (is (= (render "{{typename(x)}}" {:x #{1 2 3 4 5}})
             "clojure.lang.PersistentHashSet")
          "set typename via model-map")

      ; verify that the clojure function recognizes the correct types when the variable is passed via a constant
      ; value embedded in the template
      (is (= (render "{{typename(42)}}" nil)
             "java.lang.Integer")
          "integer typename via constant value embedded in the template")
      (is (= (render "{{typename(3.14)}}" nil)
             "java.lang.Double")
          "float typename via constant value embedded in the template")
      (is (= (render "{{typename('foobar')}}" nil)
             "java.lang.String")
          "string typename via constant value embedded in the template")
      (is (= (render "{{typename('a')}}" nil)
             "java.lang.Character")
          "char typename via constant value embedded in the template")
      (is (= (render "{{typename(true)}}" nil)
             "java.lang.Boolean")
          "boolean typename via constant value embedded in the template")
      (is (= (render "{{typename([1, 2, 3, 4, 5])}}" nil)
             "java.util.ArrayList")
          "list typename via constant value embedded in the template")
      (is (= (render "{{typename(1..5)}}" nil)
             "java.util.ArrayList")
          "vector typename via constant value embedded in the template")
      (is (= (render "{{typename({a: 1, b: 'foo', c: null})}}" nil)
             "java.util.HashMap")
          "map typename via constant value embedded in the template")

      ; simple passing / returning... not doing anything exciting with the arguments
      ; using a constant value embedded inside the template
      (is (= (render "{{identity(x)}}" {:x 42})
             "42")
          "integer via model-map")
      (is (= (render "{{identity(x)}}" {:x 3.14})
             "3.14")
          "float via model-map")
      (is (= (render "{{identity(x)}}" {:x "foobar"})
             "foobar")
          "string via model-map")
      (is (= (render "{{identity(x)}}" {:x \a})
             "a")
          "char via model-map")
      (is (= (render "{{identity(x)}}" {:x true})
             "true")
          "boolean via model-map")
      (is (= (render "{{identity(x)}}" {:x '(1 2 3 4 5)})
             "(1 2 3 4 5)")
          "list via model-map")
      (is (= (render "{{identity(x)}}" {:x [1 2 3 4 5]})
             "[1 2 3 4 5]")
          "vector via model-map")
      (is (= (render "{{identity(x)}}" {:x {:a 1 :b "foo" :c nil}})
             "{\"a\" 1, \"c\" nil, \"b\" \"foo\"}")
          "map via model-map")
      (is (= (render "{{identity(x)}}" {:x #{1 2 3 4 5}})
             "#{1 2 3 4 5}")
          "set via model-map")

      ; simple passing / returning... not doing anything exciting with the arguments
      ; using a constant value embedded inside the template
      (is (= (render "{{identity(42)}}" nil)
             "42")
          "integer via constant value embedded in the template")
      (is (= (render "{{identity(3.14)}}" nil)
             "3.14")
          "float via constant value embedded in the template")
      (is (= (render "{{identity('foobar')}}" nil)
             "foobar")
          "string via constant value embedded in the template")
      (is (= (render "{{identity('a')}}" nil)
             "a")
          "char via constant value embedded in the template")
      (is (= (render "{{identity(true)}}" nil)
             "true")
          "boolean via constant value embedded in the template")
      (is (= (render "{{identity([1, 2, 3, 4, 5])}}" nil)
             "[1, 2, 3, 4, 5]")
          "enumerated list via constant value embedded in the template")
      (is (= (render "{{identity(1..5)}}" nil)
             "[1, 2, 3, 4, 5]")
          "list by comprehension via constant value embedded in the template")
      (is (= (render "{{identity({a: 1, b: 'foo', c: null})}}" nil)
             "{b=foo, c=null, a=1}")
          "map via constant value embedded in the template")

      ; iterating over passed sequence/collection type arguments passed to a custom function from a variable
      ; inside the model-map and being returned
      (is (= (render "{% for i in identity(x) %}{{i}} {% endfor %}" {:x '(1 2 3 4 5)})
             "1 2 3 4 5 ")
          "list (iterating over a model-map var passed to a function and returned from it)")
      (is (= (render "{% for i in identity(x) %}{{i}} {% endfor %}" {:x [1 2 3 4 5]})
             "1 2 3 4 5 ")
          "vector (iterating over a model-map var passed to a function and returned from it)")
      (is (= (render "{% for k, v in identity(x) %}{{k}}: {{v}} {% endfor %}" {:x {:a 1 :b "foo" :c nil}})
             "a: 1 c: null b: foo ")
          "map (iterating over a model-map var passed to a function and returned from it)")
      (is (= (render "{% for i in identity(x) %}{{i}} {% endfor %}" {:x #{1 2 3 4 5}})
             "1 2 3 4 5 ")
          "set (iterating over a model-map var passed to a function and returned from it)")

      ; iterating over passed sequence/collection type arguments passed to a custom function from a constant
      ; value embedded in the template and being returned
      (is (= (render "{% for i in identity([1, 2, 3, 4, 5]) %}{{i}} {% endfor %}" nil)
             "1 2 3 4 5 ")
          "enumerated list (iterating over a model-map var passed to a function and returned from it)")
      (is (= (render "{% for i in identity(1..5) %}{{i}} {% endfor %}" nil)
             "1 2 3 4 5 ")
          "list by comprehension (iterating over a model-map var passed to a function and returned from it)")
      (is (= (render "{% for k, v in identity({a: 1, b: 'foo', c: null}) %}{{k}}: {{v}} {% endfor %}" nil)
             "b: foo c: null a: 1 ")
          "map (iterating over a model-map var passed to a function and returned from it)")

      (reset-functions!))))