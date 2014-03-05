# clj-jtwig

[Twig](http://twig.sensiolabs.org/) templates in Clojure, provided by [Jtwig](http://jtwig.org). clj-jtwig is a simple
Clojure wrapper around Jtwig to make using it in Clojure applications simple.

For more information on Twig templates, you can refer to the [Twig documentation](http://twig.sensiolabs.org/documentation)
and the [Jtwig documentation](http://jtwig.org/documentation). Please note that Jtwig is not yet a full implementation
of Twig, so some things you see on the Twig documentation might not yet be available in Jtwig, and as a result, not
available in clj-jtwig.

## Usage

### Leiningen

```clojure
[clj-jtwig "0.2.1"]
```

#### Java 6

Jtwig targets Java 7 so you can't use it on Java 6. If you are deploying applications into a Java 6 environment and
want to still use clj-jtwig, you can use the java6 version (maintained in
[this branch](https://github.com/gered/clj-jtwig/tree/java6)). This makes use of an otherwise vanilla Jtwig library
that has been very slightly modified so that it compiles for Java 6. No other changes have been made and this fork is
only being maintained by me *purely* for use with clj-jtwig. *It is not supported by the Jtwig developers.*

```clojure
[clj-jtwig-java6 "0.2.1"]
```

### Rendering Templates

Getting up and running with clj-jtwig is easy:

```clojure
(ns yourapp.core
  (:require [clj-jtwig.core :refer [render]]))

(defn say-hello [name]
  (render "Hello {{name}}!" {:name name})
  
(say-hello "Gered")     ; returns "Hello Gered!"
```

You can also render from files by providing a full path and filename to `clj-jtwig.core/render-file`. Most of the time
you'll want to render from a resource file that is bundled along with your app, in which case you can provide a path
and filename relative to the classpath to `clj-jtwig.core/render-resource`.

```clojure
(render-file "/Users/gered/say-hello.twig" {:name "Gered"})

; 'say-hello.twig' in this case would be located at '[your-app]/resources/say-hello.twig'
(render-resource "say-hello.twig" {:name "Gered"})
```

From these examples we can see that the second argument to `render`, `render-file` and `render-resource` is a map of
variables that can be referred to in the template (basically it is the template 'model'). You can nest data and refer
to it using 'dot' syntax like in Java.

```jinja
City of residence: {{address.city}}
```

### Web Apps

For web apps built on Compojure, you can do something like:

```clojure
(ns yourwebapp.views
  (:require [clj-jtwig.core :refer [render-resource]]
            [clj-jtwig.functions :refer [deftwigfn]]
            [ring.util.response :refer [content-type response]]
            [compojure.response :refer [Renderable]]))

(deftype JtwigRenderable [template-filename params]
  Renderable
  (render [this request]
    (content-type
      (response
        (render-resource
          template-filename
          ; add a var 'context' which contains the current servlet context path which you will want to prefix
          ; on to your css/js/img and other links that you render in your html
          (assoc params :context (or (:context request) ""))))
      "text/html; charset=utf-8")))

; params is an optional map that will get passed to clj-jtwig.core/render-resource. this is will need to contain
; any variables you want to use in 'template-filename'
(defn render [template-filename & [params]]
  (JtwigRenderable. template params))
```

And then in your route:

```clojure
(ns yourwebapp.routes
  (:use compojure.core)
  (:require [yourwebapp.views :refer [render]]))

(defn home-page []
  ; in this case, 'home.html' would be located at '[yourwebapp]/resources/views/home.html'
  (layout/render "views/home.html" {:name "Gered"}))

(defroutes yourwebapp-routes
  (GET "/" [] (home-page)))
```

### Functions

Adding custom functions is also easy:

```clojure
(ns yourapp.core
  (:require [clj-jtwig.core :refer [render]]
            [clj-jtwig.functions :refer [deftwigfn]]))

(deftwigfn "sayHello" [name]
  (str "Hello " name "!"))
```

Then your functions can be used in your templates:

```clojure
(render "{{ sayHello(name) }}" {:name "Gered"})         ; "Hello Gered!"

; or you can call it using the 'pipe' (filter) syntax
(render "{{ name|sayHello }}" {:name "Gered"})          ; "Hello Gered!"

; you can also nest functions and/or chain filters together
(render "{{ name|upper|sayHello }}" {:name "Gered"})    ; "Hello GERED!"
(render "{{ sayHello(upper(name)) }}" {:name "Gered"})  ; "Hello GERED!"
```

For convenience, you can also define one or more aliases for functions:

```clojure
(ns yourapp.core
  (:require [clj-jtwig.core :refer [render]]
            [clj-jtwig.functions :refer [defaliasedtwigfn]]))

(defaliasedtwigfn "sayHello" [name]
  ["sayHi"]
  (str "Hello " name "!"))
  

; elsewhere in your app's code ...

(render "{{ sayHello(name) }}" {:name "Gered"})         ; "Hello Gered!"
(render "{{ sayHi(name) }}" {:name "Gered"})            ; "Hello Gered!"
```

The `deftwigfn` and `defaliasedtwigfn` are macros that call `clj-jtwig.functions/add-function!` under the hood. If you
prefer, that function can be used directly. Those macros are simply a convenience so you can write template functions
in a 'defn'-style syntax.

#### Standard Library Functions

A number of functions are provided out of the box by Jtwig. A few more are provided to fill in some gaps by clj-jtwig.

| Function | Description
|----------|------------
| [abs](http://jtwig.org/documentation/functions#item_7) | Returns the absolute value of a number.
| [batch](http://jtwig.org/documentation/functions#item_8) | "Batches" items by returning a list of lists with the given number of items. If you provide a second parameter, it is used to fill missing items.
| blankIfNull | If the value given is null, returns a blank string instead of "null".
| [capitalize](http://jtwig.org/documentation/functions#item_9) | Capitalizes a value. The first character will be uppercase, all others lowercase.
| concat | Concatenates any number of values together as strings.
| [convert_encoding](http://jtwig.org/documentation/functions#item_10) | Converts a string from one encoding to another. The first argument is the expected output charset and the second one is the input charset.
| [date_format](http://jtwig.org/documentation/functions#item_11) | Formats a date to a given format. The format specifier is the same as supported by `SimpleDateFormat`.
| [date_modify](http://jtwig.org/documentation/functions#item_13) | Modifies a date with a given modifier string. Can be 'seconds', 'minutes', 'hours', 'days', 'months' or 'years'.
| [default](http://jtwig.org/documentation/functions#item_14) | Returns the passed default value if the value is undefined or empty, otherwise the value of the variable.
| dump | Uses `clojure.pprint/pprint` to dump the entire value of a variable to a string and returns that string.
| [escape](http://jtwig.org/documentation/functions#item_15) | Escapes a string for safe insertion into the final output. The second parameter specifies the escape strategy: 'html' (default), 'js' or 'xml'.
| [first](http://jtwig.org/documentation/functions#item_16) | Returns the first "element" of a sequence, or a string.
| [format](http://jtwig.org/documentation/functions#item_17) | Formats a given string by replacing the placeholders (placeholders follow the `String.format` notation).
| [join](http://jtwig.org/documentation/functions#item_18) | Returns a string which is the concatenation of the items of a sequence. The second parameter specifies a string to place between joined elements (blank string by default).
| [json_encode](http://jtwig.org/documentation/functions#item_19) | Returns the JSON representation of the given value.
| [keys](http://jtwig.org/documentation/functions#item_20) | Returns the keys of a map. It is useful when you want to iterate over the keys of a map.
| [last](http://jtwig.org/documentation/functions#item_21) | Returns the last "element" of a sequence, or a string.
| [length](http://jtwig.org/documentation/functions#item_22) | Returns the number of items of a sequence or mapping, or the length of a string.
| [lower](http://jtwig.org/documentation/functions#item_23) | Converts a value to lowercase.
| max | Returns the biggest value of a sequence or a set of values.
| [merge](http://jtwig.org/documentation/functions#item_26) | Merges a list with another list.
| min | Returns the lowest value of a sequence or a set of values.
| [nl2br](http://jtwig.org/documentation/functions#item_24) | Inserts HTML line breaks before all newlines in a string
| nth | Returns a value from a list corresponding with the index specified.
| [number_format](http://jtwig.org/documentation/functions#item_25) | Formats numbers. You can control the number of decimal places, decimal point, and thousands separator using the arguments.
| random | Returns a random item from a list or set of values. If an single number argument is provided, returns a random number from 0 to the number specified.
| range | Returns a list containing an arithmetic progression of integers. An optional third argument specifies the 'step' which by default is 1.
| [replace](http://jtwig.org/documentation/functions#item_27) | Formats a given string by replacing the placeholders (placeholders are free-form).
| rest | Returns all the items from a list except for the first one.
| [reverse](http://jtwig.org/documentation/functions#item_28) | Reverses a sequence, or a string.
| [round](http://jtwig.org/documentation/functions#item_29) | Rounds a number to a given precision.
| second | Returns the second item of a sequence.
| slice | Extracts a slice of a sequence, or a string where the 2 last arguments specify the start and end indices respectively.
| sort | Sorts a list in ascending order.
| sortDescending | Sorts a list in descending order.
| sortBy | Sorts a list of maps in ascending order. The second argument specifies the key who's value is to be used for sorting comparisons.
| sortDescendingBy |Sorts a list of maps in descending order. The second argument specifies the key who's value is to be used for sorting comparisons.
| split | Splits a string by the given delimiter and returns a list of strings.
| striptags | Strips HTML/XML tags and replaces adjacent whitespace with one space.
| title | Returns a titlecased version of the value. Words will start with uppercase letters, all remaining characters are lowercase.
| trim | Strips whitespace (or other characters) from the beginning and end of a string.
| upper | Converts a value to uppercase.
| url_encode | Percent encodes a given string as URL segment or an array as query string.

## License

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
