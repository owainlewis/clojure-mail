o(ns clojure-mail.folder)

;; note that the get folder fn is part of the store namespace

(def ^:dynamic current-folder)

(defmacro with-folder [folder store & body]
  `(let [fd# (doto (.getFolder ~store ~folder) (.open Folder/READ_ONLY))]
     (binding [current-folder fd#]
       (do ~@body))))

(defn full-name [f]
  (.getFullName f))

(defn new-message-count [f]
  (.getNewMessageCount f))

(defn get-message-by-uid [f id]
  (.getMessageByUID f id))

(defn get-message [f id]
  (.getMessage f id))