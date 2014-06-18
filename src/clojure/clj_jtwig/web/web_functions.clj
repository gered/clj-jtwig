(ns clj-jtwig.web.web-functions
  "web functions, intended to be used by web applications only. most of these will require the
   current servlet context path, so use of clj-jtwig.web.middleware.wrap-servlet-context-path
   is a prerequisite for these functions."
  (:import (java.net URI))
  (:require [clj-jtwig.web.middleware :refer [*servlet-context-path*]]
            [clojure.string :as str])
  (:use [clj-jtwig.function-utils]
        [clj-jtwig.utils]
        [clj-jtwig.options]))

;; TODO: while 'public' is the default with Compojure, applications can override with something else ...
;;       should make this customizable (some option added to clj-jtwig.options likely ...)
(def root-resource-path "public")

(defn- get-context-url [url]
  (str *servlet-context-path* url))

(defn- relative-url? [url]
  (if-not (str/blank? url)
    (let [uri (new URI url)]
      (str/blank? (.getScheme uri)))))

(defn- get-resource-modification-timestamp [^String resource-url]
  (if (relative-url? resource-url)

    (->> (str root-resource-path resource-url)
         (get-context-url)
         (get-resource-modification-date))))

(defn- get-url-string [url]
  (if-let [modification-timestamp (get-resource-modification-timestamp url)]
    ; because it looks kind of dumb to have '?0' at the end of URLs when running from a jar ...
    (if (= modification-timestamp 0)
      url
      (str url "?" modification-timestamp))
    url))

(defn- minified-url? [url]
  (re-matches #"^(.+\.)min\.(css|js)$" url))

(defn- make-minified-url [^String url]
  (let [pos (.lastIndexOf url (int \.))]
    (if (> pos -1)
      (let [name      (subs url 0 pos)
            extension (subs url (inc pos))]
        (str name ".min." extension))
      url)))

(defn- get-minified-resource-url [url]
  (if (or (not (:check-for-minified-web-resources @options))
          (minified-url? url))
    url
    (let [minified-url (make-minified-url url)]
      (if (get-resource-path (str root-resource-path minified-url))
        minified-url
        url))))

(deflibrary web-functions
  (library-function "path" [url]
    (get-context-url url))

  (library-function "stylesheet" [url & [media]]
    (let [fmt           (if media
                          "<link href=\"%s\" rel=\"stylesheet\" type=\"text/css\" media=\"%s\" />"
                          "<link href=\"%s\" rel=\"stylesheet\" type=\"text/css\" />")
          resource-path (get-minified-resource-url url)]
      (format fmt (get-url-string resource-path) media)))

  (library-function "javascript" [url]
    (let [fmt           "<script type=\"text/javascript\" src=\"%s\"></script>"
          resource-path (get-minified-resource-url url)]
      (format fmt (get-url-string resource-path)))))
