# clj-jtwig

[Twig](http://twig.sensiolabs.org/) templates in Clojure, provided by [Jtwig](http://jtwig.org). clj-jtwig is a simple
Clojure wrapper around Jtwig to make using it in Clojure applications simple. It also provides some extra functions on 
top of the standard library that Jtwig provides. As well, it adds a simple template caching layer that works 
out-of-the-box when rendering templates from files.

For more information on Twig templates, you can refer to the [Twig documentation](http://twig.sensiolabs.org/documentation)
and the [Jtwig documentation](http://jtwig.org/documentation). Please note that Jtwig is not yet a full implementation
of Twig, so some things you see on the Twig documentation might not yet be available in Jtwig, and as a result, not
available in clj-jtwig.

## Usage

**WARNING: This library is still in early development. May not be ready for production use!**

### Leiningen

[See Clojars for the up to date `project.clj` dependency line](https://clojars.org/clj-jtwig)

#### Java 6

Jtwig targets Java 7 so you can't use it on Java 6. If you are deploying applications into a Java 6 environment and
want to still use clj-jtwig, you can use the version maintained in
[this branch](https://github.com/gered/clj-jtwig/tree/java6). It makes use of an otherwise vanilla Jtwig library
that has been very slightly modified so that it compiles for Java 6. No other changes have been made and this fork is
only being maintained by me *purely* for use with clj-jtwig. *It is not supported by the Jtwig developers.*

[See Clojars for the up to date `project.clj` dependency line](https://clojars.org/clj-jtwig-java6)

### Rendering Templates

Getting up and running with clj-jtwig is easy:

```clojure
(use 'clj-jtwig.core)

(render "Hello {{name}}!" {:name "Gered"})   ; returns "Hello Gered!
```

You can also render templates from files:

```clojure
(render-file "/Users/gered/say-hello.twig" {:name "Gered"})

; 'say-hello.twig' in this case would be located at '[your-app]/resources/say-hello.twig'
(render-resource "say-hello.twig" {:name "Gered"})
```

As we can see above, there are 3 main template rendering functions, `render`, `render-file` and `render-resource`.
`render-resource` is probably what you will use most often. The file path you provide to this function is relative
to the classpath, and so is ideal for rendering templates bundled with your application.

All three "render" functions take a second parameter which is a map that contains the values to pass to the template 
which can then be referred to in the template by name. This map is also referred to as the "model map."

Nested variables can be referred to using "dot" syntax like in Java:

```clojure
(render "Hello {{person.name}}!" {:person {:name "Gered"}})   ; returns "Hello Gered!
```

You can only use "dot" syntax like in the above example when your variable names do not include any special characters
in them. For Clojure programmers, this also includes the use of hyphens in variable names. To get around this you can
use "subscript" syntax instead:

```clojure
(render "Hello {{person['first-name']}}!" {:person {:first-name "Gered"}})   ; returns "Hello Gered!
```

If your root-level variables in the model map contain special characters, you can still access them by using the
same "subscript" syntax off of the automatically added `model` variable that is passed to all templates:

```clojure
(render "Hello {{model['first-name']}}!" {:first-name "Gered"})   ; returns "Hello Gered!
```

### Web Apps

For web apps built on [Ring](https://github.com/ring-clojure/ring) and [Compojure](https://github.com/weavejester/compojure), 
you can do something like:

```clojure
(ns yourwebapp.views
  (:require [clj-jtwig.core :refer [render-resource]]
            [ring.util.response :refer [content-type response]]
            [compojure.response :refer [Renderable]]))

(deftype JtwigRenderable [template-filename params]
  Renderable
  (render [this request]
    (-> (render-resource template-filename params)
        (response)
        (content-type "text/html; charset=utf-8"))))

; params is an optional map that will get passed to clj-jtwig.core/render-resource. this is will
; need to contain any variables you want to use in 'template-filename'
(defn render [template-filename & [params]]
  (JtwigRenderable. template-filename params))
```

And then in your routes:

```clojure
(ns yourwebapp.routes
  (:use compojure.core)
  (:require [yourwebapp.views :refer [render]]))

(defn home-page []
  ; in this case, 'home.html' would be located at '[yourwebapp]/resources/views/home.html'
  (render "views/home.html" {:name "Gered"}))

(defroutes yourwebapp-routes
  (GET "/" [] (home-page)))
```

You will also probably want to add `clj-jtwig.web.middleware/wrap-servlet-context-path` to your 
[Ring middleware](https://github.com/ring-clojure/ring/wiki/Concepts#middleware) so that template functions such as 
`path`, `javascript` and `stylesheet` can automatically prepend the servlet context path to urls you provide to your 
web app's resources.

If you need to output the servlet context path in one of your templates (e.g. for use with Javascript code that 
performs AJAX requests), then you can either update the `render` function example provided above to `assoc` the value 
of `clj-jtwig.web.middleware/*servlet-context-path*` to the `params` map. Then you can refer to this value in your 
templates the same way as any other value you pass in. Or you can do something like this in one of your templates:

```html
<script type="text/javascript">
  var context = "{{ path('/') }}";
</script>
```

Which will make a global variable `context` available to your Javascript code which will have the value of the servlet 
context path.

### Functions

Adding custom functions is easy:

```clojure
(use 'clj-jtwig.functions)

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
(defaliasedtwigfn "sayHello" [name]
  ["sayHi"]
  (str "Hello " name "!"))
  
(render "{{ sayHello(name) }}" {:name "Gered"})         ; "Hello Gered!"
(render "{{ sayHi(name) }}" {:name "Gered"})            ; "Hello Gered!"
```

The `deftwigfn` and `defaliasedtwigfn` are macros that call `clj-jtwig.functions/add-function!` under the hood. If you
prefer, that function can be used directly. Those macros are simply a convenience so you can write template functions
in a 'defn'-style syntax.

#### Standard Library Functions

A number of functions are provided out of the box by Jtwig. A few more are provided by clj-jtwig to fill in some gaps. 
The following is a list of all the functions available with clj-jtwig.

| Function | Description
|----------|------------
| abs | `abs(number)`<br/>Returns the absolute value of a number.
| batch | `batch(items, batch_size)`<br/>`batch(items, batch_size, filler_item)`<br/>"Batches" items by returning a list of lists with the given number of items. If you provide a second parameter, it is used to fill missing items.
| blank_if_null | `blank_if_null(value)`<br/>If the value given is null, returns a blank string instead of "null".
| butlast | `butlast(collection)`<br/>`butlast(string)`<br/>`butlast(values, ...)`<br/>Returns all items except for the last one from a collection, series of values, or a string. If a string is passed, it will be treated as a collection of chars.
| capitalize | `capitalize(string)`<br/>Capitalizes a value. The first character will be uppercase, all others lowercase.
| center | `center(string, max_width)`<br/>`center(string, max_width, padding_string)`<br/>Pads a string with whitespace on the left and right as necessary to 'center' the given value. If the padding_string argument is provided, that string will be used to pad instead of whitespace.
| concat | `concat(values, ...)`<br/>Concatenates any number of values together as strings.
| contains | `contains(map, key)`<br/>`contains(collection, value)`<br/>`contains(string, substring)`<br/>If a map is specified, checks if it contains the given key. If a collection or string is specified, checks if the value/substring is present. Returns true if found.
| convert_encoding | `convert_encoding(string, output_charset, input_charset)`<br/>Converts a string from one encoding to another. The first argument is the expected output charset and the second one is the input charset.
| date_format | `date_format(date)`<br/>`date_format(date, format)`<br/>Formats a date to a given format. The format specifier is the same as supported by `SimpleDateFormat`. If the format argument is not specified, the format used will be `yyyy-MM-dd HH:mm:ss`. The date argument should be an instance of `java.util.Date`.
| date_modify | `date_modify(date, modifier)`<br/>Modifies a date with a given modifier string. The modifier string can be things like "+1 day" or "+30 minutes". Recognized modifiers are 'seconds', 'minutes', 'hours', 'days', 'months' or 'years'. The date argument should be an instance of `java.util.Date`. A new instance of `java.util.Date` is returned.
| default | `default(value, default_value)`<br/>Returns the passed default value if the value is undefined or empty, otherwise the value of the variable.
| dump | `dump(value)`<br/>Uses `clojure.pprint/pprint` to dump the entire value of a variable to a string and returns that string.
| dump_table | `dump_table(collection_of_maps)`<br/>Uses `clojure.pprint/print-table` to dump the entire value of a collection of maps to a string and returns that string.
| escape | `escape(string)`<br/>`escape(string, strategy)`<br/>Escapes a string for safe insertion into the final output. The optional strategy parameter specifies the escape strategy: 'html' (default), 'js' or 'xml'.
| first | `first(collection)`<br/>`first(string)`<br/>`first(values, ...)`<br/>Returns the first "element" of a collection, series of values, or a string (in which case the first character is returned).
| format | `format(format_string, values, ...)`<br/>Formats a given string by replacing the placeholders (placeholders follow the `String.format` notation). The values provided will be used in order for each placeholder in the string.
| index_of | `index_of(collection, value)`<br/>`index_of(string, substring)`<br/>Returns the first index of a value in a collection or the start of a substring in a string. Returns -1 if not found.
| javascript | `javascript(url)`<br/>Returns a `<script>` tag for a Javascript source file. The Javascript source file's modification timestamp will be appended to the URL if the url given is a relative one to help avoid browser caching issues.
| join | `join(sequence)`<br/>`join(sequence, separator)`<br/>Returns a string which is the concatenation of the items of a sequence. The separator argument specifies a string to use to place in between each joined item. If not specified, a blank string is used as the separator.
| json_encode | `json_encode(string)`<br/>Returns the JSON representation of the given value.
| keys | `keys(map)`<br/>Returns the keys of a map as a collection. It is useful when you want to iterate over the keys of a map.
| last | `last(collection)`<br/>`last(string)`<br/>`last(values, ...)`<br/>Returns the last "element" of a collection, series of values, or a string (in which case, the last character is returned).
| last_index_of | `last_index_of(collection, value)`<br/>`last_index_of(string, substring)`<br/>Returns the last index of a value in a collection or the start of a substring in a string. Returns -1 if not found.
| length | `length(collection)`<br/>`length(string)`<br/>`length(values, ...)`<br/>Returns the number of items in a collection, series of values, or the length of a string.
| lower | `lower(string)`<br/>Converts a string to lowercase.
| max | `max(collection)`<br/>`max(values, ...)`<br/>Returns the biggest value in a collection or a set of values.
| merge | `merge(first_collection, second_collection)`<br/>Merges a collection with another collection.
| min | `min(collection)`<br/>`min(values, ...)`<br/>Returns the lowest value in a collection or a set of values.
| nl2br | `nl2br(string)`<br/>Inserts HTML line breaks before all newlines in a string
| normalize_space | `normalize_space(string)`<br/>Trims leading and trailing whitespace and replaces all remaining whitespace with a single space.
| nth | `nth(collection, index)`<br/>`nth(collection, index, value_if_not_found)`<br/>Returns a value from a list corresponding with the index specified. If the value_if_not_found argument is not specified and the index provided is out of bounds, an exception will be thrown.
| number_format | `number_format(number)`<br/>`number_format(number, num_decimals)`<br/>`number_format(number, num_decimals, decimal_point_char)`<br/>`number_format(number, num_decimals, decimal_point_char, thousand_sep_char)`<br/>Formats numbers. You can control the number of decimal places, decimal point, and thousands separator using the arguments. The default values for the second, third and fourth values are '0', '.' and ',', respectively.
| path | `path(url)`<br/>Returns a path with the current servlet context prepended. Requires the use of the `clj-jtwig.web.middleware/wrap-servlet-context-path` Ring middleware to work properly.
| pad_left | `pad_left(string, max_width)`<br/>`pad_left(string, max_width, padding_string)`<br/>Pads a string with leading whitespace as necessary until the string and whitespace combined are max_width characters in length. If the padding_string argument is specified, it will be used to pad the string instead of whitespace.
| pad_right | `pad_right(string, max_width)`<br/>`pad_right(string, max_width, padding_string)`<br/>Pads a string with trailing whitespace as necessary until the string and whitespace combined are max_width characters in length. If the padding_string argument is specified, it will be used to pad the string instead of whitespace.
| random | `random(collection)`<br/>`random(string)`<br/>`random(values, ...)`<br/>`random(number)`<br/>Returns a random item from a collection or set of values. If an single number argument is provided, returns a random number from 0 to the number specified. If a string is specified, a random character from that string is returned.
| range | `range(low, high)`<br/>`range(low, high, step)`<br/>Returns a list containing an arithmetic progression of integers. The step argument specifies how to count from low to high, which by default is 1.
| repeat | `repeat(string, count)`<br/>Returns a string with the given string repeated count times.
| replace | `replace(string, placeholder_and_replacements_map)`<br/>Formats a given string by replacing the placeholders (placeholders are free-form).
| rest | `rest(collection)`<br/>`rest(string)`<br/>`rest(values, ...)`<br/>Returns all the items except for the first one from a collection, series of values, or a string. If a string is passed, it will be treated as a collection of chars.
| reverse | `reverse(collection)`<br/>`reverse(string)`<br/>`reverse(values, ...)`<br/>Reverses the items in a collection, series of values, or a string.
| round | `round(number)`<br/>`round(number, rounding_method)`<br/>Rounds a number to using the rounding method specified. Allowed rounding methods are 'common', 'ceil' and 'floor' with the default being 'common'.
| second | `second(collection)`<br/>`second(string)`<br/>`second(values, ...)`<br/>Returns the second item of a collection, series of values, or a string.
| slice | `slice(collection, start, length)`<br/>`slice(string, start, length)`<br/>Extracts a slice of a collection, or a string where the 2 last arguments specify the start and end indices respectively.
| sort | `sort(collection)`<br/>`sort(values, ...)`<br/>Sorts a collection or a set of values in ascending order. *Note that this differs in behaviour from Jtwig 2.2.0 in that the sorted collection returned from this is a new collection. The input collection is _not_ modified at all by this function.*
| sort_descending | `sort_descending(collection)`<br/>`sort_descending(values, ...)`<br/>Sorts a collection or a set of values in descending order.
| sort_by | `sort_by(collection_of_maps, sort_key)`<br/>Sorts a collection of maps in ascending order. The second argument specifies the key who's value in each map is to be used for sorting comparisons.
| sort_descending_by | `sort_descending_by(collection_of_maps, sort_key)`<br/>Sorts a collection of maps in descending order. The second argument specifies the key who's value in each map is to be used for sorting comparisons.
| split | `split(string, delimiter)`<br/>Splits a string by the given delimiter and returns a list of strings.
| striptags | `striptags(string)`<br/>Strips HTML/XML tags and replaces adjacent whitespace with one space.
| stylesheet | `stylesheet(url)`<br/>`stylesheet(url, media)`<br/>Returns a `<link>` tag for a CSS stylesheet with an optional 'media' attribute value. The CSS file's modification timestamp will be appended to the URL if the url given is a relative one to help avoid browser caching issues.
| title | `title(string)`<br/>Returns a titlecased version of the value. Words will start with uppercase letters, all remaining characters are lowercase.
| to_double | `to_double(string)`<br/>Converts the string to a double value.
| to_float | `to_float(string)`<br/>Converts the string to a float value.
| to_int | `to_int(string)`<br/>Converts the string to an integer value.
| to_keyword | `to_keyword(string)`<br/>Converts the string to a Clojure keyword.
| to_long | `to_long(string)`<br/>Converts the string to a long value.
| to_string | `to_string(value)`<br/>Converts the value to a string representation. This walks over all values in a collection, causing lazy sequences to be fully evaluated.
| trim | `trim(string)`<br/>Strips whitespace (or other characters) from the beginning and end of a string.
| upper | `upper(string)`<br/>Converts a value to uppercase.
| url_encode | `url_encode(string)`<br/>`url_encode(map)`<br/>Percent encodes a given string as a URL segment or a map of key/values as a query string (e.g. in a `key=value&key=value ...` format).
| wrap | `wrap(string, max_width)`<br/>`wrap(string, max_width, wrap_long_words)`<br/>`wrap(string, max_width, wrap_long_words, new_line_string)`<br/>Wraps the given text to the maximum width specified. If wrap_long_words is true, then long words/text such as URLs will also be cut and wrapped as necessary. new_line_string can be specified to use a different character/string as the new line separator (by default it will be the system new line character(s)).

## Options

clj-jtwig has a few global options that affect template parsing and rendering behaviour. You can adjust the options
by calling `clj-jtwig.core/set-options!` and passing in only the keys/values you want to change.

#### `:cache-compiled-templates`
**Default is ON.**

Whether or not to cache compiled templates. Caching is discussed in the next section.

#### `:skip-file-status-checks`
**Default is OFF.**

Whether or not to continue checking file modification times each time a template file is rendered *after* the first 
time it is compiled and cached. This option can be turned on to avoid extra file I/O (which is likely to be very 
minor anyway), but it means that after templates are first cached, any subsequent updates to the template files will 
not trigger the cached copy to be updated. If caching is disabled, this option is ignored.

#### `:check-for-minified-web-resources`
**Default is ON.**

If turned on, the `stylesheet` and `javascript` template functions will automatically check for an equivalent
similarly named .css/.js file with a .min.css/.min.js file extension and if found, render a link/script html tag
with that filename instead. This option is intended to be turned off during development and only turned on in
production builds to help simplify automatic switching between minified and development copies of css/js resources.

#### `:auto-convert-map-keywords`
**Default is ON.**

If turned on, the model map passed to any of the "render" functions will have any keyword keys converted to strings.
As well, custom template function parameters and return values that are maps containing keyword keys will also
have their keys converted to strings and vice-versa as necessary. This is an "all or nothing" type of option, so
you should probably turn this off if the maps you need to pass to templates will need to contain a mix of keyword
and string keys. This option is intended to make writing more idiomatic Clojure code against clj-jtwig easier to do
given that Jtwig is a Java library that has no concept of Clojure keywords. This option is discussed in more detail
below.

## Caching

Jtwig provides support for compiling templates so that subsequent renders can be performed faster. clj-jtwig builds on
this support by providing a very simple caching mechanism when rendering templates from files. Template files are
compiled the first time they are rendered and then the compiled result cached. From then on, each time that same
template file is rendered, the source file on disk is checked to see if it has been modified since it was last cached.
If it has been we re-load, compile and cache it before rendering it again. As long as the file has not been modified 
the previously compiled result is re-used.

Caching is turned on by default, but can be turned off if necessary via the `:cache-compiled-templates` option.

An important thing to be aware of when using templates that extend others, or include others is that only the
template who's filename is passed to one of the render functions is checked to see if it has been updated. If your
templates include other template files but those included files are never directly rendered themselves, then they will
not get recompiled and cached unless the parent template is updated as well. This can be a problem during development
of an application, so you may want to turn caching off during development.

## Jtwig is a Java library. What does that mean for Clojure?

Jtwig is written in Java. Model maps in Jtwig are represented by a `HashMap<String, Object>` [as can be seen here](https://github.com/lyncode/jtwig/blob/master/jtwig-core/src/main/java/com/lyncode/jtwig/JtwigModelMap.java).
Right away, this has an important implication for idiomatic Clojure code: Keywords are typically used as the keys for
maps.

There are two immediate options to get around this:

* Automatically convert the keys in all maps passed to Jtwig to use strings instead of keywords. And then do this again
  when map values are passed by Jtwig to custom template functions and then back again when map values are returned
  from custom template functions.
* Do nothing and force the application to use strings for all map keys.

There are pros and cons to each approach. Unfortunately there is no 100% perfect solution.

Out of the box, clj-jtwig is configured for the first approach, but that comes with certain assumptions about the maps
that you will be using with Jtwig. Specifically, if your application uses maps that contain a mix of key types (both
keywords *and* strings), then the automatic conversions will likely cause problems for you. You can toggle this
automatic conversion on/off via the `:auto-convert-map-keywords` option.

To demonstrate the problem:

```clojure
(use 'clj-jtwig.core)

(set-options! :auto-convert-map-keywords false)

(render "Hello {{name}}!" {:name "Gered"})                      ; = ClassCastException
(render "Hello {{name}}!" {"name" "Gered"})                     ; "Hello Gered!"
(render "Hello {{person.name}}!" {"person" {:name "Gered"}})    ; "Hello null!"

(set-options! :auto-convert-map-keywords true)

(render "Hello {{name}}!" {:name "Gered"})                      ; "Hello Gered!"
(render "Hello {{name}}!" {"name" "Gered"})                     ; "Hello Gered!"
(render "Hello {{person.name}}!" {"person" {:name "Gered"}})    ; "Hello Gered!"
```

This same type of problem also applies to variables passed to custom template functions. A trivial example:

```clojure
(use 'clj-jtwig.core)
(use 'clj-jtwig.functions)

(deftwigfn "get_map" [name]
  {:name name})

(set-options! :auto-convert-map-keywords false)

(render "Hello {{get_map(name).name}}!" {"name" "Gered"})       ; "Hello null!"

(set-options! :auto-convert-map-keywords true)

(render "Hello {{get_map(name).name}}!" {:name "Gered"})        ; "Hello Gered!"

```

## Debugging Tips

You can dump all of the values passed to the template by using the `dump` function, passing it the automatically
added `model` variable:

```jinja
{{model|dump}}
```

For any tabular values, you can also use `dump_table` to get a nice ASCII table display of each row of data.

Jtwig's error reporting (e.g. in the case of a parser error) is poor right now unfortunately. There's not much that
can be done except for making sure you frequently test your templates as you write them. Error reporting is expected
to be greatly improved in the next major release.

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
