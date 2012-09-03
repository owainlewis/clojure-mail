(ns clojure-mail.import)

;; One of the main reasons for building this library was to download
;; the entire contents of a gmail account into a database.
;; Here we want to provide a way to save emails from gmail to various
;; datastores such as mysql, postgres etc.

(defprotocol IDataStore
  ^{:doc "Abstraction for various datastores"}
  (connect [host user pass])
  (insert [record table]))

(deftype MySQL [host user pass]
  IDataStore
  (connect [host user pass])
  (insert [record table]
    ()))