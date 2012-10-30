(ns clojure-mail.core
  (:require [clojure-mail.store :as store]
            [clojure-mail.message :as msg]
            [clojure-mail.folder :as folder])
  (:import [javax.mail Folder Message Flags Flags$Flag]
           [javax.mail.internet InternetAddress]
           [javax.mail.search FlagTerm]))

;; Focus will be more on the reading and parsing of emails.
;; Very rough first draft ideas not suitable for production
;; Sending email is more easily handled by other libs

(def settings (ref {}))

(defn auth! [email pass]
  (dosync
    (ref-set settings
      {:email email :pass pass})))

(def gmail
  {:protocol "imaps"
   :server "imap.gmail.com"})

(defn gen-store []
  (apply store/mail-store
    (cons gmail
      ((juxt :email :pass) @settings))))

(def folder-names
  {:inbox "INBOX"
   :all "[Gmail]/All Mail"
   :sent "[Gmail]/Sent Mail"
   :spam "[Gmail]/Spam"})

(def sub-folder?
  "Check if a folder is a sub folder"
  (fn [folder]
    (if (= 0 (bit-and (.getType folder) Folder/HOLDS_FOLDERS))
      false
      true)))

(defn folder-seq
  "Used to get a sequence of folder names. Note that this does not recursively
   loop through subfolders like the implementation below"
  [store]
  (let [default (store/get-default-folder store)]
    (map (fn [x] (.getName x))
         (.list (store/get-default-folder store)))))

(defn all-messages
  ^{:doc "Given a store and folder returns all messages."}
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

(defn message-count
  "Returns the number of messages in a folder"
  [store folder]
  (let [fd (doto (.getFolder store folder) (.open Folder/READ_ONLY))]
    (.getMessageCount fd)))

(defn read-all
  [folder]
  (all-messages (gen-store) folder))

(defn get-inbox []
  "Returns all messages from the inbox"
  (read-all (get folder-names :inbox)))

(defn get-spam []
  (read-all (get folder-names :spam)))

(defn unread-messages
  "Find unread messages"
  [fd]
  (let [fd (doto (.getFolder (gen-store) fd) (.open Folder/READ_ONLY))
        msgs (.search fd (FlagTerm. (Flags. Flags$Flag/SEEN) false))]
    msgs))
  
(defn dump
  "Handy function that dumps out a batch of emails to disk"
  [msgs]
  (doseq [[uid msg] msgs]
    (.writeTo msg (java.io.FileOutputStream.
      (format "/usr/local/messages/%s" (str uid))))))

;;; Sending mail

(defn create-message
  "Create an instance of javax.mail.internet.MimeMessage"
  [from to subject body]
  (let [msg (javax.mail.internet.MimeMessage. session)]                                             
    (doto msg
      (.setFrom (javax.mail.internet.InternetAddress. from))
      (.addRecipients javax.mail.Message$RecipientType/TO to)
      (.setSubject subject)
      (.setText body)
      (.setHeader "X-Mailer", "msgsend")
      (.setSentDate (java.util.Date.)))))