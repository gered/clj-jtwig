(ns clj-jtwig.core
  (:require [clojure.walk :refer [stringify-keys]])
  (:import (com.lyncode.jtwig JtwigTemplate JtwigContext)))

(defn render
  "renders a template contained in the provided string, using the values in model-map
   as the model for the template."
  [s model-map & {:keys [skip-model-map-stringify?] :as options}]
  (let [template (new JtwigTemplate s)
        context  (new JtwigContext)]
    (doseq [[k v] (if-not skip-model-map-stringify?
                    (stringify-keys model-map)
                    model-map)]
      (.set context k v))
    (.output template context)))
