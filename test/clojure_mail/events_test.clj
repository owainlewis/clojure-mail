(ns clojure-mail.events-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure-mail.core :as mail]
            [clojure-mail.events :as events])
  (:import [com.sun.mail.imap IdleManager]
           [java.util Properties]
           [javax.mail Folder Message Session]
           [javax.mail.event ConnectionEvent ConnectionListener MessageCountEvent]
           [javax.mail.internet MimeMessage]))

(defn- dummy-folder
  "Create a minimal proxy Folder for testing listener registration and events."
  [open?]
  (let [session (Session/getInstance (Properties.))
        store (.getStore session "imap")
        conn-listeners (atom [])
        msg-listeners (atom [])
        open-state (atom open?)]
    {:open-state     open-state
     :conn-listeners conn-listeners
     :msg-listeners  msg-listeners
     :folder         (proxy [Folder] [store]
                       (getName [] "INBOX")
                       (getFullName [] "INBOX")
                       (getParent [] nil)
                       (exists [] true)
                       (list [_] (into-array Folder []))
                       (getSeparator [] \/)
                       (getType [] Folder/HOLDS_MESSAGES)
                       (create [_] false)
                       (hasNewMessages [] false)
                       (getFolder [_] nil)
                       (delete [_] false)
                       (renameTo [_] false)
                       (open [_] (reset! open-state true))
                       (close [_] (reset! open-state false))
                       (isOpen [] @open-state)
                       (getPermanentFlags [] nil)
                       (getMessageCount [] 0)
                       (getMessage [_] nil)
                       (appendMessages [_] nil)
                       (expunge [] (into-array Message []))
                       (addConnectionListener [l] (swap! conn-listeners conj l))
                       (addMessageCountListener [l] (swap! msg-listeners conj l)))}))

(deftest event->map-test
  (let [session (Session/getInstance (Properties.))
        msg (MimeMessage. session)
        {:keys [^Folder folder]} (dummy-folder true)
        added (MessageCountEvent. folder MessageCountEvent/ADDED false (into-array Message [msg]))
        removed (MessageCountEvent. folder MessageCountEvent/REMOVED true (into-array Message [msg]))]
    (testing "converts ADDED events"
      (let [m (events/event->map added)]
        (is (= :added (:type m)))
        (is (= [msg] (vec (:messages m))))))
    (testing "converts REMOVED events"
      (let [m (events/event->map removed)]
        (is (= :removed (:type m)))
        (is (= [msg] (vec (:messages m))))))))

(deftest connection-event->map-test
  (let [{:keys [folder]} (dummy-folder true)]
    (is (= {:type :opened :source folder}
           (events/connection-event->map (ConnectionEvent. folder ConnectionEvent/OPENED))))
    (is (= {:type :disconnected :source folder}
           (events/connection-event->map (ConnectionEvent. folder ConnectionEvent/DISCONNECTED))))
    (is (= {:type :closed :source folder}
           (events/connection-event->map (ConnectionEvent. folder ConnectionEvent/CLOSED))))))

(deftest add-connection-listener-test
  (testing "registers a listener on a Folder and dispatches opened/disconnected/closed"
    (let [{:keys [folder conn-listeners]} (dummy-folder true)
          seen (atom [])
          ^ConnectionListener listener (events/add-connection-listener
                                        folder
                                        {:opened       #(swap! seen conj [:opened (:type %)])
                                         :disconnected #(swap! seen conj [:disconnected (:type %)])
                                         :closed       #(swap! seen conj [:closed (:type %)])})]
      (is (= 1 (count @conn-listeners)))
      (.opened listener (ConnectionEvent. folder ConnectionEvent/OPENED))
      (.disconnected listener (ConnectionEvent. folder ConnectionEvent/DISCONNECTED))
      (.closed listener (ConnectionEvent. folder ConnectionEvent/CLOSED))
      (is (= [[:opened :opened]
              [:disconnected :disconnected]
              [:closed :closed]]
             @seen))))
  (testing "throws IllegalArgumentException for unsupported targets"
    (is (thrown? IllegalArgumentException
                 (events/add-connection-listener "not-a-folder" {:closed identity})))))

(deftest idle-manager-lifecycle-test
  (testing "new-idle-manager starts running and stop stops it cleanly"
    (let [session (mail/get-session "imap")
          ^IdleManager manager (events/new-idle-manager session)]
      (try
        (is (instance? IdleManager manager))
        (is (true? (events/running? manager)))
        (finally
          (events/stop manager)))
      ;; Give the background selector loop a brief moment to terminate
      (loop [attempts 20]
        (when (and (pos? attempts) (events/running? manager))
          (Thread/sleep 25)
          (recur (dec attempts))))
      (is (false? (events/running? manager))))))

(deftest add-message-count-listener-options-test
  (testing "registers connection callbacks when :on-closed/:on-disconnected are provided"
    (let [{:keys [folder conn-listeners msg-listeners open-state]} (dummy-folder true)
          ^Session session (mail/get-session "imap")
          manager (events/new-idle-manager session)
          added-events (atom [])
          closed-events (atom [])]
      (try
        ;; Stub watch via a proxy? IdleManager is not an interface, so test the listener directly
        ;; by verifying invalid non-IMAPFolder watch throws MessagingException after registering listeners.
        (is (thrown? javax.mail.MessagingException
                     (events/add-message-count-listener
                      #(swap! added-events conj %)
                      (constantly nil)
                      folder
                      manager
                      {:on-closed #(swap! closed-events conj %)})))
        (is (= 1 (count @conn-listeners)))
        (is (= 1 (count @msg-listeners)))
        ;; Triggering the registered ConnectionListener fires :on-closed
        (.closed ^ConnectionListener (first @conn-listeners)
                 (ConnectionEvent. folder ConnectionEvent/CLOSED))
        (is (= [:closed] (mapv :type @closed-events)))
        ;; Closing the folder prevents re-watching from throwing inside messagesAdded
        (reset! open-state false)
        (let [msg (MimeMessage. session)
              ev (MessageCountEvent. folder MessageCountEvent/ADDED false (into-array Message [msg]))]
          (.messagesAdded ^javax.mail.event.MessageCountListener (first @msg-listeners) ev)
          (is (= [:added] (mapv :type @added-events))))
        (finally
          (events/stop manager))))))
