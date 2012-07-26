(ns clojure-mail.core
  (:import [javax.mail Session Store Folder Message]
           [javax.mail.internet InternetAddress]))

;; Focus will be more on the reading and parsing of emails. Sending messages should be more trivial.

;; IMAP client interface

(defn IMAP [] )

(defn SMTP [] )