(ns clojure-mail.message
  (:require [medley.core :refer [filter-keys]])
  (:import [javax.mail.internet InternetAddress MimeMultipart MimeMessage]
           [javax.mail Message$RecipientType]))

(defn mime-type
  "Determine the function to call to get the body text of a message"
  [type]
  (let [infered-type
        (clojure.string/lower-case
         (first (clojure.string/split type #"[;]")))]
    (condp = infered-type
      "multipart/alternative" :multipart
      "text/html" :html
      "text/plain" :plain)))

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
  (imap-address->map (.getFrom m)))

(defn subject
  "Fetch the subject of a mail message"
  [^MimeMessage msg]
  (.getSubject msg))

(defn sender
  "Extract the message sender"
  [^MimeMessage msg]
  (imap-address->map (.getSender msg)))

;; Dates
;; *********************************************************

(defn date-sent
  "Return the date a mail message was sent"
  [m]
  (.getSentDate m))

(defn date-received
  "Return the date a message was received"
  [m]
  (.getReceivedDate m))

;; Flags
;; *********************************************************

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

(defn message-headers
  "Returns all the headers from a message"
  [^MimeMessage msg]
  (let [headers (.getAllHeaders msg)
        results (enumeration-seq headers)]
    (into {}
          (map #(vector (.getName %) (.getValue %)) results))))

(defn- multipart? [m]
  "Returns true if a message is a multipart email"
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
  [^MimeMessage msg]
  "Read all the body content from a message
   If the message is multipart then a vector is
   returned containing each message
   [{:content-type \"TEXT\\PLAIN\" :body \"Foo\"}
    {:content-type \"TEXT\\HTML\"  :body \"Bar\"}]"
  [msg]
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

;; Public API for working with messages
;; *********************************************************

(defn read-message [msg & {:keys [fields]}]
  "Returns a workable map of the message content.
   This is the ultimate goal in extracting a message
   as a clojure map.
   Any errors that occured while fetching the fields are added to the :errors field of the map.
   Options:
   fields - a list of the available fields you want to return, defaults to all fields"
  (let [fields-map {:id id
                    :to to
                    :cc cc
                    :bcc bcc
                    :from from
                    :sender sender
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
