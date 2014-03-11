(ns clojure-mail.refactor
  (:import [java.util Properties]
           [javax.mail.internet InternetAddress]
           [javax.mail.search FlagTerm]
           [javax.mail Session Store Folder Message Flags Flags$Flag]))

(def gmail
  {:protocol "imaps"
   :server   "imap.gmail.com"})

(def folder-names
  {:inbox "INBOX"
   :all   "[Gmail]/All Mail"
   :sent  "[Gmail]/Sent Mail"
   :spam  "[Gmail]/Spam"})

(defn as-properties
  [m]
  (let [p (Properties.)]
    (doseq [[k v] m]
      (.setProperty p (str k) (str v)))
        p))

(defn store
  "A store models a message store and its access protocol,
   for storing and retrieving messages"
  [protocol server user pass]
  (let [p (as-properties [["mail.store.protocol" protocol]])]
    (try
      (doto (.getStore (Session/getDefaultInstance p) protocol)
        (.connect server user pass))
      (catch javax.mail.AuthenticationFailedException e
        (format "Invalid credentials %s : %s" user pass)))))

(def gmail-store
  (let [{:keys [protocol server]} gmail]
    (partial store protocol server)))

(defn all-messages
  "Given a store and folder returns all messages"
  [^com.sun.mail.imap.IMAPStore store folder]
  (let [s (.getDefaultFolder store)
        inbox (.getFolder s folder)
        folder (doto inbox (.open Folder/READ_ONLY))]
    (.getMessages folder)))
