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

(defn as-props
  [m]
  (let [p (Properties.)]
    (doseq [[k v] m]
      (.setProperty p (str k) (str v)))
    p))

(defn store
  "An abstract class that models a message store and its access protocol,
  for storing and retrieving messages.
  Subclasses provide actual implementations."
  [protocol server user pass]
  (let [p (as-props [["mail.store.protocol" protocol]])]
    (doto (.getStore (Session/getDefaultInstance p) protocol)
      (.connect server user pass))))

(defn sub-folder?
  [f] )

(defn get-default-folder
  ^{:doc "Returns a Folder object that represents the 'root' of the default namespace presented to the user by the Store."}
  [store]
  (.getDefaultFolder store))

(defn get-folder
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
     (let [sub? #(if (= 0 (bit-and (.getType %) Folder/HOLDS_FOLDERS)) false true)]
       (map #(cons (.getName %) (if (sub? %) (folders s %))) (.list f)))))

(defn messages [s fd & opt]
  (let [fd (doto (.getFolder s fd) (.open Folder/READ_ONLY))
        [flags set] opt
        msgs (if opt 
               (.search fd (FlagTerm. (Flags. flags) set)) 
               (.getMessages fd))]
    (map #(vector (.getUID fd %) %) msgs)))

(defn dump [msgs]
  (doseq [[uid msg] msgs]
    (.writeTo msg (java.io.FileOutputStream. (str uid)))))

(defn mail-store
  [client user pass]
  (let [protocol (get client :protocol)
        server (get client :server)]
    (store protocol server user pass)))
