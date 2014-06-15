(defproject clj-jtwig-java6 "0.5"
  :description "Clojure wrapper for JTwig (Java 6 dependencies)"
  :url "https://github.com/gered/clj-jtwig/tree/java6"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :repositories [["sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                              :snapshots false}]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.lyncode/jtwig-core-java6 "2.1.7"]
                 [org.apache.commons/commons-lang3 "3.1"]])
