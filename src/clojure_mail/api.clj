(ns clojure-mail.api
  (:require [clojure-mail.core :as core]
            [clojure-mail.message :as msg]
            [clojure-mail.store :as store]
            [clojure-mail.folder :as folder]))

;; A nicer interface to abstract some complexity in the API

(defn get-mail
  "Return all mail"
  [username password]
  (let [store (store/make-store core/gmail username password)]
    store))

