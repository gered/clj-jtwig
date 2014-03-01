(ns clj-jtwig.core-test
  (:import (java.io FileNotFoundException))
  (:require [clojure.test :refer :all]
            [clj-jtwig.core :refer :all]))

; The purpose of these tests is to establish that our wrapper around JTwig works. That is,
; we will be focusing on stuff like making sure that passing Clojure data structures
; (e.g. maps, vectors, lists) over to JTwig works fine.
; JTwig includes its own test suite which tests actual template parsing and evaluation
; functionality, so there's no point in duplicating that kind of testing here once we
; establish that the above mentioned stuff works fine.

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