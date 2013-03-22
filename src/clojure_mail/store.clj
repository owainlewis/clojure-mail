(ns clojure-mail.store
    (:import [java.util Properties]
             [javax.mail Session Store]))

;; IMAPStore abstractions

(defn- store
  "An abstract class that models a message store and its access protocol,
  for storing and retrieving messages. Subclasses provide actual implementations."
  [protocol server user pass]
  (letfn [(as-properties [m] (let [p (Properties.)]
    (doseq [[k v] m]
      (.setProperty p (str k) (str v)))
    p))]
  (let [p (as-properties [["mail.store.protocol" protocol]])]
    (doto (.getStore (Session/getDefaultInstance p) protocol)
      (.connect server user pass)))))

(defn connected?
  "Returns true if a connection is established"
  [^com.sun.mail.imap.IMAPStore s]
  (.isConnected s))

(defn close
  [s]
  (.close s))

(defn get-default-folder
  ^{:doc "Returns a Folder object that represents the 'root' of the default
          namespace presented to the user by the Store."}
  [^com.sun.mail.imap.IMAPStore s]
  (.getDefaultFolder s))

(defn get-folder
  "Return the Folder object corresponding to the given name."
  [^com.sun.mail.imap.IMAPStore s name]
  (.getFolder s name))

(defn make-store
  "Create a new mail store"
  [client user pass]
  (let [protocol (get client :protocol)
        server (get client :server)]
    (try
      (store protocol server user pass)
    (catch javax.mail.AuthenticationFailedException e
      (format "Invalid credentials %s : %s - %s" user pass (.getMessage e))))))
