(ns clj-jtwig.convert
  "functions for converting data types both ways between clojure and java")

(defprotocol JavaToClojure
  (to-clojure [x]))

(extend-protocol JavaToClojure
  java.util.Collection
  (to-clojure [x]
    ; REVIEW: using doall to force complete evaluation because map will otherwise return a LazySeq based on a java
    ;         collection. want to avoid any potential scenarios where the clojure code using this converted value
    ;         doesn't have time to fully evaluate the sequence before any other java code modifies the underlying
    ;         collection object ... or am i just being paranoid? :)
    (->> x
         (map to-clojure)
         (doall)))

  java.util.Map
  (to-clojure [x]
    (->> x
         (.entrySet)
         (reduce
           (fn [m [k v]]
             (assoc m (to-clojure k) (to-clojure v)))
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

  clojure.lang.Keyword
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