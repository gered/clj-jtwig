(ns clj-jtwig.functions
  "custom template function/filter support functions."
  (:import (com.lyncode.jtwig.functions.repository FunctionResolver)
           (com.lyncode.jtwig.functions.exceptions FunctionNotFoundException FunctionException)
           (com.lyncode.jtwig.functions.annotations JtwigFunction)
           (com.lyncode.jtwig.functions.parameters GivenParameters)
           (clj_jtwig TemplateFunction))
  (:require [clj-jtwig.convert :refer [java->clojure clojure->java]]
            [clj-jtwig.function-utils :refer [make-function-handler]])
  (:use [clj-jtwig.standard-functions]
        [clj-jtwig.web.web-functions]))

(def ^:private object-array-type (Class/forName "[Ljava.lang.Object;"))

(def ^:private function-parameters (doto (GivenParameters.)
                                     (.add (to-array [object-array-type]))))

(defn- add-function-library! [repository functions]
  (doseq [fn-obj functions]
    (.store repository fn-obj))
  repository)

(defn- create-function-repository []
  (doto (new FunctionResolver)
    (add-function-library! standard-functions)
    (add-function-library! web-functions)))

; we'll be reusing the same function repository object for all contexts created when rendering templates.
; any custom functions added will be added to this instance
(defonce functions (atom (create-function-repository)))

(defn reset-functions!
  "removes any added custom template function handlers. use this with care!"
  []
  (reset! functions (create-function-repository)))

; intended for internal-use only. mainly exists for use in unit tests
(defn get-function [^String name]
  (try
    (.get @functions name function-parameters)
    (catch FunctionNotFoundException ex)))

; intended for internal-use only. mainly exists for use in unit tests
(defn function-exists? [^String name]
  (not (nil? (get-function name))))

(defmacro deftwigfn
  "defines a new template function. templates can call it by by the name specified and passing in the
   same number of arguments as in args. the return value of the last form in body is returned to the
   template. functions defined this way have no aliases and can only be called by the name given."
  [fn-name args & body]
  `(let [f# (fn ~args ~@body)]
     (.store
       @functions
       (make-function-handler ~fn-name [] f#))
     (get-function ~fn-name)))

(defmacro defaliasedtwigfn
  "defines a new template function. templates can call it by by the name specified (or one of the
   aliases specified) and passing in the same number of arguments as in args. the return value of
   the last form in body is returned to the template."
  [fn-name args aliases & body]
  `(let [f# (fn ~args ~@body)]
     (.store
       @functions
       (make-function-handler ~fn-name ~aliases f#))
     (get-function ~fn-name)))
