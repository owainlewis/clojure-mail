(ns clojure-mail.message
  (:import [javax.mail.internet InternetAddress]))

(defn mime-type
  "Determine the function to call to get the body text of a message"
  [type]
  (let [infered-type
         (clojure.string/lower-case
           (first (clojure.string/split type #"[;]")))]
  (condp = infered-type
    "multipart/alternative" :multipart
    "text/html" :html
    "text/plain" :plain
    (str "unexpected type, \"" type \"))))

(defn to
  "Returns a sequence of receivers"
  [m]
  (map str
    (.getRecipients m javax.mail.Message$RecipientType/TO)))

(defn from
  [m]
  (InternetAddress/toString
    (.getFrom m)))

(defn subject
  "Fetch the subject of a mail message"
  [m]
  (.getSubject m))

(defn sender
  "Extract the message sender"
  [m]
  (.getSender m))

;; Dates
;; *********************************************************

(defn date-sent
  "Return the date a mail message was sent"
  [m]
  (.toString
    (.getSentDate m)))

(defn date-recieved
  "Return the date a message was recieved"
  [m]
  (.toString
    (.getReceivedDate m)))

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
  [^com.sun.mail.imap.IMAPMessage msg]
  (let [headers (.getAllHeaders msg)
        results (enumeration-seq headers)]
    (into {}
      (map #(vector (.getName %) (.getValue %)) results))))

(defn- multipart? [m]
  "Returns true if a message is a multipart email"
  (.startsWith (content-type m) "multipart"))

(defn- read-multi [mime-multi-part]
  (let [count (.getCount mime-multi-part)]
    (for [part (map #(.getBodyPart mime-multi-part %) (range count))]
      (if (multipart? part)
        (.getContent part)
        part))))

(defn- message-parts
  [^javax.mail.internet.MimeMultipart msg]
  (if (multipart? msg)
    (read-multi (get-content msg))))

(defn msg->map
  "Convert a mail message body into a Clojure map
   with content type and message contents"
  [msg]
  {:content-type (.getContentType msg)
   :body (.getContent msg)})

(defn message-body
  [^com.sun.mail.imap.IMAPMessage msg]
  "Read all the body content from a message
   If the message is multipart then a vector is
   returned containing each message
   [{:content-type \"TEXT\\PLAIN\" :body \"Foo\"}
    {:content-type \"TEXT\\HTML\"  :body \"Bar\"}]"
  [msg]
  (if (multipart? msg)
    (map msg->map (message-parts msg))
    (msg->map msg)))

;; Public API for working with messages
;; *********************************************************

(defmacro safe-get
  "try to perform an action else just return nil"
  [& body]
  `(try
    (do ~@body)
  (catch Exception e#
    nil)))

(defn read-message [msg]
  "Returns a workable map of the message content.
   This is the ultimate goal in extracting a message
   as a clojure map"
  (try
    {:to (safe-get (first (to msg)))
     :from (safe-get (sender msg))
     :subject (safe-get (subject msg))
     :date-sent (safe-get (date-sent msg))
     :date-recieved (safe-get (date-recieved msg))
     :multipart? (safe-get (multipart? msg))
     :content-type (safe-get (content-type msg))
     :body (safe-get (message-body msg)) }
  (catch Exception e {:error e})))
