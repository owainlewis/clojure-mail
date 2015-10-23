(ns clojure-mail.events
  (:require [clojure-mail.message :refer [read-message]])
  (:import (com.sun.mail.imap IdleManager)
           (java.util.concurrent Executors)
           (javax.mail.event MessageCountEvent MessageCountListener)))

(defn event->map
  [^MessageCountEvent e]
  {:messages (.getMessages e)
   :type     (let [t (.getType e)]
               (if (= 1 t)
                 :added
                 :removed))})

(defn- message-count-listener
  [message-added message-removed folder ^IdleManager idle-manager]
  (reify MessageCountListener
    (^void messagesAdded [this ^MessageCountEvent event]
      (message-added event)
      (.watch idle-manager folder))
    (^void messagesRemoved [this ^MessageCountEvent event]
      (message-removed event)
      (.watch idle-manager folder))))

(defn add-message-count-listener
  "add a message count listener to a folder
  message-added - a function of 1 argument (an event) that is called when the folder message count increases
  message-removed - a function of 1 argument (an event) that is called when the folder message count decreases
  folder - the folder you want to add the listener to, this folder must have been opened
  idle-manager - an idle manager, the idle manager should be stopped when you no longer wish to watch the folder
  An event is a map {:messages [javax.mail.Message] :type :added|:removed}"
  [message-added message-removed folder idle-manager]
  (.addMessageCountListener folder
                            (message-count-listener (comp message-added event->map)
                                                    (comp message-removed event->map)
                                                    folder idle-manager))
  (.watch idle-manager folder))

(defn new-idle-manager
  "An idle manager is required to add a message count listener to a folder"
  [session]
  (let [es (Executors/newCachedThreadPool)]
    (IdleManager. session es)))

(defn stop
  "stop the idle manager"
  [^IdleManager idle-manager]
  (.stop idle-manager))

