(ns clj-jtwig.core
  (:require [clojure.walk :refer [stringify-keys]])
  (:import (com.lyncode.jtwig JtwigTemplate JtwigContext JtwigModelMap)
           (com.lyncode.jtwig.functions.exceptions FunctionNotFoundException)
           (com.lyncode.jtwig.functions.repository DefaultFunctionRepository)
           (com.lyncode.jtwig.functions JtwigFunction)
           (java.io File FileNotFoundException)))

(defn- create-function-repository []
  (new DefaultFunctionRepository (make-array JtwigFunction 0)))

; we'll be reusing the same function repository object for all contexts created when rendering templates.
; any custom functions added will be added to this instance
(defonce functions (atom (create-function-repository)))

(defn reset-functions!
  "removes any added custom template function handlers"
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
   name specified and passing in the same number of arguments accepted by f. the return value of
   f is returned to the template.
   prefer to use the 'deftwigfn' macro when possible."
  [name f]
  (if (function-exists? name)
    (throw (new Exception (str "JTwig template function \"" name "\" already defined.")))
    (let [handler (reify JtwigFunction
                    (execute [_ arguments]
                      (apply f (vec (aclone arguments)))))]
      (.add @functions handler name (make-array String 0))
      (.retrieve @functions name))))

(defmacro deftwigfn
  "adds a new template function. templates can call it by by the name specified and passing in the
   same number of arguments as in args. the return value of the last form in body is returned to the
   template."
  [fn-name args & body]
  `(do
     (add-function! ~fn-name (fn ~args ~@body))))

(defn- get-resource-path [filename]
  (-> (Thread/currentThread)
      (.getContextClassLoader)
      (.getResource filename)))

(defn- make-context [model-map {:keys [skip-model-map-stringify?] :as options}]
  (let [context (new JtwigContext (new JtwigModelMap) @functions)]
    (doseq [[k v] (if-not skip-model-map-stringify?
                    (stringify-keys model-map)
                    model-map)]
      (.set context k v))
    context))

(defn- render-template
  [template model-map & [options]]
  (.output template (make-context model-map options)))

(defn render
  "renders a template contained in the provided string, using the values in model-map
   as the model for the template."
  [s model-map & [options]]
  (let [template (new JtwigTemplate s)]
    (render-template template model-map options)))

(defn render-file
  "renders a template from a file, using the values in model-map as the model for the template"
  [filename model-map & [options]]
  (let [file     (new File filename)
        template (new JtwigTemplate file)]
    (if-not (.exists file)
      (throw (new FileNotFoundException (str "Template file \"" filename "\" not found."))))
    (render-template template model-map options)))

(defn render-resource
  "renders a template from a resource file, using the values in the model-map as the model for
   the template."
  [filename model-map & [options]]
  (if-let [resource-filename (get-resource-path filename)]
    (render-file (.getPath resource-filename) model-map options)))
