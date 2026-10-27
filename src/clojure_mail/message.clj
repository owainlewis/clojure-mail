(ns clojure-mail.message
  (:require [clojure.string :as str])
  (:import [javax.mail.internet InternetAddress MimeMultipart MimeMessage]
           [javax.mail Message Message$RecipientType Flags Flags$Flag Multipart Part UIDFolder]))

(defn mime-type
  "Determine the function to call to get the body text of a message"
  [type]
  (let [infered-type
        (str/lower-case
         (str/trim (first (str/split type #"[;]"))))]
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
  [^Message msg recipient-type]
  (map imap-address->map
       (.getRecipients msg ^Message$RecipientType recipient-type)))

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
  [^Message m]
  (map imap-address->map (.getFrom m)))

(defn subject
  "Fetch the subject of a mail message"
  [^Message msg]
  (.getSubject msg))

(defn sender
  "Extract the message sender"
  [^MimeMessage msg]
  (let [address (.getSender msg)]
    (when address
      (imap-address->map address))))

(defn date-sent
  "Return the date a mail message was sent"
  [^Message m]
  (.getSentDate m))

(defn date-received
  "Return the date a message was received"
  [^Message m]
  (.getReceivedDate m))

(defn flags
  [^Message m]
  (.getFlags m))

(defn content-type [^Part m]
  (.getContentType m))

(def ^:private system-flags
  {"ANSWERED" Flags$Flag/ANSWERED
   "DELETED"  Flags$Flag/DELETED
   "DRAFT"    Flags$Flag/DRAFT
   "FLAGGED"  Flags$Flag/FLAGGED
   "RECENT"   Flags$Flag/RECENT
   "SEEN"     Flags$Flag/SEEN
   "USER"     Flags$Flag/USER})

(defn has-flag?
  "Returns true if `message` has `flag` set.
   `flag` can be a `javax.mail.Flags$Flag`, a system flag name such as
   \"SEEN\" or :seen, or the name of a user-defined flag."
  [message flag]
  (let [^Flags f (flags message)]
    (boolean
     (if (instance? Flags$Flag flag)
       (.contains f ^Flags$Flag flag)
       (let [flag-name (name flag)]
         (if-let [system-flag (get system-flags (str/upper-case flag-name))]
           (.contains f ^Flags$Flag system-flag)
           (.contains f ^String flag-name)))))))

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

(defn in-reply-to
  "Returns the In-Reply-To header of a message, or nil"
  [^Message m]
  (first (.getHeader m "In-Reply-To")))

(defn id [^MimeMessage m]
  (.getMessageID m))

(defn encoding [^MimeMessage m]
  (.getEncoding m))

(defn get-content [^Part m]
  (.getContent m))

(defn uid
  "return the uid of the message"
  [^Message message]
  (let [^UIDFolder folder (.getFolder message)]
    (.getUID folder message)))

(defn message-headers
  "Returns all the headers from a message"
  [^Part msg]
  (let [headers (.getAllHeaders msg)
        results (enumeration-seq headers)]
    (into []
          (map (fn [^javax.mail.Header h] (hash-map (.getName h) (.getValue h))) results))))

(defn- multipart?
  "Returns true if a message is a multipart email"
  [m]
  (boolean
   (some-> ^String (content-type m) str/lower-case (str/starts-with? "multipart"))))

(defn msg->map
  "Convert a mail message body into a Clojure map
   with content type and message contents"
  [^Part msg]
  {:content-type (.getContentType msg)
   :body         (.getContent msg)})

(defn read-multi [^Multipart mime-multi-part]
  (let [count (.getCount mime-multi-part)]
    (for [part (map #(.getBodyPart mime-multi-part (int %)) (range count))]
      (if (multipart? part)
        (read-multi (.getContent ^Part part))
        (msg->map part)))))

(defn message-parts
  [^Part msg]
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
              (select-keys fields-map fields)
              fields-map)]
    (reduce-kv (partial add-field msg) {} kvs)))

(defn- set-flag
  "Sets a flag on a message"
  [^Message msg ^Flags flag]
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

(defn mark-draft
  "Set DRAFT flag on a message"
  [msg]
  (set-flag msg (Flags. Flags$Flag/DRAFT)))

(defn ^:deprecated mark-as-flagged
  "Deprecated: despite its name this sets the DRAFT flag.
   Use `mark-draft` for DRAFT or `mark-flagged` for FLAGGED."
  [msg]
  (mark-draft msg))

(defn mark-recent
  "Set RECENT flag on a message"
  [msg]
  (set-flag msg (Flags. Flags$Flag/RECENT)))
