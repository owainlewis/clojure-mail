(ns clojure-mail.import)

;; A tool that imports all your messages from an SMTP server and saves them
;; into a database

(defn dump-to-file
  [message]
  (for [[k v] message]
    (print k)
    (print v)))

(defn import [store]
  ())