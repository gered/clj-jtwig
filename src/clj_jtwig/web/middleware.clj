(ns clj-jtwig.web.middleware)

(declare ^:dynamic *servlet-context-path*)

(defn wrap-servlet-context-path
  "Binds the current request's context path to a var which we can use in
   various jtwig functions that need it without having to explicitly
   pass the path in as a function parameter."
  [handler]
  (fn [req]
    (binding [*servlet-context-path* (:context req)]
      (handler req))))