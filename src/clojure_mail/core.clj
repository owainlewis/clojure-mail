(ns clojure-mail.core
  (:require [clojure-mail.message :as message]
            [clojure-mail.folder :as folder])
  (:import [java.util Properties]
           [javax.mail.search FlagTerm]
           [java.io FileInputStream FileOutputStream]
           [javax.mail.internet MimeMessage]
           [javax.mail Message
            Session
            Store
            Folder
            Flags
            Flags$Flag
            UIDFolder]))

(defonce ^:dynamic *store* nil)

(defmacro with-store
  "Takes a store which has been connected, and binds it to `*store*` within the
  scope of the form.

  **Usage:**

   user> (with-store (gmail/store \"username@gmail.com\" \"app-password\")
           (all-messages \"inbox\"))"
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
  (let [session (Session/getDefaultInstance (Properties.))]
    (with-open [msg (FileInputStream. (str path-to-message))]
      (MimeMessage. session msg))))

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
                       (and (number? protocol) (pos? protocol) (< protocol 65536)) protocol
                       (= (keyword protocol) :imaps) 993
                       (= (keyword protocol) :imap) 143
                       :else 993)]
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
     (doto (.getStore ^Session session ^String protocol)
       (.connect ^String target-host (int target-port) ^String email ^String pass)))))

(defn xoauth2-store
  "Connect to an IMAP store using OAuth 2 (XOAUTH2 SASL mechanism).
   `server` may be a hostname string or `[hostname port]`."
  ([server email oauth-token]
   (xoauth2-store "imaps" server email oauth-token))
  ([protocol server email oauth-token]
   (let [[target-host target-port] (server->host-port protocol server)
         p (as-properties
            {"mail.store.protocol"                          protocol
             (format "mail.%s.ssl.enable" protocol)         true
             (format "mail.%s.sasl.enable" protocol)        true
             (format "mail.%s.auth.login.disable" protocol) true
             (format "mail.%s.auth.plain.disable" protocol) true
             (format "mail.%s.auth.mechanisms" protocol)    "XOAUTH2"
             (format "mail.%s.usesocketchannels" protocol)  true})
         session (Session/getInstance p)]
     (doto (.getStore session ^String protocol)
       (.connect ^String target-host (int target-port) ^String email ^String oauth-token)))))

(defn connected?
  "Returns true if a connection is established"
  [^Store s]
  (.isConnected s))

(defn close-store
  "Close an open IMAP store connection"
  [^Store s]
  (.close s))

(defn get-default-folder
  "Returns a Folder object that represents the 'root' of the default
   namespace presented to the user by the Store."
  [^Store s]
  (.getDefaultFolder s))

(defn get-folder
  "Return the Folder object corresponding to the given name."
  [^Store s ^String name]
  (.getFolder s name))

(defn get-folder-uid-validity
  "Return the Folder UIDValidity"
  [^UIDFolder folder]
  (.getUIDValidity folder))

(defn sub-folder?
  "Check if a folder can hold other folders"
  [^Folder folder]
  (not (zero? (bit-and (.getType folder) Folder/HOLDS_FOLDERS))))

(defn folders
  "Returns a seq of all IMAP folders including sub folders"
  ([^Store store] (folders store (.getDefaultFolder store)))
  ([store ^Folder f]
   (map
    (fn [^Folder sub]
      (cons (.getName sub)
            (when (sub-folder? sub)
              (folders store sub))))
    (.list f))))

(def folder-permissions
  {:readonly  Folder/READ_ONLY
   :readwrite Folder/READ_WRITE})

(defn open-folder
  "Open and return a folder.
  A folder-name is string (usually a label name)"
  (^Folder [folder-name perm-level] (open-folder *store* folder-name perm-level))
  (^Folder [store folder-name perm-level]
   (let [mode (or (get folder-permissions perm-level)
                  (throw (IllegalArgumentException.
                          (str "Unknown folder permission level: " (pr-str perm-level)
                               ". Expected one of " (keys folder-permissions)))))
         ^Folder found-folder (get-folder store folder-name)]
     (.open found-folder (int mode))
     found-folder)))

