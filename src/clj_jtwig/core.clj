(ns clj-jtwig.core
  "wrapper functions for working with JTwig from clojure"
  (:require [clojure.walk :refer [stringify-keys]]
            [clj-jtwig.convert :refer [java->clojure clojure->java]])
  (:import (com.lyncode.jtwig JtwigTemplate JtwigContext JtwigModelMap)
           (com.lyncode.jtwig.functions.exceptions FunctionNotFoundException)
           (com.lyncode.jtwig.functions.repository DefaultFunctionRepository)
           (com.lyncode.jtwig.functions JtwigFunction)
           (com.lyncode.jtwig.tree.api Content)
           (java.io File FileNotFoundException ByteArrayOutputStream)))

; global options
(defonce options (atom {:cache-compiled-templates true}))

(declare flush-template-cache!)

(defn toggle-compiled-template-caching!
  "toggle caching of compiled templates on/off. if off, every time a template is rendered from a file
   it will be re-loaded from disk and re-compiled before being rendered. caching is turned on by default."
  [enable?]
  ; always clear the cache when toggling. this will help ensure that any possiblity of weird behaviour from
  ; leftover stuff being stuck in the cache pre-toggle-on/off won't happen
  (flush-template-cache!)
  (swap! options assoc :cache-compiled-templates enable?))

; cache of compiled templates. key is the file path. value is a map with :last-modified which is the source file's
; last modification timestamp and :template which is a com.lyncode.jtwig.tree.api.Content object which has been
; compiled already and can be rendered by calling it's 'render' method
(defonce compiled-templates (atom {}))

(defn- compile-template-string [^String contents]
  (->> contents
       (new JtwigTemplate)
       (.compile)))

(defn- compile-template-file [^File file]
  (->> file
       (new JtwigTemplate)
       (.compile)))

(defn- inside-jar? [^File file]
  (-> file
      (.getPath)
      ; the path of a file inside a jar looks something like "jar:file:/path/to/file.jar!/path/to/file"
      (.contains "jar!")))

(defn- get-file-last-modified [^File file]
  (if (inside-jar? file)
    0
    (.lastModified file)))

(defn- newer? [^File file other-timestamp]
  (let [file-last-modified (get-file-last-modified file)]
    ; a time of 0 means the modification time couldn't be read or the file is inside a JAR container. if it's an I/O
    ; error, we'll get an exception during file reading. so, just return true indicating that the file is "newer"
    ; and assume that the file is inside a JAR in which case, we can't do proper time-based caching anyway, so lets
    ; just always re-compile
    (if (= 0 file-last-modified)
      true
      (> file-last-modified other-timestamp))))

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
    (if (and cached
             (not (newer? file (:last-modified cached))))
      (:template cached)
      (cache-and-return!))))

(defn- compile-template! [^File file]
  (if-not (.exists file)
    (throw (new FileNotFoundException (str "Template file \"" file "\" not found.")))
    (let [compile-template-fn (fn [file]
                                (compile-template-file file))]
      (if (:cache-compiled-templates @options)
        (cache-compiled-template! file compile-template-fn)
        (compile-template-fn file)))))

(defn flush-template-cache!
  "removes any cached compiled templates, forcing all future template rendering to first re-compile before
   re-adding to the cache."
  []
  (reset! compiled-templates {}))

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
                      (clojure->java (apply f (map java->clojure arguments)))))]
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

(defn- make-model-map [model-map-values {:keys [skip-model-map-stringify?] :as options}]
  (let [model-map-obj (new JtwigModelMap)
        values        (if-not skip-model-map-stringify?
                        (stringify-keys model-map-values)
                        model-map-values)]
    (doseq [[k v] values]
      (.add model-map-obj k v))
    model-map-obj))

(defn- make-context [model-map options]
  (let [model-map-obj (make-model-map model-map options)]
    (new JtwigContext model-map-obj @functions)))

(defn- render-compiled-template
  [^Content compiled-template model-map & [options]]
  (let [context (make-context model-map options)]
    ; technically we don't have to use with-open with a ByteArrayOutputStream but if we later
    ; decide to use another OutputStream implementation, this is already all set up :)
    (with-open [stream (new ByteArrayOutputStream)]
      (.render compiled-template stream context)
      (.toString stream))))

(defn render
  "renders a template contained in the provided string, using the values in model-map
   as the model for the template. templates rendered using this function are always
   parsed, compiled and rendered. the compiled results are never cached."
  [s model-map & [options]]
  (let [compiled-template (compile-template-string s)]
    (render-compiled-template compiled-template model-map options)))

(defn render-file
  "renders a template from a file, using the values in model-map as the model for the template"
  [filename model-map & [options]]
  (let [file              (new File filename)
        compiled-template (compile-template! file)]
    (render-compiled-template compiled-template model-map options)))

(defn render-resource
  "renders a template from a resource file, using the values in the model-map as the model for
   the template."
  [filename model-map & [options]]
  (if-let [resource-filename (get-resource-path filename)]
    (render-file (.getPath resource-filename) model-map options)))
