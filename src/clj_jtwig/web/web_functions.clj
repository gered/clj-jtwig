(ns clj-jtwig.web.web-functions
  "web functions, intended to be used by web applications only. most of these will require the
   current servlet context path, so use of clj-jtwig.web.middleware.wrap-servlet-context-path
   is a prerequisite for these functions."
  (:require [clj-jtwig.web.middleware :refer [*servlet-context-path*]]))

; defined using the same type of map structure as in clj-jtwig.standard-functions

(defonce web-functions
  {"path"
   {:fn (fn [url]
          (str *servlet-context-path* url))}})
