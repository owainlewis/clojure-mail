(ns clojure-mail.events
  (:import (com.sun.mail.imap IdleManager)
           (java.util Collections WeakHashMap)
           (java.util.concurrent ExecutorService Executors ThreadFactory)
           (javax.mail Folder Service Session)
           (javax.mail.event ConnectionEvent ConnectionListener MessageCountEvent MessageCountListener)))

(def ^:private idle-executors
  (Collections/synchronizedMap (WeakHashMap.)))

(defn- daemon-thread-factory
  []
  (reify ThreadFactory
    (newThread [_ runnable]
      (doto (Thread. runnable "clojure-mail-idle")
        (.setDaemon true)))))

(defn event->map
  [^MessageCountEvent e]
  {:messages (.getMessages e)
   :type     (if (= MessageCountEvent/ADDED (.getType e))
               :added
               :removed)})

(defn connection-event->map
  "Convert a `javax.mail.event.ConnectionEvent` into a map with `:type`
   (`:opened`, `:disconnected`, or `:closed`) and `:source`."
  [^ConnectionEvent e]
  {:type   (condp = (.getType e)
             ConnectionEvent/OPENED :opened
             ConnectionEvent/DISCONNECTED :disconnected
             ConnectionEvent/CLOSED :closed
             :unknown)
   :source (.getSource e)})

(defn add-connection-listener
  "Attach a `ConnectionListener` to a `Folder` or `Store`/`Service` to observe
   connection lifecycle events (such as a dropped IMAP IDLE connection).
   `handlers` is a map with optional keys `:opened`, `:disconnected`, and `:closed`,
   each a 1-arg function receiving the event map from `connection-event->map`.
   Returns the registered `ConnectionListener`."
  [target {:keys [opened disconnected closed]}]
  (let [listener (reify ConnectionListener
                   (^void opened [_ ^ConnectionEvent event]
                     (when opened
                       (opened (connection-event->map event))))
                   (^void disconnected [_ ^ConnectionEvent event]
                     (when disconnected
                       (disconnected (connection-event->map event))))
                   (^void closed [_ ^ConnectionEvent event]
                     (when closed
                       (closed (connection-event->map event)))))]
    (cond
      (instance? Folder target) (.addConnectionListener ^Folder target listener)
      (instance? Service target) (.addConnectionListener ^Service target listener)
      :else (throw (IllegalArgumentException.
                    (str "Expected a javax.mail.Folder or javax.mail.Service, got: " (type target)))))
    listener))

(defn running?
  "Returns true if the `IdleManager` is still running."
  [^IdleManager idle-manager]
  (.isRunning idle-manager))

(defn- rewatch!
  [^Folder folder ^IdleManager idle-manager on-error]
  (when (and (.isRunning idle-manager) (.isOpen folder))
    (try
      (.watch idle-manager folder)
      (catch Exception e
        (when on-error
          (on-error e))))))

(defn- message-count-listener
  [message-added message-removed ^Folder folder ^IdleManager idle-manager on-error]
  (reify MessageCountListener
    (^void messagesAdded [_ ^MessageCountEvent event]
      (message-added event)
      (rewatch! folder idle-manager on-error))
    (^void messagesRemoved [_ ^MessageCountEvent event]
      (message-removed event)
      (rewatch! folder idle-manager on-error))))

(defn add-message-count-listener
  "Add a message count listener to a folder and begin watching it with `idle-manager`.
  message-added - a function of 1 argument (an event) that is called when the folder message count increases
  message-removed - a function of 1 argument (an event) that is called when the folder message count decreases
  folder - the folder you want to add the listener to; this folder must have been opened
  idle-manager - an idle manager; stop it with `stop` when you no longer wish to watch the folder
  opts - optional map supporting:
    :on-closed       - 1-arg fn called with a connection event map when the folder closes (e.g. dropped IDLE)
    :on-disconnected - 1-arg fn called with a connection event map when the folder disconnects
    :on-error        - 1-arg fn called with an exception if re-watching the folder fails
  An event is a map {:messages [javax.mail.Message] :type :added|:removed}"
  ([message-added message-removed ^Folder folder ^IdleManager idle-manager]
   (add-message-count-listener message-added message-removed folder idle-manager nil))
  ([message-added message-removed ^Folder folder ^IdleManager idle-manager
    {:keys [on-closed on-disconnected on-error]}]
   (when (or on-closed on-disconnected)
     (add-connection-listener folder {:closed on-closed
                                      :disconnected on-disconnected}))
   (.addMessageCountListener folder
                             (message-count-listener (comp message-added event->map)
                                                     (comp message-removed event->map)
                                                     folder
                                                     idle-manager
                                                     on-error))
   (.watch idle-manager folder)))

(defn new-idle-manager
  "An idle manager is required to add a message count listener to a folder.
   Creates a daemon-backed executor pool that is shut down when `stop` is called."
  [^Session session]
  (let [es (Executors/newCachedThreadPool (daemon-thread-factory))
        manager (IdleManager. session es)]
    (.put ^java.util.Map idle-executors manager es)
    manager))

(defn stop
  "Stop the idle manager and shut down its executor pool if created by `new-idle-manager`."
  [^IdleManager idle-manager]
  (try
    (.stop idle-manager)
    (finally
      (when-let [^ExecutorService es (.remove ^java.util.Map idle-executors idle-manager)]
        (.shutdownNow es)))))

