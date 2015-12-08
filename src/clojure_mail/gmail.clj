(ns clojure-mail.gmail
  "some convenience functions when using gmail"
  (:require [clojure-mail.core :as mail]))

(defn get-session [] (mail/get-session "imaps"))

(defn store
  ([email password]
   (mail/store "imaps" "imap.gmail.com" email password))
  ([session email password]
   (mail/store "imaps" session "imap.gmail.com" email password)))

(defn xoauth-store
  ([email oauth-access-token]
   (mail/xoauth2-store "imaps" "imap.gmail.com" email oauth-access-token)))

(def gmail-folders
  {:inbox "INBOX"
   :all   "[Gmail]/All Mail"
   :sent  "[Gmail]/Sent Mail"
   :spam  "[Gmail]/Spam"})

(defn folder->folder-name
  [folder]
  (or (folder gmail-folders)
      folder))

(defn open-folder
  ([folder-name perm-level]
   (mail/open-folder (folder->folder-name folder-name) perm-level))
  ([store folder-name perm-level]
   (mail/open-folder store (folder->folder-name folder-name) perm-level)))
