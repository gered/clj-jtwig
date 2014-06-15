(ns clj-jtwig.standard-functions
  "standard function definitions. these are functions that are not yet included in JTwig's standard function
   library and are just here to fill in the gaps for now."
  (:import (org.apache.commons.lang3.text WordUtils)
           (org.apache.commons.lang3 StringUtils))
  (:use [clojure.pprint]))

; we are using a separate map to hold the standard functions instead of using deftwigfn, etc. because doing it this
; way makes it easy to re-add all these functions when/if the JTwig function repository object needs to be
; recreated (e.g. during unit tests).

; the keys are function names. each value is a map containing :fn which is the actual function, and an optional
; :aliases, which should be a vector of strings containing one or more possible aliases for this function.

(defonce standard-functions
  {"blank_if_null"
   {:fn (fn [x]
          (if (nil? x) "" x))
    :aliases ["nonull"]}

   "butlast"
   {:fn (fn [sequence]
          ; matching behaviour of jtwig's first/last implementation
          (if (map? sequence)
            (-> sequence vals butlast)
            (butlast sequence)))}

   "center"
   {:fn (fn [s size & [padding-string]]
          (StringUtils/center s size (or padding-string " ")))}

   "contains"
   {:fn (fn [coll value]
          (if (map? coll)
            (contains? coll value)
            ; explicit use of '=' to allow testing for falsey values
            (some #(= value %) coll)))}

   "dump"
   {:fn (fn [x]
          (with-out-str
            (clojure.pprint/pprint x)))}

   "dump_table"
   {:fn (fn [x]
          (with-out-str
            (clojure.pprint/print-table x)))}

   "index_of"
   {:fn (fn [coll value]
          (cond
            (instance? java.util.List coll) (.indexOf coll value)
            (string? coll)                  (.indexOf coll (if (char? value) (int value) value))
            :else                           (throw (new Exception (str "'index_of' passed invalid collection type: " (type coll))))))}

   "last_index_of"
   {:fn (fn [coll value]
          (cond
            (instance? java.util.List coll) (.lastIndexOf coll value)
            (string? coll)                  (.lastIndexOf coll (if (char? value) (int value) value))
            :else                           (throw (new Exception (str "'last_index_of' passed invalid collection type: " (type coll))))))}

   "max"
   {:fn (fn [& numbers]
          (if (coll? (first numbers))
            (apply max (first numbers))
            (apply max numbers)))}

   "min"
   {:fn (fn [& numbers]
          (if (coll? (first numbers))
            (apply min (first numbers))
            (apply min numbers)))}

   "normalize_space"
   {:fn (fn [s]
          (StringUtils/normalizeSpace s))}

   "nth"
   {:fn (fn [sequence index & optional-not-found]
          (let [values (if (map? sequence)    ; map instance check to match behaviour of jtwig's first/last implementation
                         (-> sequence vals)
                         sequence)]
            (if optional-not-found
              (nth values index (first optional-not-found))
              (nth values index))))}

   "pad_left"
   {:fn (fn [s size & [padding-string]]
          (StringUtils/leftPad s size (or padding-string " ")))}

   "pad_right"
   {:fn (fn [s size & [padding-string]]
          (StringUtils/rightPad s size (or padding-string " ")))}

   "random"
   {:fn (fn [& values]
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
              (rand))))}

   "range"
   {:fn (fn [low high & [step]]
          (range low high (or step 1)))}

   "repeat"
   {:fn (fn [s n]
          (StringUtils/repeat s n))}

   "rest"
   {:fn (fn [sequence]
          ; matching behaviour of jtwig's first/last implementation
          (if (map? sequence)
            (-> sequence vals rest)
            (rest sequence)))}

   "second"
   {:fn (fn [sequence]
          ; matching behaviour of jtwig's first/last implementation
          (if (map? sequence)
            (-> sequence vals second)
            (second sequence)))}

   "sort"
   {:fn (fn [sequence]
          (sort < sequence))}

   "sort_descending"
   {:fn (fn [sequence]
          (sort > sequence))
    :aliases ["sort_desc"]}

   "sort_by"
   {:fn (fn [coll k]
          (sort-by #(get % k) coll))}

   "sort_descending_by"
   {:fn (fn [coll k]
          (sort-by #(get % k) #(compare %2 %1) coll))
    :aliases ["sort_desc_by"]}

   "wrap"
   {:fn (fn [s length & [wrap-long-words? new-line-string]]
          (WordUtils/wrap
            s
            length
            new-line-string
            (if (nil? wrap-long-words?)
              false
              wrap-long-words?)))}})
