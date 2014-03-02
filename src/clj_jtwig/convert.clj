(ns clj-jtwig.convert)

(defprotocol JavaToClojure
  (to-clojure [x]))

(extend-protocol JavaToClojure
  java.util.Collection
  (to-clojure [x]
    (map to-clojure x))

  java.util.Map
  (to-clojure [x]
    (->> x
         (.entrySet)
         (reduce
           (fn [m [k v]]
             ; TODO: perhaps we should be doing (keyword k) instead? i don't like that it technically is not an
             ;       exact conversion if we do it that way though, even if it is more idiomatic for clojure ...
             (assoc m k (to-clojure v)))
           {})))

  java.lang.Number
  (to-clojure [x]
    x)

  java.lang.Boolean
  (to-clojure [x]
    x)

  java.lang.Character
  (to-clojure [x]
    x)

  java.lang.String
  (to-clojure [x]
    x)

  java.lang.Object
  (to-clojure [x]
    (-> x
        (bean)    ; TODO: this is definitely not the fastest method ...
        (dissoc :class)))

  nil
  (to-clojure [_]
    nil))

(defprotocol ClojureToJava
  (to-java [x]))

(extend-protocol ClojureToJava
  clojure.lang.IPersistentMap
  (to-java [x]
    (let [hashmap (new java.util.HashMap (count x))]
      (doseq [[k v] x]
        (.put hashmap (to-java k) (to-java v)))
      hashmap))

  clojure.lang.IPersistentCollection
  (to-java [x]
    (let [array (new java.util.ArrayList (count x))]
      (doseq [item x]
        (.add array (to-java item)))
      array))

  java.lang.Object
  (to-java [x]
    x)

  nil
  (to-java [x]
    nil))

(defn java->clojure
  "converts a java value to an equivalent value using one of the clojure data types"
  [x]
  (to-clojure x))

(defn clojure->java
  "converts a clojure value to an equivalent value using a java object"
  [x]
  (to-java x))
