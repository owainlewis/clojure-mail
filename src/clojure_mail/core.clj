(ns clojure-mail.core
  (use [clojure-mail store message folder])
  (:import [javax.mail Folder Message Flags]
           [javax.mail.internet InternetAddress]
           [javax.mail.search FlagTerm]))

(comment

  ;; A sample session
  ;; First we make a store connection
  (def store (mail-store gmail "user@gmail.com" "password"))
  ;; We can verify the connection
  (connected? store) 

  ;; Lets grab some messages from the inbox

  (def messages (take 10 (messages store "INBOX")))

)

;; Focus will be more on the reading and parsing of emails.
;; Very rough first draft ideas not suitable for production
;; Sending email is more easily handled by other libs

(def settings (ref {:email "" :password ""}))

(defonce auth ((juxt :email :password) (deref settings)))

(defprotocol Imap
  (connect [a b] "connect to IMAP server"))

(def gmail {:protocol "imaps" :server "imap.gmail.com"})

(defonce last-uid (com.sun.mail.imap.IMAPFolder/LASTUID))

;; TODO map of gmail folder defaults

(def gmail-sent "[Gmail]/Sent Mail")

(def gmail-spam "[Gmail]/Spam")

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
        msgs (.getMessages fd)]
    (map #(vector (.getUID fd %) %) msgs)))

(defn message-content-type
  "Returns the content type of a message object"
  [^javax.mail.internet.MimeMultipart msg]
  (.getContentType msg))

(defn is-mime-type?
  [msg type]
  (.isMimeType msg type))

(defn message-count
  "Returns the number of messages in a folder"
  [store folder]
  (let [fd (doto (.getFolder store folder) (.open Folder/READ_ONLY))]
    (.getMessageCount fd)))

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
    (.writeTo msg (java.io.FileOutputStream. (format "/usr/local/messages/%s" (str uid))))))