(defn close-folder
  "Close an open folder if it is currently open. Expunges deleted messages when
   `expunge?` is true (default false)."
  ([^Folder folder]
   (close-folder folder false))
  ([^Folder folder expunge?]
   (when (.isOpen folder)
     (.close folder (boolean expunge?)))))

(defn ^:no-doc open-folder*
  (^Folder [folder-name]
   (open-folder *store* folder-name :readonly))
  (^Folder [store-or-folder folder-or-perm]
   (if (instance? Store store-or-folder)
     (open-folder store-or-folder folder-or-perm :readonly)
     (open-folder *store* store-or-folder folder-or-perm)))
  (^Folder [store folder-name perm-level]
   (open-folder store folder-name perm-level)))

(defmacro with-folder
  "Open a folder, bind it to `sym`, evaluate `body`, and close the folder on exit
   (without expunging).

   Bindings may be:
     [sym store folder-name perm-level]
     [sym store folder-name]            ; defaults to :readonly
     [sym folder-name perm-level]       ; uses *store*
     [sym folder-name]                  ; uses *store* and :readonly

   Usage:
     (with-folder [f store \"INBOX\" :readonly]
       (clojure-mail.folder/message-count f))"
  [[sym & open-args] & body]
  (when-not (<= 1 (count open-args) 3)
    (throw (IllegalArgumentException.
            "with-folder expects [sym folder-name], [sym folder-name perm-level], [sym store folder-name], or [sym store folder-name perm-level]")))
  `(let [~sym (open-folder* ~@open-args)]
     (try
       ~@body
       (finally
         (close-folder ~sym false)))))

(defn message-count
  "Returns the number of messages in a folder"
  ([folder-name] (message-count *store* folder-name))
  ([store folder-name]
   (with-folder [^Folder folder store folder-name :readonly]
     (.getMessageCount folder))))

(defn user-flags [message]
  (let [^Flags flags (message/flags message)]
    (.getUserFlags flags)))

(defn unread-messages
  "Find unread messages"
  ([folder-name] (unread-messages *store* folder-name))
  ([store folder-name]
   (let [^Folder folder (open-folder store folder-name :readwrite)]
     (.search folder
              (FlagTerm. (Flags. Flags$Flag/SEEN) false)))))

(defn mark-all-read
  "Mark all messages in folder as read"
  ([folder-name] (mark-all-read *store* folder-name))
  ([store folder-name]
   (with-folder [^Folder folder store folder-name :readwrite]
     (let [messages (.search folder (FlagTerm. (Flags. Flags$Flag/SEEN) false))]
       (doseq [^Message m messages]
         (.setFlags m (Flags. Flags$Flag/SEEN) true))))))

(defn save-message-to-file
  "Write a message to a file. With one argument, writes to the current directory
   named after its Message-ID. With two arguments, writes to `path`.
   Returns the file path."
  ([^Message message]
   (let [filename (apply str (drop 1 (message/id message)))]
     (save-message-to-file message filename)))
  ([^Message message path]
   (let [target (str path)]
     (with-open [out (FileOutputStream. target)]
       (.writeTo message out))
     target)))

(defn dump
  "Handy function that dumps out a batch of emails to disk"
  [msgs]
  (let [message-futures
        (doall
         (map #(future (save-message-to-file %)) msgs))]
    (doall (map deref message-futures))))

(defn all-messages
  "Given a store and folder returns all messages
   reversed so the newest messages come first. 
  If since-uid is provided, return all messages with newer or equal uid"
  ([folder-name] (all-messages *store* folder-name))
  ([store folder-name & {:keys [since-uid]}]
   (let [^Folder folder (open-folder store folder-name :readonly)]
     (->> (if-not since-uid
            (.getMessages folder)
            (.getMessagesByUID ^UIDFolder folder (long since-uid) UIDFolder/LASTUID))
          reverse))))

(defn inbox
  "Get all messages from your inbox, newest first."
  ([] (all-messages *store* "inbox"))
  ([store] (all-messages store "inbox")))

(defn search-inbox
  "Search your inbox. `query` is either a single string (matched against
   subject and body) or search conditions as accepted by
   `clojure-mail.folder/search`, e.g. `:subject \"foo\" :received-on :today`.
   Returns a vector of messages."
  [store & query]
  (let [inbox (open-folder store "inbox" :readonly)]
    (into []
          (apply folder/search inbox query))))
