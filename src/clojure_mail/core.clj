(ns clojure-mail.core
  (:require [clojure-mail.store :as store]
            [clojure-mail.message :as msg]
            [clojure-mail.folder :as folder])
  (:import [javax.mail Folder Message Flags]
           [javax.mail.internet InternetAddress]
           [javax.mail.search FlagTerm]))

;; Focus will be more on the reading and parsing of emails.
;; Very rough first draft ideas not suitable for production
;; Sending email is more easily handled by other libs

(def settings (ref {:email "" :password ""}))

(defonce auth ((juxt :email :password) (deref settings)))

(def gmail {:protocol "imaps" :server "imap.gmail.com"})

(defonce last-uid (com.sun.mail.imap.IMAPFolder/LASTUID))

;; TODO map of gmail folder defaults

(def folders
  {:sent "[Gmail]/Sent Mail"
   :spam "[Gmail]/Spam"})

;; End Store

(def sub-folder?
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

(defn dump
  "Handy function that dumps out a batch of emails to disk"
  [msgs]
  (doseq [[uid msg] msgs]
    (.writeTo msg (java.io.FileOutputStream.
      (format "/usr/local/messages/%s" (str uid))))))