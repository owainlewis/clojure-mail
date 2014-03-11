(ns clojure-mail.refactor
  (:import [java.util Properties]
           [javax.mail.search FlagTerm]
           [javax.mail.internet MimeMessage
                                MimeMultipart
                                InternetAddress]
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

(defn connected?
  "Returns true if a connection is established"
  [^com.sun.mail.imap.IMAPStore s]
  (.isConnected s))

(defn close
  [s]
  (.close s))

(defn get-default-folder
  ^{:doc "Returns a Folder object that represents the 'root' of the default
          namespace presented to the user by the Store."}
  [^com.sun.mail.imap.IMAPStore s]
  (.getDefaultFolder s))

(defn get-folder
  "Return the Folder object corresponding to the given name."
  [^com.sun.mail.imap.IMAPStore s name]
  (.getFolder s name))

(def gmail-store
  (let [{:keys [protocol server]} gmail]
    (partial store protocol server)))

(defn all-messages
  "Given a store and folder returns all messages
   reversed so the newest messages come first"
  [^com.sun.mail.imap.IMAPStore store folder]
  (let [s (.getDefaultFolder store)
        inbox (.getFolder s folder)
        folder (doto inbox (.open Folder/READ_ONLY))]
    (->> (.getMessages folder)
         reverse)))

;; Message parser
;; *********************************************************
;; Utilities for parsing email messages

(defn- mime-type
  "Determine the function to call to get the body text of a message"
  [msg type]
  (condp = type
    "multipart/alternative" :multipart
    "text/html" :html
    "text/plain" :plain
    (str "unexpected type, \"" type \")))

(defn to
  "Returns a sequence of receivers"
  [m]
  (map str
    (.getRecipients m javax.mail.Message$RecipientType/TO)))

(defn from [m]
  (.toString
    (.getFrom m)))

(defn subject [m]
  (.getSubject m))

(defn sender [m]
  (.toString
   (.getSender m)))

;; Dates
;; *********************************************************

(defn date-sent [m]
  (.toString
    (.getSentDate m)))

(defn date-recieved [m]
  (.toString
    (.getReceivedDate m)))

;; Flags
;; *********************************************************

(defn flags [m]
  (.getFlags m))

(defn content-type [m]
  (let [type (.getContentType m)]
    type))

(defn has-flag?
  [message flag]
  (let [f (flags message)]
    (boolean
      (.contains f flag))))

(defn read?
  "Checks if this message has been read"
  [message]
  (has-flag? message "SEEN"))

(defn answered?
  "Check if the message has an answered flag"
  [message]
  (has-flag? message "ANSWERED"))

(defn recent?
  [message]
  (has-flag? message "RECENT"))

(defn in-reply-to [m]
  (.getInReplyTo m))

(defn message-id [m]
  (.getMessageID m))

(defn encoding [m]
  (.getEncoding m))

(defn get-content [m]
  (.getContent m))

(defn message-headers
  "Returns all the headers from a message"
  [^com.sun.mail.imap.IMAPMessage msg]
  (let [headers (.getAllHeaders msg)
        results (enumeration-seq headers)]
    (into {}
      (map #(vector (.getName %) (.getValue %)) results))))

(defn- multipart? [m]
  "Returns true if a message is a multipart email"
  (.startsWith (content-type m) "multipart"))

(defn- read-multi [mime-multi-part]
  (let [count (.getCount mime-multi-part)]
    (for [part (map #(.getBodyPart mime-multi-part %) (range count))]
      (if (multipart? part)
        (.getContent part)
        part))))

(defn- message-parts
  [^javax.mail.internet.MimeMultipart msg]
  (if (multipart? msg)
    (read-multi (get-content msg))))

(defn message-body [^com.sun.mail.imap.IMAPMessage msg]
  "Read all the body content from a message"
  [msg]
  (into []
    (if (multipart? msg)
      (let [parts (message-parts msg)]
        (map #(hash-map (.getContentType %) (.getContent %)) parts))
      (list (hash-map (content-type msg) (.getContent msg))))))

;; Public API for working with messages
;; *********************************************************

(defn read-message [msg]
  "Returns a workable map of the message content.
   This is the ultimate goal in extracting a message
   as a clojure map"
  (try
    {:to (to msg)
     :from (sender msg)
     :subject (subject msg)
     :sender (sender msg)
     :date-sent (date-sent msg)
     :date-recieved (date-recieved msg)
     :multipart? (multipart? msg)
     :content-type (content-type msg)
     :body (message-body msg) }
  (catch Exception e {})))

;; *********************************************************
