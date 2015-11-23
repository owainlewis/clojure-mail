(ns clojure-mail.folder
  (:refer-clojure :exclude [list])
  (:import [javax.mail.search SearchTerm OrTerm SubjectTerm BodyTerm]
           (com.sun.mail.imap IMAPFolder IMAPFolder$FetchProfileItem IMAPMessage)
           (javax.mail FetchProfile FetchProfile$Item)))

;; note that the get folder fn is part of the store namespace

(def ^:dynamic current-folder)

(defmacro with-folder [folder store & body]
  `(let [fd# (doto (.getFolder ~store ~folder) (.open IMAPFolder/READ_ONLY))]
     (binding [current-folder fd#]
       (do ~@body))))

(defn get-folder
  "Returns an IMAPFolder instance"
  [store folder-name]
  (.getFolder store folder-name))

(defn full-name [f]
  (.getFullName f))

(defn new-message-count
  "Get number of new messages in folder f"
  [f]
  (.getNewMessageCount f))

(defn message-count
  "Get total number of messages in folder f"
  [f]
  (.getMessageCount f))

(defn unread-message-count
  "Get number of unread messages in folder f"
  [f]
  (.getUnreadMessageCount f))

(defn get-message-by-uid [f id]
  (.getMessageByUID f id))

(defn get-message [f id]
  (.getMessage f id))

(defn fetch-messages
  "Pre-fetch message attributes for a given fetch profile.
  Messages are retrieved as light weight objects and individual fields such as headers or body are populated lazily.
  When bulk fetching messages you can pre-fetch these items based on a com.sun.mail.imap.FetchProfileItem
  f - the folder from which to fetch the messages
  ms - the messages to fetch
  :fetch-profile - optional fetch profile, defaults to entire message. fetch profiles are:

      :message
      :headers
      :flags
      :envelope
      :content-info
      :size
      "
  [f ms & {:keys [fetch-profile] :or {fetch-profile :message}}]
  (let [fp (FetchProfile.)
        item (condp = fetch-profile
               :message IMAPFolder$FetchProfileItem/MESSAGE
               :headers IMAPFolder$FetchProfileItem/HEADERS
               :flags IMAPFolder$FetchProfileItem/FLAGS
               :envelope IMAPFolder$FetchProfileItem/ENVELOPE
               :content-info IMAPFolder$FetchProfileItem/CONTENT_INFO
               :size FetchProfile$Item/SIZE)
        _ (.add fp item)]
    (.fetch f (into-array IMAPMessage ms) fp)))

(defn get-messages
  "Gets all messages from folder f or get the Message objects for message numbers ranging from start through end,
  both start and end inclusive. Note that message numbers start at 1, not 0."
  ([folder]
   (.getMessages folder))
  ([folder start end]
   (.getMessages folder start end)))

(defn search [f query]
  (let [search-term (OrTerm. (SubjectTerm. query) (BodyTerm. query))]
    (.search f search-term)))

(defn list
  "List all folders under folder f"
  [f]
  (.list f))
