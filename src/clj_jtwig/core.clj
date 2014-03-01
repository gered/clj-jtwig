(ns clj-jtwig.core
  (:require [clojure.walk :refer [stringify-keys]]
            [clojure.java.io :as io])
  (:import (com.lyncode.jtwig JtwigTemplate JtwigContext)
           (com.lyncode.jtwig.functions.repository DefaultFunctionRepository)
           (com.lyncode.jtwig.functions JtwigFunction)
           (java.io File FileNotFoundException)))

; we'll be reusing the same function repository object for all contexts created when rendering templates.
; any custom functions added will be added to this instance
(defonce functions (atom (new DefaultFunctionRepository (make-array JtwigFunction 0))))

(defn- get-resource-path [filename]
  (-> (Thread/currentThread)
      (.getContextClassLoader)
      (.getResource filename)))

(defn- render-template
  [template model-map & [{:keys [skip-model-map-stringify?] :as options}]]
  (let [context (new JtwigContext)]
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
