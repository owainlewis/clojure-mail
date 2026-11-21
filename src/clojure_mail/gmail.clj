(ns clojure-mail.gmail
  "Convenience functions for connecting to and reading from Gmail over IMAP."
  (:require [clojure-mail.core :as mail]))

(defn get-session
  "Return a JavaMail Session configured for IMAPS."
  []
  (mail/get-session "imaps"))

(defn store
  "Connect to Gmail's IMAP server using an email address and password (or app password)."
  ([email password]
   (mail/store "imaps" "imap.gmail.com" email password))
  ([session email password]
   (mail/store "imaps" session "imap.gmail.com" email password)))

(defn xoauth-store
  "Connect to Gmail's IMAP server using an OAuth 2 access token."
  ([email oauth-access-token]
   (mail/xoauth2-store "imaps" "imap.gmail.com" email oauth-access-token)))

(def gmail-folders
  {:inbox     "INBOX"
   :all       "[Gmail]/All Mail"
   :drafts    "[Gmail]/Drafts"
   :important "[Gmail]/Important"
   :sent      "[Gmail]/Sent Mail"
   :spam      "[Gmail]/Spam"
   :starred   "[Gmail]/Starred"
   :trash     "[Gmail]/Trash"})

(defn folder->folder-name
  "Resolve a Gmail folder keyword (e.g. `:sent`) to its IMAP folder name,
   or return `folder` unchanged."
  [folder]
  (get gmail-folders folder folder))

(defn open-folder
  "Open a Gmail folder by keyword (see `gmail-folders`) or string name."
  ([folder-name perm-level]
   (mail/open-folder (folder->folder-name folder-name) perm-level))
  ([store folder-name perm-level]
   (mail/open-folder store (folder->folder-name folder-name) perm-level)))
