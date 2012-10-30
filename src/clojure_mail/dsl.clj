(ns clojure-mail.dsl)

;; A nicer interface for interacting with messages

(def settings {:username "" :password ""})

(defmacro with-auth
  ""
  [&body]
  `(binding [*store* (mail-store gmail (:username settings) (:password settings) ]
     (do ~@body))))

(defn read-from-folder
  "Returns all messages from a folder"
  [^String folder-name])

(defn all [folder])

(defn get
  "Get a single message by uid"
  [message-uid])
  