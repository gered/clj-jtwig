(defproject clj-jtwig "0.5.1"
  :description "Clojure wrapper for Jtwig"
  :url "https://github.com/gered/clj-jtwig"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :repositories [["sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                              :snapshots false}]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.lyncode/jtwig-core "3.0.0-SNAPSHOT"]
                 [org.apache.commons/commons-lang3 "3.1"]]
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"])
