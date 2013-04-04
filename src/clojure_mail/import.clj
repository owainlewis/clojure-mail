(ns clojure-mail.import
  (:use 'clojure-mail.core))

;; One of the main reasons for building this library was to download
;; the entire contents of a gmail account into a database.
;; Here we want to provide a way to save emails from gmail to various
;; datastores such as mysql, postgres etc.
