(ns clj-jtwig.core
  (:require [clojure.walk :refer [stringify-keys]]
            [clojure.java.io :as io])
  (:import (com.lyncode.jtwig JtwigTemplate JtwigContext JtwigModelMap)
           (com.lyncode.jtwig.functions.exceptions FunctionNotFoundException)
           (com.lyncode.jtwig.functions.repository DefaultFunctionRepository)
           (com.lyncode.jtwig.functions JtwigFunction)
           (java.io File FileNotFoundException)))

; we'll be reusing the same function repository object for all contexts created when rendering templates.
; any custom functions added will be added to this instance
(defonce functions (atom (new DefaultFunctionRepository (make-array JtwigFunction 0))))

(defn- twig-fn-exists? [name]
  (try
    (.retrieve @functions name)
    true
    (catch FunctionNotFoundException ex
      false)))

(defmacro deftwigfn
  "Adds a new template function. Templates can call it by calling a function using the supplied
   fn-name and passing the listed arguments. The value of the last form in the function is returned
   to the template."
  [fn-name args & body]
  `(do
     (if (twig-fn-exists? ~fn-name)
       (throw (new Exception (str "JTwig template function \"" ~fn-name "\" already defined.")))
       (let [func#    (fn ~args ~@body)
             handler# (reify JtwigFunction
                        (execute [_ arguments#]
                          (apply func# (vec (aclone arguments#)))))]
         (.add @functions handler# ~fn-name (make-array String 0))))))

(defn- get-resource-path [filename]
  (-> (Thread/currentThread)
      (.getContextClassLoader)
      (.getResource filename)))

(defn- make-context []
  (new JtwigContext (new JtwigModelMap) @functions))

(defn- render-template
  [template model-map & [{:keys [skip-model-map-stringify?] :as options}]]
  (let [context (make-context)]
    (doseq [[k v] (if-not skip-model-map-stringify?
                    (stringify-keys model-map)
                    model-map)]
      (.set context k v))
    (.output template context)))

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
