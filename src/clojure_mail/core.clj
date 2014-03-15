
(ns clojure-mail.core
  (:require [clojure-mail.parser :refer [html->text]])
  (:import  [java.util Properties]
            [javax.mail.search FlagTerm]
            [java.io FileInputStream File]
            [javax.mail.internet MimeMessage
                                 MimeMultipart
                                 InternetAddress]
            [javax.mail Session
                        Store
                        Folder
                        Message
                        Flags
                        Flags$Flag]))

;; Authentication
;; ***********************************************

(def settings (ref {:email nil :pass nil}))

(defn auth! [email pass]
  (dosync
    (ref-set settings
      {:email email :pass pass})))

(def gmail
  {:protocol "imaps"
   :server "imap.gmail.com"})

(defn assert-credentials!
  "Make sure that a user has set Gmail credentials"
  []
  (when (empty? @settings)
    (let [msg "\nYou must set your Gmail credentials with (auth! email password)\n"]
      (throw (Exception. msg)))))

(defmacro with-auth [user pass & body])

;; ***********************************************

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

(defn file->message
  "read a downloaded mail message in the same format
   as you would find on the mail server. This can
   be used to read saved messages from text files
   and for parsing fixtures in tests etc"
  [path-to-message]
  (let [props (Session/getDefaultInstance (Properties.))
        msg (FileInputStream. (File. path-to-message))]
    (MimeMessage. props msg)))

;; Mail store
;; *******************************************************

(defn store
  "A store models a message store and its access protocol,
   for storing and retrieving messages"
  [protocol server email pass]
  (let [p (as-properties [["mail.store.protocol" protocol]])]
    (try
      (doto (.getStore (Session/getDefaultInstance p) protocol)
        (.connect server email pass))
      (catch javax.mail.AuthenticationFailedException e
        (format "Invalid credentials %s : %s" email pass)))))

(defn gen-store
  "Generates an email store which allows us access to our inbox"
  []
  (assert-credentials!)
  (let [[email pass] ((juxt :email :pass) @settings)]
    (apply (partial store (:protocol gmail) (:server gmail))
      (vals @settings))))

(defn connected?
  "Returns true if a connection is established"
  [^com.sun.mail.imap.IMAPStore s]
  (.isConnected s))

(defn close-store
  "Close an open IMAP store connection"
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

(defn mime-type
  "Determine the function to call to get the body text of a message"
  [type]
  (let [infered-type
         (clojure.string/lower-case
           (first (clojure.string/split type #"[;]")))]
  (condp = infered-type
    "multipart/alternative" :multipart
    "text/html" :html
    "text/plain" :plain
    (str "unexpected type, \"" type \"))))

(defn to
  "Returns a sequence of receivers"
  [m]
  (map str
    (.getRecipients m javax.mail.Message$RecipientType/TO)))

(defn from
  [m]
  (.getFrom m))

(defn subject
  "Fetch the subject of a mail message"
  [m]
  (.getSubject m))

(defn sender
  "Extract the message sender"
  [m]
  (.toString
   (.getSender m)))

;; Dates
;; *********************************************************

(defn date-sent
  "Return the date a mail message was sent"
  [m]
  (.toString
    (.getSentDate m)))

(defn date-recieved
  "Return the date a message was recieved"
  [m]
  (.toString
    (.getReceivedDate m)))

;; Flags
;; *********************************************************

(defn flags
  [m]
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

(defn msg->map
  "Convert a mail message body into a Clojure map
   with content type and message contents"
  [msg]
  {:content-type (.getContentType msg)
   :body (.getContent msg)})

(defn message-body
  [^com.sun.mail.imap.IMAPMessage msg]
  "Read all the body content from a message
   If the message is multipart then a vector is
   returned containing each message
   [{:content-type \"TEXT\\PLAIN\" :body \"Foo\"}
    {:content-type \"TEXT\\HTML\"  :body \"Bar\"}]"
  [msg]
  (if (multipart? msg)
    (map msg->map (message-parts msg))
    (msg->map msg)))

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
  (catch Exception e nil)))

;; *********************************************************

(def sub-folder?
  "Check if a folder is a sub folder"
  (fn [folder]
    (if (= 0 (bit-and
               (.getType folder) Folder/HOLDS_FOLDERS))
      false
      true)))

(defn folders
  "Returns a seq of all IMAP folders inlcuding sub folders"
  ([store] (folders store (.getDefaultFolder store)))
  ([store f]
  (map
    #(cons (.getName %)
      (if (sub-folder? %)
        (folders store %)))
          (.list f))))

(defn message-count
  "Returns the number of messages in a folder"
  [store folder]
  (let [fd (doto (.getFolder store folder)
                 (.open Folder/READ_ONLY))]
    (.getMessageCount fd)))

(defn user-flags [message]
  (let [flags (flags message)]
    (.getUserFlags flags)))

(defn unread-messages
  "Find unread messages"
  [folder-name]
  (with-open [connection (gen-store)]
    (let [folder (doto (.getFolder connection folder-name)
                   (.open Folder/READ_ONLY))]
      (doall (map read-message
               (.search folder
                 (FlagTerm. (Flags. Flags$Flag/SEEN) false)))))))

(defn mark-all-read
  [folder-name]
  (with-open [connection (gen-store)]
      (let [folder (doto (.getFolder connection folder-name)
                     (.open Folder/READ_WRITE))
            messages (.search folder
                       (FlagTerm. (Flags. Flags$Flag/SEEN) false))]
         (doall (map #(.setFlags % (Flags. Flags$Flag/SEEN) true) messages))
        nil)))

(defn dump
  "Handy function that dumps out a batch of emails to disk"
  [dir msgs]
  (doseq [msg msgs]
    (.writeTo msg (java.io.FileOutputStream.
      (format "%s%s" dir (str (message-id msg)))))))

;; Public API
;; *********************************************************

(defn read-messages
  "Get all messages from a users inbox"
  ([folder-name email password limit]
  (let [store (gmail-store email password)
        folder (get folder-names folder-name)
        messages (take limit (all-messages store folder))]
        (doall
          (map #(read-message %)
            messages)))))

(defn inbox
  "Get n messages from your inbox"
  ([limit]
    (let [[email password] (vals @settings)]
      (inbox email password limit)))
  ([email password limit]
    (read-messages :inbox email password limit)))
