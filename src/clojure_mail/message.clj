(ns clojure-mail.message
  (:require [medley.core :refer [filter-keys]])
  (:import [javax.mail.internet InternetAddress MimeMultipart MimeMessage]
           [javax.mail Message$RecipientType Flags Flags$Flag]))

(defn mime-type
  "Determine the function to call to get the body text of a message"
  [type]
  (let [infered-type
        (clojure.string/lower-case
         (first (clojure.string/split type #"[;]")))]
    (condp = infered-type
      "multipart/alternative" :multipart
      "multipart/mixed" :multipart
      "text/html" :html
      "text/plain" :plain
      nil)))

(defn imap-address->map
  [^InternetAddress address]
  {:address  (.getAddress address)
   :name (.getPersonal address)})

(defn recipients
  [^MimeMessage msg recipient-type]
  (map imap-address->map
       (.getRecipients msg recipient-type)))

(defn to
  "Returns a sequence of receivers"
  [m]
  (recipients m Message$RecipientType/TO))

(defn cc
  "Returns a sequence of receivers"
  [m]
  (recipients m Message$RecipientType/CC))

(defn bcc
  "Returns a sequence of receivers"
  [m]
  (recipients m Message$RecipientType/BCC))

(defn from
  [m]
  (map imap-address->map (.getFrom m)))

(defn subject
  "Fetch the subject of a mail message"
  [^MimeMessage msg]
  (.getSubject msg))

(defn sender
  "Extract the message sender"
  [^MimeMessage msg]
  (let [address (.getSender msg)]
    (when address
      (imap-address->map address))))

(defn date-sent
  "Return the date a mail message was sent"
  [m]
  (.getSentDate m))

(defn date-received
  "Return the date a message was received"
  [m]
  (.getReceivedDate m))

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

(defn deleted?
  [message]
  (has-flag? message "DELETED"))

(defn draft?
  [message]
  (has-flag? message "DRAFT"))

(defn flagged?
  [message]
  (has-flag? message "FLAGGED"))

(defn recent?
  [message]
  (has-flag? message "RECENT"))

(defn in-reply-to [m]
  (.getInReplyTo m))

(defn id [m]
  (.getMessageID m))

(defn encoding [m]
  (.getEncoding m))

(defn get-content [m]
  (.getContent m))

(defn uid
  "return the uid of the message"
  [message]
  (let [folder (.getFolder message)]
    (.getUID folder message)))

(defn message-headers
  "Returns all the headers from a message"
  [^MimeMessage msg]
  (let [headers (.getAllHeaders msg)
        results (enumeration-seq headers)]
    (into []
          (map #(hash-map (.getName %) (.getValue %)) results))))

(defn- multipart?
  "Returns true if a message is a multipart email"
  [m]
  (.startsWith (content-type m) "multipart"))

(defn msg->map
  "Convert a mail message body into a Clojure map
   with content type and message contents"
  [msg]
  {:content-type (.getContentType msg)
   :body         (.getContent msg)})

(defn read-multi [mime-multi-part]
  (let [count (.getCount mime-multi-part)]
    (for [part (map #(.getBodyPart mime-multi-part %) (range count))]
      (if (multipart? part)
        (read-multi (.getContent part))
        (msg->map part)))))

(defn message-parts
  [^MimeMultipart msg]
  (when (multipart? msg)
    (read-multi
     (get-content msg))))

(defn message-body
  "Read all the body content from a message
   If the message is multipart then a vector is
   returned containing each message
   [{:content-type \"TEXT\\PLAIN\" :body \"Foo\"}
    {:content-type \"TEXT\\HTML\"  :body \"Bar\"}]"
  [^MimeMessage msg]
  (if (multipart? msg)
    (message-parts msg)
    (msg->map msg)))

(defn add-field
  "add the result of (f msg) to a map m, if there is an Exception, add the Exception to the :error field of the map"
  [msg m k f]
  (try
    (assoc m k (f msg))
    (catch Exception e
      (update-in m [:errors] (partial cons e)))))

(defn read-message
  "Returns a workable map of the message content.
   This is the ultimate goal in extracting a message
   as a clojure map.
   Any errors that occured while fetching the fields are added to the :errors field of the map.
   Options:
   fields - a list of the available fields you want to return, defaults to all fields"
  [msg & {:keys [fields]}]
  (let [fields-map {:id id
                    :to to
                    :cc cc
                    :bcc bcc
                    :from from
                    :sender sender
                    :subject subject
                    :date-sent date-sent
                    :date-received date-received
                    :multipart? multipart?
                    :content-type content-type
                    :body message-body
                    :headers message-headers}
        kvs (if fields
              (filter-keys (set fields) fields-map)
              fields-map)]
    (reduce-kv (partial add-field msg) {} kvs)))

(defn- set-flag
  "Sets a flag on a message"
  [msg flag]
  (.setFlags msg flag true))

(defn mark-read
  "Set SEEN flag on a message"
  [msg]
  (set-flag msg (Flags. Flags$Flag/SEEN)))

(defn mark-deleted
  "Set DELETED flag on a message"
  [msg]
  (set-flag msg (Flags. Flags$Flag/DELETED)))

(defn mark-answered
  "Set ANSWERED flag on a message"
  [msg]
  (set-flag msg (Flags. Flags$Flag/ANSWERED)))

(defn mark-flagged
  "Set FLAGGED flag on a message"
  [msg]
  (set-flag msg (Flags. Flags$Flag/FLAGGED)))

(defn mark-as-flagged
  "Set DRAFT flag on a message"
  [msg]
  (set-flag msg (Flags. Flags$Flag/DRAFT)))

(defn mark-recent
  "Set RECENT flag on a message"
  [msg]
  (set-flag msg (Flags. Flags$Flag/RECENT)))
