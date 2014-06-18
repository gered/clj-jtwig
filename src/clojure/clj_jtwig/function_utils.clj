(ns clj-jtwig.function-utils
  (:import (com.lyncode.jtwig.functions.exceptions FunctionException)
           (com.lyncode.jtwig.functions.annotations JtwigFunction Parameter)
           (clj_jtwig TemplateFunction))
  (:require [clj-jtwig.convert :refer [java->clojure clojure->java]]))

(defmacro make-function-handler [name aliases f]
  (let [arguments (with-meta
                    (gensym "arguments#")
                    `{Parameter {}})]
    `(reify TemplateFunction
       (~(with-meta
           'execute
           `{JtwigFunction {:name ~name :aliases ~aliases}})
        [_ ~arguments]
        (try
          (clojure->java (apply ~f (map java->clojure ~arguments)))
          (catch Exception ex#
            (throw (new FunctionException ex#))))))))

(defmacro deflibrary [name & function-handlers]
  `(def ~(symbol name)
     [~@function-handlers]))

(defmacro library-function
  [fn-name args & body]
  `(let [f# (fn ~args ~@body)]
     (make-function-handler ~fn-name [] f#)))

(defmacro library-aliased-function
  [fn-name aliases args & body]
  `(let [f# (fn ~args ~@body)]
     (make-function-handler ~fn-name ~aliases f#)))