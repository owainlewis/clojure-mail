(ns clojure-mail.imap-message)

(defn message-headers
  "Returns all the headers from a message"
  [^com.sun.mail.imap.IMAPMessage msg]
  (let [headers (.getAllHeaders msg)
        results (enumeration-seq headers)]
    (map #(vector (.getName %) (.getValue %)) results)))
