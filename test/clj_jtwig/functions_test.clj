(ns clj-jtwig.functions-test
  (:import (com.lyncode.jtwig.functions.repository CallableFunction))
  (:require [clojure.test :refer :all]
            [clj-jtwig.core :refer :all]
            [clj-jtwig.functions :refer :all]))

(defn valid-function-handler? [x]
  (and (not (nil? x))
       (instance? CallableFunction x)))

(deftest template-functions
  (testing "Adding custom template functions"
    (do
      (reset-functions!)

      (is (valid-function-handler?
            (deftwigfn "add" [a b]
              (+ a b))))

      (is (true? (function-exists? "add")))
      (is (false? (function-exists? "foobar")))

      (is (valid-function-handler?
            (deftwigfn "add" [a b]
              (+ a b))))

      (is (= (render "{{add(1, 2)}}")
             "3")
          "calling a custom function")
      (is (= (render "{{add(a, b)}}" {:a 1 :b 2})
             "3")
          "calling a custom function, passing in variables from the model-map as arguments")
      (is (= (render "{{x|add(1)}}" {:x 1})
             "2")
          "calling a custom function using the 'filter' syntax")

      (reset-functions!)))

  (testing "Custom template function aliases"
    (do
      (reset-functions!)

      (is (valid-function-handler?
            (defaliasedtwigfn "add" [a b]
              ["plus" "myAddFn"]
              (+ a b))))

      (is (true? (function-exists? "add")))
      (is (true? (function-exists? "plus")))
      (is (true? (function-exists? "myAddFn")))

      (is (= (render "{{add(1, 2)}}")
             "3")
          "calling a custom function by name")
      (is (= (render "{{plus(1, 2)}}")
             "3")
          "calling a custom function by alias")
      (is (= (render "{{myAddFn(1, 2)}}")
             "3")
          "calling a custom function by another alias")

      (reset-functions!)))

  (testing "Fixed and variable number of template function arguments"
    (do
      (reset-functions!)

      (is (valid-function-handler?
            (deftwigfn "add2" [a b]
              (+ a b))))
      (is (true? (function-exists? "add2")))
      (is (valid-function-handler?
            (deftwigfn "addAll" [& numbers]
              (apply + numbers))))
      (is (true? (function-exists? "addAll")))

      (is (= (render "{{add2(1, 2)}}")
             "3")
          "fixed number of arguments (correct amount)")
      (is (thrown?
            Exception
            (render "{{add2(1)}}")))
      (is (= (render "{{addAll(1, 2, 3, 4, 5)}}")
             "15")
          "variable number of arguments (non-zero)")
      (is (= (render "{{addAll}}")
             "")
          "variable number of arguments (zero)")

      (reset-functions!)))

  (testing "Passing different data structures to template functions"
    (do
      (reset-functions!)

      (is (valid-function-handler?
            (deftwigfn "identity" [x]
              x)))
      (is (true? (function-exists? "identity")))
      (is (valid-function-handler?
            (deftwigfn "typename" [x]
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
             "clojure.lang.LazySeq")
          "list typename via model-map")
      (is (= (render "{{typename(x)}}" {:x [1 2 3 4 5]})
             "clojure.lang.LazySeq")
          "vector typename via model-map")
      (is (= (render "{{typename(x)}}" {:x {:a 1 :b "foo" :c nil}})
             "clojure.lang.PersistentArrayMap")
          "map typename via model-map")
      (is (= (render "{{typename(x)}}" {:x #{1 2 3 4 5}})
             "clojure.lang.LazySeq")
          "set typename via model-map")

      ; verify that the clojure function recognizes the correct types when the variable is passed via a constant
      ; value embedded in the template
      (is (= (render "{{typename(42)}}")
             "java.lang.Integer")
          "integer typename via constant value embedded in the template")
      (is (= (render "{{typename(3.14)}}")
             "java.lang.Double")
          "float typename via constant value embedded in the template")
      (is (= (render "{{typename('foobar')}}")
             "java.lang.String")
          "string typename via constant value embedded in the template")
      (is (= (render "{{typename('a')}}")
             "java.lang.Character")
          "char typename via constant value embedded in the template")
      (is (= (render "{{typename(true)}}")
             "java.lang.Boolean")
          "boolean typename via constant value embedded in the template")
      (is (= (render "{{typename([1, 2, 3, 4, 5])}}")
             "clojure.lang.LazySeq")
          "list typename via constant value embedded in the template")
      (is (= (render "{{typename(1..5)}}")
             "clojure.lang.LazySeq")
          "vector typename via constant value embedded in the template")
      (is (= (render "{{typename({a: 1, b: 'foo', c: null})}}")
             "clojure.lang.PersistentArrayMap")
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
             "1")
          "boolean via model-map")
      (is (= (render "{{identity(x)}}" {:x '(1 2 3 4 5)})
             "[1, 2, 3, 4, 5]")
          "list via model-map")
      (is (= (render "{{identity(x)}}" {:x [1 2 3 4 5]})
             "[1, 2, 3, 4, 5]")
          "vector via model-map")
      ; TODO: order of iteration through a map is undefined, the string being tested may not always be the same (wrt. order)
      (is (= (render "{{identity(x)}}" {:x {:a 1 :b "foo" :c nil}})
             "{b=foo, c=null, a=1}")
          "map via model-map")
      ; TODO: fix test, set->vector conversion performs an iteration through the set (the order is undefined)
      (is (= (render "{{identity(x)}}" {:x #{1 2 3 4 5}})
             "[1, 4, 3, 2, 5]")
          "set via model-map")

      ; simple passing / returning... not doing anything exciting with the arguments
      ; using a constant value embedded inside the template
      (is (= (render "{{identity(42)}}")
             "42")
          "integer via constant value embedded in the template")
      (is (= (render "{{identity(3.14)}}")
             "3.14")
          "float via constant value embedded in the template")
      (is (= (render "{{identity('foobar')}}")
             "foobar")
          "string via constant value embedded in the template")
      (is (= (render "{{identity('a')}}")
             "a")
          "char via constant value embedded in the template")
      (is (= (render "{{identity(true)}}")
             "1")
          "boolean via constant value embedded in the template")
      (is (= (render "{{identity([1, 2, 3, 4, 5])}}")
             "[1, 2, 3, 4, 5]")
          "enumerated list via constant value embedded in the template")
      (is (= (render "{{identity(1..5)}}")
             "[1, 2, 3, 4, 5]")
          "list by comprehension via constant value embedded in the template")
      ; TODO: order of iteration through a map is undefined, the string being tested may not always be the same (wrt. order)
      (is (= (render "{{identity({a: 1, b: 'foo', c: null})}}")
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
      ; TODO: order of iteration through a map is undefined, the string being tested may not always be the same (wrt. order)
      (is (= (render "{% for k, v in identity(x) %}{{k}}: {{v}} {% endfor %}" {:x {:a 1 :b "foo" :c nil}})
             "b: foo c:  a: 1 ")
          "map (iterating over a model-map var passed to a function and returned from it)")
      (is (= (render "{% for i in identity(x) %}{{i}} {% endfor %}" {:x #{1 2 3 4 5}})
             "1 4 3 2 5 ")
          "set (iterating over a model-map var passed to a function and returned from it)")

      ; iterating over passed sequence/collection type arguments passed to a custom function from a constant
      ; value embedded in the template and being returned
      (is (= (render "{% for i in identity([1, 2, 3, 4, 5]) %}{{i}} {% endfor %}")
             "1 2 3 4 5 ")
          "enumerated list (iterating over a model-map var passed to a function and returned from it)")
      (is (= (render "{% for i in identity(1..5) %}{{i}} {% endfor %}")
             "1 2 3 4 5 ")
          "list by comprehension (iterating over a model-map var passed to a function and returned from it)")
      ; TODO: order of iteration through a map is undefined, the string being tested may not always be the same (wrt. order)
      (is (= (render "{% for k, v in identity({a: 1, b: 'foo', c: null}) %}{{k}}: {{v}} {% endfor %}")
             "b: foo c:  a: 1 ")
          "map (iterating over a model-map var passed to a function and returned from it)")

      (reset-functions!))))

(deftest standard-functions
  (testing "Standard functions were added properly"
    (is (true? (function-exists? "blank_if_null")))
    (is (true? (function-exists? "butlast")))
    (is (true? (function-exists? "dump")))
    (is (true? (function-exists? "dump_table")))
    (is (true? (function-exists? "nth")))
    (is (true? (function-exists? "max")))
    (is (true? (function-exists? "min")))
    (is (true? (function-exists? "random")))
    (is (true? (function-exists? "range")))
    (is (true? (function-exists? "rest")))
    (is (true? (function-exists? "second")))
    (is (true? (function-exists? "sort")))
    (is (true? (function-exists? "sort_descending")))
    (is (true? (function-exists? "sort_by")))
    (is (true? (function-exists? "sort_descending_by"))))

  (testing "blank_if_null"
    (is (= (render "{{ a|blank_if_null }}")
           ""))
    (is (= (render "{{ a|blank_if_null }}" {:a nil})
           ""))
    (is (= (render "{{ a|blank_if_null }}" {:a "foo"})
           "foo"))
    (is (= (render "{{ a|nonull }}")
           "")))

  (testing "butlast"
    (is (= (render "{{ [1, 2, 3, 4, 5]|butlast }}")
           "[1, 2, 3, 4]")))

  (testing "center"
    (is (= (render "{{ center('bat', 5) }}")
           " bat "))
    (is (= (render "{{ center('bat', 3) }}")
           "bat"))
    (is (= (render "{{ center('bat', 5, 'x') }}")
           "xbatx")))

  (testing "contains"
    (is (= (render "{{ {a: 1, b: 2, c: 3}|contains(\"b\") }}")
           "1"))
    (is (= (render "{{ {a: 1, b: 2, c: 3}|contains(\"d\") }}")
           "0"))
    (is (= (render "{{ [1, 2, 3, 4]|contains(2) }}")
           "1"))
    (is (= (render "{{ [1, 2, 3, 4]|contains(5) }}")
           "0"))
    (is (= (render "{{ \"abcdef\"|contains(\"abc\") }}")
           "1"))
    (is (= (render "{{ \"abcdef\"|contains(\"xyz\") }}")
           "0")))

  (testing "dump"
    (is (= (render "{{ a|dump }}" {:a [{:foo "bar"} [1, 2, 3] "hello"]})
           "({:foo \"bar\"} (1 2 3) \"hello\")\n")))

  (testing "dump_table"
    (is (= (render "{{ t|dump_table }}", {:t [{:a 1 :b 2 :c 3} {:b 5 :a 7 :c "dog"}]})
           "\n| :a | :b |  :c |\n|----+----+-----|\n|  1 |  2 |   3 |\n|  7 |  5 | dog |\n")))

  (testing "index_of"
    (is (= (render "{{ [1, 2, 3, 2, 1]|index_of(2) }}")
           "1"))
    (is (= (render "{{ [1, 2, 3, 2, 1]|index_of(5) }}")
           "-1"))
    (is (= (render "{{ \"abcdcba\"|index_of(\"b\") }}")
           "1"))
    (is (= (render "{{ \"abcdcba\"|index_of(\"z\") }}")
           "-1")))

  (testing "last_index_of"
    (is (= (render "{{ [1, 2, 3, 2, 1]|last_index_of(2) }}")
           "3"))
    (is (= (render "{{ [1, 2, 3, 2, 1]|last_index_of(5) }}")
           "-1"))
    (is (= (render "{{ \"abcdcba\"|last_index_of(\"b\") }}")
           "5"))
    (is (= (render "{{ \"abcdcba\"|last_index_of(\"z\") }}")
           "-1")))

  (testing "max"
    (is (= (render "{{ [2, 1, 5, 3, 4]|max }}")
           "5"))
    (is (= (render "{{ max(2, 1, 5, 3, 4) }}")
           "5")))

  (testing "min"
    (is (= (render "{{ [2, 1, 5, 3, 4]|min }}")
           "1"))
    (is (= (render "{{ min(2, 1, 5, 3, 4) }}")
           "1")))

  (testing "normalize_space"
    (is (= (render "{{ normalize_space('  hello  world  ') }}")
           "hello world")))

  (testing "nth"
    (is (= (render "{{ [1, 2, 3, 4, 5]|nth(2) }}")
           "3"))
    (is (thrown?
          Exception
          (render "{{ [1, 2, 3, 4, 5]|nth(6) }}")))
    (is (= (render "{{ [1, 2, 3, 4, 5]|nth(6, \"not found\") }}")
           "not found")))

  (testing "pad_left"
    (is (= (render "{{ pad_left('bat', 5) }}")
           "  bat"))
    (is (= (render "{{ pad_left('bat', 3) }}")
           "bat"))
    (is (= (render "{{ pad_left('bat', 5, 'x') }}")
           "xxbat")))

  (testing "pad_right"
    (is (= (render "{{ pad_right('bat', 5) }}")
           "bat  "))
    (is (= (render "{{ pad_right('bat', 3) }}")
           "bat"))
    (is (= (render "{{ pad_right('bat', 5, 'x') }}")
           "batxx")))

  (testing "random"
    (is (some #{(render "{{ ['apple', 'orange', 'citrus']|random }}")}
              ["apple" "orange" "citrus"]))
    (is (some #{(render "{{ \"ABC\"|random }}")}
              ["A" "B" "C"])))

  (testing "range"
    (is (= (render "{{ range(1, 5) }}")
           "[1, 2, 3, 4]"))
    (is (= (render "{{ range(1, 5, 2) }}")
           "[1, 3]")))

  (testing "repeat"
    (is (= (render "{{ repeat('x', 10) }}")
           "xxxxxxxxxx"))
    (is (= (render "{{ repeat('x', 0) }}")
           "")))

  (testing "rest"
    (is (= (render "{{ [1, 2, 3, 4, 5]|rest }}")
           "[2, 3, 4, 5]")))

  (testing "second"
    (is (= (render "{{ [1, 2, 3, 4, 5]|second }}")
           "2")))

  (testing "sort"
    (is (= (render "{{ [2, 1, 5, 3, 4]|sort }}")
           "[1, 2, 3, 4, 5]")))

  (testing "sort_descending"
    (is (= (render "{{ [2, 1, 5, 3, 4]|sort_descending }}")
           "[5, 4, 3, 2, 1]")))

  (testing "sort_by"
    (is (= (render "{{ [{a: 2}, {a: 1}, {a: 5}, {a: 3}, {a: 4}]|sort_by(\"a\") }}")
           "[{a=1}, {a=2}, {a=3}, {a=4}, {a=5}]")))

  (testing "sort_descending_by"
    (is (= (render "{{ [{a: 2}, {a: 1}, {a: 5}, {a: 3}, {a: 4}]|sort_descending_by(\"a\") }}")
           "[{a=5}, {a=4}, {a=3}, {a=2}, {a=1}]")))

  (testing "wrap"
    (is (= (render "{{ wrap(\"Here is one line of text that is going to be wrapped after 20 columns.\", 20) }}")
           "Here is one line of\ntext that is going\nto be wrapped after\n20 columns."))
    (is (= (render "{{ wrap(\"Here is one line of text that is going to be wrapped after 20 columns.\", 20, false, \"<br />\") }}")
           "Here is one line of<br />text that is going<br />to be wrapped after<br />20 columns."))
    (is (= (render "{{ wrap(\"Click here to jump to the commons website - http://commons.apache.org\", 20, false) }}")
           "Click here to jump\nto the commons\nwebsite -\nhttp://commons.apache.org"))
    (is (= (render "{{ wrap(\"Click here to jump to the commons website - http://commons.apache.org\", 20, true) }}")
           "Click here to jump\nto the commons\nwebsite -\nhttp://commons.apach\ne.org"))))
