(ns clojure-mail.message)

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

(defn message-headers
  "Returns all the headers from a message"
  [^com.sun.mail.imap.IMAPMessage msg]
  (let [headers (.getAllHeaders msg)
        results (enumeration-seq headers)]
    (map #(vector (.getName %) (.getValue %)) results)))
