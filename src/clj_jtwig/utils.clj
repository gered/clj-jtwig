(ns clj-jtwig.utils
  "various helper / utility functions"
  (:import (java.net URL)
           (java.io File)
           (java.util.jar JarFile)))

(defn inside-jar? [^File file]
  (-> file
      (.getPath)
      ; the path of a file inside a jar looks something like "jar:file:/path/to/file.jar!/path/inside/jar/to/file"
      (.contains "jar!")))

(defn get-jar-resource-filename [^String resource-filename]
  (let [pos (.indexOf resource-filename "jar!")]
    (if-not (= -1 pos)
      (subs resource-filename (+ pos 5))
      resource-filename)))

(defn get-jar-filename [^String resource-filename]
  (let [start (.indexOf resource-filename "file:")
        end   (.indexOf resource-filename "jar!")]
    (if (and (not= -1 start)
             (not= -1 end))
      (subs resource-filename 5 (+ end 3))
      resource-filename)))

(defn exists? [^File file]
  (if (inside-jar? file)
    (let [filename  (.getPath file)
          jar-file  (new JarFile (get-jar-filename filename))
          jar-entry (.getJarEntry jar-file (get-jar-resource-filename filename))]
      (not (nil? jar-entry)))
    (.exists file)))

(defn get-file-last-modified [^File file]
  (if (inside-jar? file)
    0
    (.lastModified file)))

(defn get-resource-path
  (^URL [^String filename]
   (-> (Thread/currentThread)
       (.getContextClassLoader)
       (.getResource filename))))

(defn get-resource-modification-date [^String filename]
  (when-let [resource-filename (get-resource-path filename)]
    (->> resource-filename
         (.getPath)
         (new File)
         (get-file-last-modified))))