(ns clj-jtwig.functions
  "standard functions added to jtwig contexts by default. these are in addition to the
 functions added by default in all jtwig function repository objects"
  (:import (com.lyncode.jtwig.functions JtwigFunction)
           (com.lyncode.jtwig.functions.repository DefaultFunctionRepository)
           (com.lyncode.jtwig.functions.exceptions FunctionNotFoundException FunctionException))
  (:require [clj-jtwig.convert :refer [java->clojure clojure->java]])
  (:use [clj-jtwig.standard-functions]))

(defn- make-function-handler [f]
  (reify JtwigFunction
    (execute [_ arguments]
      (try
        (clojure->java (apply f (map java->clojure arguments)))
        (catch Exception ex
          (throw (new FunctionException ex)))))))

(defn- make-aliased-array [aliases]
  (let [n     (count aliases)
        array (make-array String n)]
    (doseq [index (range n)]
      (aset array index (nth aliases index)))
    array))

(defn- create-function-repository []
  (let [repository (new DefaultFunctionRepository (make-array JtwigFunction 0))]
    ; always add our standard functions to new repository objects
    (doseq [[name {:keys [aliases fn]}] standard-functions]
      (.add repository
            (make-function-handler fn)
            name
            (make-aliased-array aliases)))
    repository))

; we'll be reusing the same function repository object for all contexts created when rendering templates.
; any custom functions added will be added to this instance
(defonce functions (atom (create-function-repository)))

(defn reset-functions!
  "removes any added custom template function handlers. use this with care!"
  []
  (reset! functions (create-function-repository)))

(defn function-exists? [name]
  (try
    (.retrieve @functions name)
    true
    (catch FunctionNotFoundException ex
      false)))

(defn add-function!
  "adds a new template function using the name specified. templates can call the function by the
   name specified (or one of the aliases specified) and passing in the same number of arguments
   accepted by f. the return value of f is returned to the template. if this function has no aliases
   then nil can be specified for the aliases arg.
   prefer to use the 'deftwigfn' macro when possible."
  [name aliases f]
  (let [handler (make-function-handler f)]
    (.add @functions handler name (make-aliased-array aliases))
    (.retrieve @functions name)))

(defmacro deftwigfn
  "adds a new template function. templates can call it by by the name specified and passing in the
   same number of arguments as in args. the return value of the last form in body is returned to the
   template. functions defined this way have no aliases and can only be called by the name given."
  [fn-name args & body]
  `(do
     (add-function! ~fn-name nil (fn ~args ~@body))))

(defmacro defaliasedtwigfn
  "adds a new template function. templates can call it by by the name specified (or one of the
   aliases specified) and passing in the same number of arguments as in args. the return value of
   the last form in body is returned to the template."
  [fn-name args aliases & body]
  `(do
     (add-function! ~fn-name ~aliases (fn ~args ~@body))))
