(ns clj-jtwig.standard-functions
  "standard function definitions. these are functions that are not yet included in Jtwig's standard function
   library and are just here to fill in the gaps for now."
  (:import (org.apache.commons.lang3.text WordUtils)
           (org.apache.commons.lang3 StringUtils))
  (:use [clojure.pprint]
        [clj-jtwig.function-utils]
        [clj-jtwig.options]))

(defn- possible-keyword-string [x]
  (if (and (:auto-convert-map-keywords @options)
           (string? x))
    (keyword x)
    x))

(deflibrary standard-functions
  (library-aliased-function "blank_if_null" ["nonull"] [x]
    (if (nil? x) "" x))

  (library-function "butlast" [sequence]
    ; matching behaviour of jtwig's first/last implementation
    (if (map? sequence)
      (-> sequence vals butlast)
      (butlast sequence)))

  (library-function "center" [s size & [padding-string]]
    (StringUtils/center s size (or padding-string " ")))

  (library-function "contains" [coll value]
    (cond
      (map? coll)    (contains? coll (possible-keyword-string value))
      (string? coll) (.contains coll value)
      ; explicit use of '=' to allow testing for falsey values
      (coll? coll)   (not (nil? (some #(= value %) coll)))
      :else          (throw (new Exception (str "'contains' passed invalid collection type: " (type coll))))))

  (library-function "dump" [x]
    (with-out-str
      (clojure.pprint/pprint x)))

  (library-function "dump_table" [x]
    (with-out-str
      (clojure.pprint/print-table x)))

  (library-function "index_of" [coll value]
    (cond
      (instance? java.util.List coll) (.indexOf coll value)
      (string? coll)                  (.indexOf coll (if (char? value) (int value) value))
      :else                           (throw (new Exception (str "'index_of' passed invalid collection type: " (type coll))))))

  (library-function "last_index_of" [coll value]
    (cond
      (instance? java.util.List coll) (.lastIndexOf coll value)
      (string? coll)                  (.lastIndexOf coll (if (char? value) (int value) value))
      :else                           (throw (new Exception (str "'last_index_of' passed invalid collection type: " (type coll))))))

  (library-function "max" [& numbers]
    (if (coll? (first numbers))
      (apply max (first numbers))
      (apply max numbers)))

  (library-function "min" [& numbers]
    (if (coll? (first numbers))
      (apply min (first numbers))
      (apply min numbers)))

  (library-function "normalize_space" [s]
    (StringUtils/normalizeSpace s))

  (library-function "nth" [sequence index & optional-not-found]
    (let [values (if (map? sequence)    ; map instance check to match behaviour of jtwig's first/last implementation
                   (-> sequence vals)
                   sequence)]
      (if optional-not-found
        (nth values index (first optional-not-found))
        (nth values index))))

  (library-function "pad_left" [s size & [padding-string]]
    (StringUtils/leftPad s size (or padding-string " ")))

  (library-function "pad_right" [s size & [padding-string]]
    (StringUtils/rightPad s size (or padding-string " ")))

  (library-function "random" [& values]
    (let [first-value (first values)]
      (cond
        (and (= (count values) 1)
             (coll? first-value))
        (rand-nth first-value)

        (> (count values) 1)
        (rand-nth values)

        (string? first-value)
        (rand-nth (seq first-value))

        (number? first-value)
        (rand-int first-value)

        :else
        (rand))))

  (library-function "range" [low high & [step]]
    (range low high (or step 1)))

  (library-function "repeat" [s n]
    (StringUtils/repeat s n))

  (library-function "rest" [sequence]
    ; matching behaviour of jtwig's first/last implementation
    (if (map? sequence)
      (-> sequence vals rest)
      (rest sequence)))

  (library-function "second" [sequence]
    ; matching behaviour of jtwig's first/last implementation
    (if (map? sequence)
      (-> sequence vals second)
      (second sequence)))

  (library-function "sort" [sequence]
    (sort < sequence))

  (library-aliased-function "sort_descending" ["sort_desc"] [sequence]
    (sort > sequence))

  (library-function "sort_by" [coll k]
    (let [sort-key (possible-keyword-string k)]
      (sort-by #(get % sort-key) coll)))

  (library-aliased-function "sort_descending_by" ["sort_desc_by"] [coll k]
    (let [sort-key (possible-keyword-string k)]
      (sort-by #(get % sort-key) #(compare %2 %1) coll)))

  (library-function "to_double" [x]
    (Double/parseDouble x))

  (library-function "to_float" [x]
    (Float/parseFloat x))

  (library-function "to_int" [x]
    (Integer/parseInt x))

  (library-function "to_keyword" [x]
    (keyword x))

  (library-function "to_long" [x]
    (Long/parseLong x))

  (library-function "to_string" [x]
    (cond
      (keyword? x)                       (name x)
      (instance? clojure.lang.LazySeq x) (str (seq x))
      (coll? x)                          (str x)
      :else                              (.toString x)))

  (library-function "wrap" [s length & [wrap-long-words? new-line-string]]
    (WordUtils/wrap
      s
      length
      new-line-string
      (if (nil? wrap-long-words?)
        false
        wrap-long-words?))))