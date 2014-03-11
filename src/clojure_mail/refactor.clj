(ns clojure-mail.refactor
  (:import [java.util Properties]
           [javax.mail Session Store]))

(def gmail
  {:protocol "imaps"
   :server   "imap.gmail.com"})

(defn store
  "An abstract class that models a message store and its access protocol,
   for storing and retrieving messages.
   Subclasses provide actual implementations."
  [protocol server user pass]
  (letfn [(as-properties [m] (let [p (Properties.)]
    (doseq [[k v] m]
      (.setProperty p (str k) (str v)))
    p))]
  (let [p (as-properties [["mail.store.protocol" protocol]])]
    (doto (.getStore (Session/getDefaultInstance p) protocol)
      (.connect server user pass)))))

(def gmail-store
  (let [{:keys [protocol server]} gmail]
    (partial store protocol server)))
