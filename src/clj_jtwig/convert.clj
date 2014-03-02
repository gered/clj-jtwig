(ns clj-jtwig.convert)

(defprotocol JavaToClojure
  (convert [x]))

(extend-protocol JavaToClojure
  java.util.Collection
  (convert [x]
    (map convert x))

  java.util.Map
  (convert [x]
    (->> x
         (.entrySet)
         (reduce
           (fn [m [k v]]
             ; TODO: perhaps we should be doing (keyword k) instead? i don't like that it technically is not an
             ;       exact conversion if we do it that way though, even if it is more idiomatic for clojure ...
             (assoc m k (convert v)))
           {})))

  java.lang.Number
  (convert [x]
    x)

  java.lang.Boolean
  (convert [x]
    x)

  java.lang.Character
  (convert [x]
    x)

  java.lang.String
  (convert [x]
    x)

  java.lang.Object
  (convert [x]
    (-> x
        (bean)    ; TODO: this is definitely not the fastest method ...
        (dissoc :class)))

  nil
  (convert [_]
    nil))

(defn java->clojure
  "converts a java value to an equivalent value using one of the clojure data types"
  [x]
  (convert x))
