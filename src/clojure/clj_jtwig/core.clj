(ns clj-jtwig.core
  "wrapper functions for working with Jtwig from clojure"
  (:import (com.lyncode.jtwig JtwigTemplate JtwigContext JtwigModelMap)
           (com.lyncode.jtwig.content.api Renderable)
           (com.lyncode.jtwig.configuration JtwigConfiguration)
           (com.lyncode.jtwig.render RenderContext)
           (com.lyncode.jtwig.render.config RenderConfiguration)
           (com.lyncode.jtwig.resource ClasspathJtwigResource StringJtwigResource FileJtwigResource)
           (java.io File FileNotFoundException ByteArrayOutputStream)
           (java.net URL))
  (:require [clojure.walk :refer [stringify-keys]])
  (:use [clj-jtwig.functions]
        [clj-jtwig.utils]
        [clj-jtwig.options]))

(defonce configuration (JtwigConfiguration.))

(declare flush-template-cache!)

(defn set-options!
  "sets global options. can specify values for all options or just the ones you care about. changing some
   options via this function can trigger various important 'house-keeping' operations, so you should
   always use this function rather then manually updating clj-jtwig.options/options.
   see clj-jtwig.options for the option keys you can specify here."
  [& opts]
  (doseq [[k v] (apply hash-map opts)]
    (cond
      (= k :cache-compiled-templates)
      ; always clear the cache when toggling. this will help ensure that any possiblity of weird behaviour from
      ; leftover stuff being stuck in the cache pre-toggle-on/off won't happen
      (flush-template-cache!)

      (= k :strict-mode)
      (-> configuration .render (.strictMode v)))

    (swap! options assoc k v)))

; cache of compiled templates. key is the file path. value is a map with :last-modified which is the source file's
; last modification timestamp and :template which is a com.lyncode.jtwig.content.api.Renderable object which has been
; compiled already and can be rendered by calling it's 'render' method
(defonce compiled-templates (atom {}))

(defn- compile-template-string [^String contents]
  (-> contents
      (StringJtwigResource.)
      (JtwigTemplate. configuration)
      (.compile)))

(defn- compile-template-file [^File file]
  (if (inside-jar? file)
    (-> (.getPath file)
        (get-jar-resource-filename)
        (ClasspathJtwigResource.)
        (JtwigTemplate. configuration)
        (.compile))
    (-> file
        (FileJtwigResource.)
        (JtwigTemplate. configuration)
        (.compile))))

(defn- newer? [^File file other-timestamp]
  (let [file-last-modified (get-file-last-modified file)]
    ; a time of 0 means the modification time couldn't be read or the file is inside a JAR container. if it's an I/O
    ; error, we'll get an exception during file reading. so, just return true indicating that the file is "newer"
    ; and assume that the file is inside a JAR in which case, we can't do proper time-based caching anyway, so lets
    ; just always re-compile
    (if (= 0 file-last-modified)
      true
      (> file-last-modified other-timestamp))))

; this function really only exists so i can easily change the exception type / message in the future
; since this file-exists check is needed in a few places
(defn- err-if-no-file [^File file]
  (if-not (exists? file)
    (throw (new FileNotFoundException (str "Template file \"" file "\" not found.")))))

(defn- cache-compiled-template! [^File file create-fn]
  (let [filepath          (.getPath file)
        cache-and-return! (fn []
                            (let [new-compiled-template (create-fn file)]
                              (swap!
                                compiled-templates
                                assoc
                                filepath
                                {:last-modified (get-file-last-modified file)
                                 :template      new-compiled-template})
                              new-compiled-template))
        cached            (get @compiled-templates filepath)]
    (cond
      (not cached)
      (do
        (err-if-no-file file)
        (cache-and-return!))

      (:skip-file-status-checks @options)
      (:template cached)

      :else
      (do
        (err-if-no-file file)
        (if (newer? file (:last-modified cached))
          (cache-and-return!)
          (:template cached))))))

(defn- compile-template! [^File file]
  (let [compile-template-fn (fn [file]
                              (compile-template-file file))]
    (if (:cache-compiled-templates @options)
      (cache-compiled-template! file compile-template-fn)
      (do
        (err-if-no-file file)
        (compile-template-fn file)))))

(defn flush-template-cache!
  "removes any cached compiled templates, forcing all future template rendering to first re-compile before
   re-adding to the cache."
  []
  (reset! compiled-templates {}))

(defn- make-model-map [model-map-values]
  (let [model-map-obj (new JtwigModelMap)
        values        (if (:auto-convert-map-keywords @options)
                        (stringify-keys model-map-values)
                        model-map-values)]
    (doseq [[k v] values]
      (.add model-map-obj k v))
    model-map-obj))

(defn- make-context [model-map]
  (let [model-map-obj (make-model-map model-map)]
    (new JtwigContext model-map-obj @functions)))

(defn- render-compiled-template
  [^Renderable renderable model-map]
  (with-open [stream (new ByteArrayOutputStream)]
    (let [context        (make-context model-map)
          render-context (RenderContext/create (.render configuration) context stream)]
      (.render renderable render-context)
      (.toString stream))))

(defn render
  "renders a template contained in the provided string, using the values in model-map
   as the model for the template. templates rendered using this function are always
   parsed, compiled and rendered. the compiled results are never cached."
  [^String s & [model-map]]
  (let [renderable (compile-template-string s)]
    (render-compiled-template renderable model-map)))

(defn render-file
  "renders a template from a file, using the values in model-map as the model for the template"
  [^String filename & [model-map]]
  (let [file       (new File filename)
        renderable (compile-template! file)]
    (render-compiled-template renderable model-map)))

(defn render-resource
  "renders a template from a resource file, using the values in the model-map as the model for
   the template."
  [^String filename & [model-map]]
  (if-let [resource-filename (get-resource-path filename)]
    (render-file (.getPath resource-filename) model-map)
    (throw (new FileNotFoundException (str "Template file \"" filename "\" not found.")))))
