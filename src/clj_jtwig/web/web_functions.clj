(ns clj-jtwig.web.web-functions
  "web functions, intended to be used by web applications only. most of these will require the
   current servlet context path, so use of clj-jtwig.web.middleware.wrap-servlet-context-path
   is a prerequisite for these functions."
  (:import (java.net URI))
  (:require [clj-jtwig.web.middleware :refer [*servlet-context-path*]]
            [clojure.string :as str])
  (:use [clj-jtwig.utils]))

(defn- get-context-url [url]
  (str *servlet-context-path* url))

(defn- relative-url? [url]
  (if-not (str/blank? url)
    (let [uri (new URI url)]
      (str/blank? (.getScheme uri)))))

(defn- get-url-string [url]
  (if-let [modification-timestamp (if (relative-url? url)
                                    ;; TODO: while 'public' is the default with Compojure, applications can override with something else ...
                                    (->> (str "public" url)
                                         (get-context-url)
                                         (get-resource-modification-date)))]
    (str url "?" modification-timestamp)
    url))

; defined using the same type of map structure as in clj-jtwig.standard-functions

(defonce web-functions
  {"path"
   {:fn (fn [url]
          (get-context-url url))}

   "stylesheet"
   {:fn (fn [url & [media]]
          (let [fmt (if media
                      "<link href=\"%s\" rel=\"stylesheet\" type=\"text/css\" media=\"%s\" />"
                      "<link href=\"%s\" rel=\"stylesheet\" type=\"text/css\" />")]
            (format fmt (get-url-string url) media)))}

   "javascript"
   {:fn (fn [url]
          (format "<script type=\"text/javascript\" src=\"%s\"></script>" (get-url-string url)))}})
