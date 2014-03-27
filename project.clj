(defproject clj-jtwig "0.3.1"
  :description "Clojure wrapper for JTwig"
  :url "https://github.com/gered/clj-jtwig"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :repositories [["sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                              :snapshots false}]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.lyncode/jtwig-core "2.1.4"]
                 [org.apache.commons/commons-lang3 "3.1"]])
