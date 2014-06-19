(ns clj-jtwig.options
  "Options controlling certain behaviours of clj-jtwig. You should prefer to change these options through any
   functions in clj-jtwig.core instead of directly modifying the values in the options map below.")

; global options
(defonce options
  (atom
    {; true/false to enable/disable compiled template caching when using templates from
     ; files only. this does not affect templates being rendered directly from strings
     :cache-compiled-templates         true

     ; true/false to enable/disable file status checks (existance of file and last modification
     ; date/time check). if true, these checks will be skipped ONLY if a compiled template for
     ; the filepath given is cached already. if this is true and an attempt is made to render
     ; a template which is not yet cached, these checks will still be run (this is to ensure that
     ; templates can still be loaded and compiled the first time they are rendered).
     ; if caching is completely disabled (via the above option), then this setting is ignored and
     ; file status checks will always be performed.
     ; this option is intended to help increase performance when you know in advance that your
     ; templates will not be modified/deleted after they are first compiled and you want to skip
     ; any unnecessary file I/O.
     :skip-file-status-checks          false

     ; true/false to enable/disable the web functions "stylesheet" and "javascript" support for
     ; automatically checking for equivalent minified css/js files. If this is true, then when
     ; these functions are called and passed a URL to a non-minified css/js file, the function
     ; will check first to see if there is a '.min.js' or '.min.css' file with the same beginning
     ; part of the filename, and if so use that file instead.
     ; note that enabling this option does obviously incur a slight file I/O performance penalty
     ; whenever these functions are used
     :check-for-minified-web-resources false

     ; automatically convert keyword keys in maps to/from strings as necessary when being passed
     ; in model-maps, when passed to Jtwig functions and when returned as values from Jtwig
     ; functions. this does incur a slight performance penalty, but is best turned on to avoid
     ; having to do any manual conversions yourself and to keep your Clojure code as idiomatic
     ; as possible. Jtwig model-maps at the very least do require all the keys to be strings
     ; (not keywords) to ensure that model-map value resolution works as expected.
     :auto-convert-map-keywords        true

     ; the root path (relative to the classpath) where web resources such as js, css, images are
     ; located. typically in your project structure this path will be located under the
     ; "resources" directory.
     :web-resource-path-root           "public"}))