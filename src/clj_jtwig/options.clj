(ns clj-jtwig.options)

; global options
(defonce options
  (atom
    {; true/false to enable/disable compiled template caching when using templates from
     ; files only. this does not affect templates being rendered directly from strings
      :cache-compiled-templates true

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
      :skip-file-status-checks  false}))