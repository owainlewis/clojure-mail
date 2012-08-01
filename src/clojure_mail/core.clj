(ns clojure-mail.core
  (:import [java.util Properties]
           [javax.mail Session Store Folder Message Flags]
           [javax.mail.internet InternetAddress]
           [javax.mail.search FlagTerm]))

;; Focus will be more on the reading and parsing of emails.

;; Sending email is more easily handled by other libs

;; IMAP client interface

(defn IMAP [] )

(def gmail {:protocol "imaps" :server "imap.gmail.com"})

;; TODO map of gmail folder defaults

(def gmail-sent "[Gmail]/Sent Mail")

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

(def- sub-folder?
  (fn [_]
  (if (= 0 (bit-and (.getType _) Folder/HOLDS_FOLDERS)) false true)))

(defn- get-default-folder
  ^{:doc "Returns a Folder object that represents the 'root' of the default namespace presented to the user by the Store."}
  [store]
  (.getDefaultFolder store))

(defn- get-folder
  "Return the Folder object corresponding to the given name."
  [store name]
  (.getFolder store name))

(defn folder-seq
  "Used to get a sequence of folder names. Note that this does not recursively
   loop through subfolders like the implementation below"
  [store]
  (let [default (get-default-folder store)]
    (map (fn [x] (.getName x))
      (.list (get-default-folder c)))))
  
(defn folders 
  ([s] (folders s (.getDefaultFolder s)))
  ([s f]
  (map
    #(cons (.getName %)
      (if (sub-folder? %)
        (folders s %)))
          (.list f))))

(defn messages [s fd & opt]
  (let [fd (doto (.getFolder s fd) (.open Folder/READ_ONLY))
        [flags set] opt
        msgs (if opt 
               (.search fd (FlagTerm. (Flags. flags) set)) 
               (.getMessages fd))]
    (map #(vector (.getUID fd %) %) msgs)))

(defn read-msg
  "Read a single message and print out to the terminal"
  [msg] msg)

(defn print-all-messages
  "Debugging only. Prints out all UIDs and message instances to console"
  [messages]
  (doseq [[uid msg] messages]
    (println 
      (format "%s - %s" uid msg))))
  
(defn dump [msgs]
  (doseq [[uid msg] msgs]
    (.writeTo msg (java.io.FileOutputStream. (str uid)))))

(defn mail-store
  "Create a new mail store"
  [client user pass]
  (let [protocol (get client :protocol)
        server (get client :server)]
    (store protocol server user pass)))
