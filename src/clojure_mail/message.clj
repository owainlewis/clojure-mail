(ns clojure-mail.message
  (import [javax.mail.internet MimeMultipart]))

(defn from [m]
  (.getFrom m))

(defn subject [m]
  (.getSubject m))

(defn sender [m]
  (.getSender m))

(defn content-type [m]
  (let [type (.getContentType m)]
    type))

(defn in-reply-to [m]
  (.getInReplyTo m))

(defn message-id [m]
  (.getMessageID m))

(defn encoding [m]
  (.getEncoding m))

(defn get-content [m]
  (.getContent m))

(defn message-headers
  "Returns all the headers from a message"
  [^com.sun.mail.imap.IMAPMessage msg]
  (let [headers (.getAllHeaders msg)
        results (enumeration-seq headers)]
    (map #(vector (.getName %) (.getValue %)) results)))

(defn multipart? [m]
  ^{:doc "Returns true if a message is a multipart email"}
  (.startsWith (content-type m) "multipart"))

(defn read-multi [mime-multi-part]
  (let [count (.getCount mime-multi-part)]
    (for [part (map #(.getBodyPart mime-multi-part %) (range count))]
      (if (multipart? part)
        (.getContent part)
        part))))

(defn message-parts
  [^javax.mail.internet.MimeMultipart msg]
  (when (multipart? msg)
    (read-multi (get-content msg))))

(defn message-body-map
  "Read all the body content from a message"
  [msg]
  (let [parts (message-parts msg)]
    (map #(.getContent %) parts)))

(defn dump-message [msg]
  ())
