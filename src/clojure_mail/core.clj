(ns clojure-mail.core
  (:import [java.util Properties]
           [javax.mail Session Store Folder Message Flags]
           [javax.mail.internet InternetAddress]
           [javax.mail.search FlagTerm]))

;; Focus will be more on the reading and parsing of emails.
;; Very rough first draft ideas not suitable for production
;; Sending email is more easily handled by other libs
;; IMAP client interface

;; TODO Refactor everything

(def settings (ref {:email "" :password ""}))

(defonce auth ((juxt :email :password) (deref settings)))

(defprotocol Imap
  (connect [a b] "connect to IMAP server"))

(def gmail {:protocol "imaps" :server "imap.gmail.com"})

(defonce last-uid (com.sun.mail.imap.IMAPFolder/LASTUID))

;; TODO map of gmail folder defaults

(def gmail-sent "[Gmail]/Sent Mail")

;; ============================
;; Move to store namespace
;; com.sun.mail.imap.IMAPStore
;; ============================

(defn- store
  "An abstract class that models a message store and its access protocol,
  for storing and retrieving messages. Subclasses provide actual implementations."
  [protocol server user pass]
  (letfn [(as-properties [m] (let [p (Properties.)]
    (doseq [[k v] m]
      (.setProperty p (str k) (str v)))
    p))]
  (let [p (as-properties [["mail.store.protocol" protocol]])]
    (doto (.getStore (Session/getDefaultInstance p) protocol)
      (.connect server user pass)))))

(defn connected?
  "Returns true if a connection is established"
  [^com.sun.mail.imap.IMAPStore s]
  (.isConnected s))

(defn close
  [^com.sun.mail.imap.IMAPStore s]
  (.close s))

(defn- get-default-folder
  ^{:doc "Returns a Folder object that represents the 'root' of the default
          namespace presented to the user by the Store."}
  [^com.sun.mail.imap.IMAPStore s]
  (.getDefaultFolder s))

(defn- get-folder
  "Return the Folder object corresponding to the given name."
  [^com.sun.mail.imap.IMAPStore s name]
  (.getFolder s name))

;; End Store

(def sub-folder?
  (fn [_]
  (if (= 0 (bit-and (.getType _) Folder/HOLDS_FOLDERS)) false true)))

(defn folder-seq
  "Used to get a sequence of folder names. Note that this does not recursively
   loop through subfolders like the implementation below"
  [store]
  (let [default (get-default-folder store)]
    (map (fn [x] (.getName x))
         (.list (get-default-folder store)))))

(defn all-messages
  ^{:doc "Refactored messages fn below. Given a store and folder returns all
   messages. Be aware that there may be a large volume of mail so consider
   taking x items rather than the entire contents of the folder"}
  [^com.sun.mail.imap.IMAPStore store folder]
  (let [s (.getDefaultFolder store)
        inbox (.getFolder s folder)
        folder (doto inbox (.open Folder/READ_ONLY))]
    (.getMessages folder)))

(defn message-headers
  "Returns all the headers from a message"
  [^com.sun.mail.imap.IMAPMessage msg]
  (let [headers (.getAllHeaders msg)
        results (enumeration-seq headers)]
    (map #(vector (.getName %) (.getValue %)) results)))

(defn folders
  "Returns a seq of all IMAP folders inlcuding sub folders"
  ([s] (folders s (.getDefaultFolder s)))
  ([s f]
  (map
    #(cons (.getName %)
      (if (sub-folder? %)
        (folders s %)))
          (.list f))))

(defn messages [s fd & opt]
  (let [fd (doto (.getFolder s fd) (.open Folder/READ_ONLY))
        [flags set] opt
        msgs (if opt 
               (.search fd (FlagTerm. (Flags. flags) set)) 
               (.getMessages fd))]
    (map #(vector (.getUID fd %) %) msgs)))

(defn message-content-type
  "Returns the content type of a message object"
  [^javax.mail.internet.MimeMultipart msg]
  (.getContentType msg))

;; There are a ton of these methods that need adding
;; Split into Mime namespace and do the same for other classes here when doing refactor

(defn get-msg-size
  "Returns message size in bytes"
  [msg]
  (.getSize msg))

(defn is-mime-type?
  [msg type]
  (.isMimeType msg type))

(defn message-count
  "Returns the number of messages in a folder"
  [store folder]
  (let [fd (doto (.getFolder store folder) (.open Folder/READ_ONLY))]
    (.getMessageCount fd)))

(def ^:dynamic current-folder)

(defmacro with-folder [folder store & body]
  `(let [fd# (doto (.getFolder ~store ~folder) (.open Folder/READ_ONLY))]
     (binding [current-folder fd#]
       (do ~@body))))

(defn get-body-text
  "Determine the function to call to get the body text of a message"
  [msg type]
  (condp = type
    "multipart/alternative" :multipart
    "text/html" :html
    "text/plain" :plain
    (str "unexpected type, \"" type \")))

(defn get-msg-parts
  [^javax.mail.internet.MimeMultipart msg]
  (let [no-parts (get (clojure.core/bean msg) :count)
        parts (map #(.getBodyPart msg %) (range no-parts))]
    parts))

(defn read-msg
  "Read a single message"
  ([msg]
  (let [message (clojure.core/bean msg)
        from (get message :from)]
    from)))

(defn print-message
  "Debugging only. Prints out all UIDs and message instances to console"
  [message]
  (doseq [[uid msg] message]
    (println 
      (format "%s - %s" uid (clojure.core/bean msg)))))
  
(defn dump [msgs]
  (doseq [[uid msg] msgs]
    (.writeTo msg (java.io.FileOutputStream. (str uid)))))

(defn mail-store
  "Create a new mail store"
  [client user pass]
  (let [protocol (get client :protocol)
        server (get client :server)]
    (store protocol server user pass)))
