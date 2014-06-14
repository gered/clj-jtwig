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
     :check-for-minified-web-resources true

     ; whether or not to automatically stringify the keys of model-maps. Jtwig requires that all
     ; the keys will be strings for model value resolution to work correctly. if you are already
     ; setting your keys as maps, then you can turn this option off to save a bit on performance
     :stringify-model-map-keys         true

     }))