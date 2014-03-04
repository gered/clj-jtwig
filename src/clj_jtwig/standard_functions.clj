(ns clj-jtwig.standard-functions
  "standard function definitions. these are functions that are not yet included in JTwig's standard function
   library and are just here to fill in the gaps for now."
  (:use [clojure.pprint]))

; we are using a separate map to hold the standard functions instead of using deftwigfn, etc. because doing it this
; way makes it easy to re-add all these functions when/if the JTwig function repository object needs to be
; recreated (e.g. during unit tests).

; the keys are function names. each value is a map containing :fn which is the actual function, and an optional
; :aliases, which should be a vector of strings containing one or more possible aliases for this function.

(defonce standard-functions
  {"blankIfNull"
   {:fn (fn [x]
          (if (nil? x) "" x))}

   "butlast"
   {:fn (fn [sequence]
          ; matching behaviour of jtwig's first/last implementation
          (if (map? sequence)
            (-> sequence vals butlast)
            (butlast sequence)))}

   "dump"
   {:fn (fn [x]
          (with-out-str
            (clojure.pprint/pprint x)))}

   "nth"
   {:fn (fn [sequence index & optional-not-found]
          (let [values (if (map? sequence)    ; map instance check to match behaviour of jtwig's first/last implementation
                         (-> sequence vals)
                         sequence)]
            (if optional-not-found
              (nth values index (first optional-not-found))
              (nth values index))))}

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

   "sortDescending"
   {:fn (fn [sequence]
          (sort > sequence))}

   "sortBy"
   {:fn (fn [coll k]
          (sort-by #(get % k) coll))}

   "sortDescendingBy"
   {:fn (fn [coll k]
          (sort-by #(get % k) #(compare %2 %1) coll))}})
