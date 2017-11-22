(ns clojure-mail.core
  (:require [clojure-mail.parser :refer [html->text]]
            [clojure-mail.message :as message]
            [clojure-mail.message :refer [read-message]]
            [clojure-mail.folder :as folder])
  (:import [java.util Properties]
           [javax.mail.search FlagTerm]
           [java.io FileInputStream File]
           [javax.mail.internet MimeMessage]
           [javax.mail Session
                       Folder
                       Flags
                       Flags$Flag AuthenticationFailedException]
           (com.sun.mail.imap IMAPStore)))

(defonce ^:dynamic *store* nil)

(defmacro with-store
  "Takes a store which has been connected, and binds to to *store* within the
  scope of the form.

  **Usage:**

   user> (with-store (gmail-store \"username@gmail.com\" \"password\")
           (read-messages :inbox 5))
   ;=> "
  [s & body]
  `(binding [*store* ~s]
     ~@body))

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
  (let [props (Session/getDefaultInstance (Properties.))]
    (with-open [msg (FileInputStream. path-to-message)]
      (MimeMessage. props msg))))

(defn get-session
  [protocol]
  (let [p (as-properties
            {"mail.store.protocol"                         protocol
             (format "mail.%s.usesocketchannels" protocol) true})]
    (Session/getInstance p)))

(defn server->host-port
  "Given a protocol (`\"imap\"` or `\"imaps\"`) and a `server` that can be

   * a `String` with the hostname
   * a `vector` of the form `[hostname, port]`

   It returns a vector `[host, port]` using the `protocol` well-known
   ports if required."
  [protocol server]
  (let [default-port (cond
                       (and (number? protocol) (< protocol 65536) ) protocol
                       (= (keyword protocol) :imaps) 993
                       (= (keyword protocol) :imap) 143
                       :else 993) ]
    (if (sequential? server)
      (do
        (when (empty? server)
          (throw (IllegalArgumentException. "Empty sequential server")))
        (if (> (count server) 1)
          (vec (take 2 server))
          [(first server) default-port]))
      [server default-port])))

(defn store
  "A store models a message store and its access protocol,
   for storing and retrieving messages.
   The `server` parameter can be a String with the hostname or
   a vector like [^String hostname ^int port]. The first form will
   make the connection use the default ports for the defined
   `protocol`."
  ([server email pass]
   (store "imaps" server email pass))
  ([protocol server email pass]
   (let [p (as-properties
             {"mail.store.protocol"                         protocol
              (format "mail.%s.usesocketchannels" protocol) true})
         session (Session/getInstance p)]
     (store protocol session server email pass)))
  ([protocol session server email pass]
   (let [[target-host target-port] (server->host-port protocol server)]
     (doto (.getStore session protocol)
       (.connect ^String target-host ^int target-port ^String email ^String pass)))))

(defn xoauth2-store
  ([server email oauth-token]
   (xoauth2-store "imaps" server email oauth-token))
  ([protocol server email oauth-token]
   (let [p (as-properties
             {(format "mail.%s.ssl.enable" protocol)         true
              (format "mail.%s.sasl.enable" protocol)        true
              (format "mail.%s.auth.login.disable" protocol) true
              (format "mail.%s.auth.plain.disable" protocol) true
              (format "mail.%s.auth.mechanisms" protocol)    "XOAUTH2"
              (format "mail.%s.usesocketchannels" protocol)  true})
         session (Session/getInstance p)]
     (doto (.getStore session protocol)
       (.connect server, email, oauth-token)))))

(defn connected?
  "Returns true if a connection is established"
  [^IMAPStore s]
  (.isConnected s))

(defn close-store
  "Close an open IMAP store connection"
  [s]
  (.close s))

(defn get-default-folder
  ^{:doc "Returns a Folder object that represents the 'root' of the default
          namespace presented to the user by the Store."}
  [^IMAPStore s]
  (.getDefaultFolder s))

(defn get-folder
  "Return the Folder object corresponding to the given name."
  [^IMAPStore s name]
  (.getFolder s name))

(defn get-folder-uid-validity
  "Return the Folder UIDValidity"
  [folder]
  (.getUIDValidity folder))

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

(def folder-permissions
  {:readonly  Folder/READ_ONLY
   :readwrite Folder/READ_WRITE})

(defn open-folder
  "Open and return a folder.
  A folder-name is string (usually a label name)"
  ([folder-name perm-level] (open-folder *store* folder-name perm-level))
  ([store folder-name perm-level]
   (let [found-folder (get-folder store folder-name)]
     (.open found-folder (get folder-permissions perm-level))
     found-folder)))

(defn message-count
  "Returns the number of messages in a folder"
  ([folder-name] (message-count *store* folder-name))
  ([store folder-name]
   (let [folder (open-folder store folder-name :readonly)]
     (.getMessageCount folder))))

(defn user-flags [message]
  (let [flags (message/flags message)]
    (.getUserFlags flags)))

(defn unread-messages
  "Find unread messages"
  ([folder-name] (unread-messages *store* folder-name))
  ([^IMAPStore store folder-name]
   (let [folder (open-folder store folder-name :readwrite)]
     (.search folder
              (FlagTerm. (Flags. Flags$Flag/SEEN) false)))))

(defn mark-all-read
  "Mark all messages in folder as read"
  ([folder-name] (mark-all-read *store* folder-name))
  ([^IMAPStore store folder-name]
   (let [folder (open-folder store folder-name :readwrite)
         messages (.search folder (FlagTerm. (Flags. Flags$Flag/SEEN) false))]
     (dorun (map #(.setFlags % (Flags. Flags$Flag/SEEN) true) messages)))))

(defn save-message-to-file
  [message]
  (let [filename
        (apply str
               (drop 1 (message/id message)))]
    (.writeTo message
              (java.io.FileOutputStream.
                filename))))

(defn dump
  "Handy function that dumps out a batch of emails to disk"
  [msgs]
  (let [message-futures
        (doall
          (map #(future (save-message-to-file %)) msgs))]
    (map deref message-futures)))

(defn all-messages
  "Given a store and folder returns all messages
   reversed so the newest messages come first. 
  If since-uid is provided, return all messages with newer or equal uid"
  ([folder-name] (all-messages *store* folder-name))
  ([^IMAPStore store folder-name & {:keys [since-uid]}]
   (let [folder (open-folder store folder-name :readonly)]
     (->> (if-not since-uid
            (.getMessages folder)
            (.getMessagesByUID folder since-uid javax.mail.UIDFolder/LASTUID))
          reverse))))

(defn inbox
  "Get n messages from your inbox"
  ([] (all-messages *store* "inbox"))
  ([store] (all-messages store "inbox")))

(defn search-inbox
  "Search your inbox for a specific term
   Returns a vector of IMAPMessage objects"
  [store term]
  (let [inbox (open-folder store "inbox" :readonly)]
    (into []
          (folder/search inbox term))))
