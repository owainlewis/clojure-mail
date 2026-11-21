(ns clojure-mail.imap-integration
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure-mail.core :refer [all-messages close-folder close-store connected?
                                       folders get-default-folder get-folder
                                       get-folder-uid-validity inbox mark-all-read
                                       message-count open-folder search-inbox store
                                       sub-folder? unread-messages user-flags with-folder
                                       with-store]]
            [clojure-mail.events :as events]
            [clojure-mail.folder :as folder]
            [clojure-mail.helpers.fixtures :as fxt]
            [clojure-mail.message :as message])
  (:import [javax.mail AuthenticationFailedException Flags Folder]))

(use-fixtures :once (fxt/make-gm-fixture :imap))

(def ^:const user-config [{:login    "user1" :pass "password1" :email "user1@localhost"
                           :messages [{:from    "user2@localhost"
                                       :subject "From user2 to user1"
                                       :body    "Random text."}
                                      {:from    "user2@localhost"
                                       :subject "Another e-mail from user2"
                                       :body    "More random text."}]}
                          {:login    "user2" :pass "password2" :email "user2@localhost"
                           :messages [{:from    "user1@localhost"
                                       :subject "From user1 to user2"
                                       :body    "Random text."}]}
                          {:login    "user3" :pass "password3" :email "user3@localhost"
                           :messages []}])

(use-fixtures :each (fxt/make-custom-fixture-from-config user-config))

(deftest single-store-test
  (testing "A store identifies itself as connected after login."
    (let [port (fxt/get-server-port :imap)
          test-store (store "imap" ["localhost" port] "user1" "password1")]
      (is (connected? test-store))
      (close-store test-store)
      (is (not (connected? test-store)))))
  (testing "A javax.mail.AuthenticationFailedException is thrown if credentials are not correct."
    (let [port (fxt/get-server-port :imap)]
      (is (thrown? AuthenticationFailedException
                   (store "imap" ["localhost" port] "user1" "bad-password"))))))

(deftest multiple-store-test
  (testing "Multiple stores running at the same time identify themselves as connected."
    (let [port (fxt/get-server-port :imap)
          test-store-1 (store "imap" ["localhost" port] "user1" "password1")
          test-store-2 (store "imap" ["localhost" port] "user2" "password2")]
      (is (and (connected? test-store-1) (connected? test-store-2)))
      (close-store test-store-1)
      (close-store test-store-2))))

(deftest store-message-count-test
  (let [server "localhost"
        port (fxt/get-server-port :imap)]
    (testing "Message count for INBOX in an empty store"
      (let [sut (store "imap" [server port] "user3" "password3")]
        (is (= 0 (message-count sut "inbox")))
        (close-store sut)))
    (testing "Message count for INBOX in a store with an unread message"
      (let [sut (store "imap" [server port] "user2" "password2")]
        (is (= 1 (message-count sut "inbox")))
        (close-store sut)))
    (testing "Message count for INBOX in a store with two unread messages"
      (let [sut (store "imap" [server port] "user1" "password1")]
        (is (= 2 (message-count sut "inbox")))
        (close-store sut)))))

(deftest single-email-retrieval-test
  (testing "We can retrieve an existing message for a user with a single e-mail in its inbox."
    (let [user-index 1
          email-index 0
          target-msg (fxt/get-message-map-from-config user-config user-index email-index)
          port (fxt/get-server-port :imap)
          credentials (fxt/get-credentials-from-config user-config user-index)
          test-store-2 (apply store "imap" ["localhost" port] credentials)
          all-msgs (all-messages test-store-2 "inbox")
          sut (first all-msgs)]
      (is (= (:subject target-msg) (message/subject sut)) "The retrieved message subject matches.")
      (is (= (:body target-msg) (->> sut message/message-body :body)) "The retrieved message body matches.")
      (is (= (:from target-msg) (->> sut message/from first :address)) "The retrieved message from field matches.")
      (is (= (:to target-msg) (->> sut message/to first :address)) "The retrieved message to field matches.")
      (close-store test-store-2))))

(deftest inbox-and-since-uid-test
  (let [port (fxt/get-server-port :imap)
        test-store (store "imap" ["localhost" port] "user1" "password1")]
    (try
      (let [msgs (inbox test-store)
            uids (mapv message/uid msgs)]
        (is (= 2 (count msgs)))
        (is (= ["Another e-mail from user2" "From user2 to user1"]
               (mapv message/subject msgs)))
        (is (every? pos? uids))
        (let [newer (all-messages test-store "inbox" :since-uid (first uids))]
          (is (= ["Another e-mail from user2"]
                 (mapv message/subject newer)))))
      (finally
        (close-store test-store)))))

(deftest seen-flag-and-mark-all-read-test
  (let [port (fxt/get-server-port :imap)
        test-store (store "imap" ["localhost" port] "user1" "password1")]
    (try
      (testing "unread-messages returns both unread messages"
        (is (= 2 (count (unread-messages test-store "inbox")))))
      (testing "mark-all-read marks all messages seen and closes the folder"
        (mark-all-read test-store "inbox")
        (is (not (.isOpen ^Folder (get-folder test-store "inbox"))))
        (is (= 0 (count (unread-messages test-store "inbox")))))
      (testing "individual message flags and user flags work over IMAP"
        (with-folder [f test-store "inbox" :readwrite]
          (let [msg (first (folder/get-messages f))]
            (is (message/read? msg))
            (.setFlags ^javax.mail.Message msg (Flags. "custom-label") true)
            (is (message/has-flag? msg "custom-label"))
            (is (some #{"custom-label"} (user-flags msg))))))
      (finally
        (close-store test-store)))))

(deftest message-count-and-with-folder-close-test
  (let [port (fxt/get-server-port :imap)
        test-store (store "imap" ["localhost" port] "user1" "password1")]
    (try
      (testing "message-count does not leave the folder open"
        (is (= 2 (message-count test-store "inbox")))
        (is (not (.isOpen ^Folder (get-folder test-store "inbox")))))
      (testing "core/with-folder opens and closes folder, even on exception"
        (let [captured (atom nil)]
          (is (= 2 (with-folder [f test-store "inbox"]
                     (reset! captured f)
                     (is (.isOpen ^Folder f))
                     (folder/message-count f))))
          (is (not (.isOpen ^Folder @captured))))
        (let [captured (atom nil)]
          (is (thrown? RuntimeException
                       (with-folder [f test-store "inbox" :readonly]
                         (reset! captured f)
                         (throw (RuntimeException. "boom")))))
          (is (not (.isOpen ^Folder @captured))))
        (testing "core/with-folder works with *store*"
          (with-store test-store
            (is (= 2 (with-folder [f "inbox"]
                       (folder/message-count f))))
            (is (= 2 (with-folder [f "inbox" :readonly]
                       (folder/message-count f)))))))
      (testing "folder/with-folder binds current-folder and closes folder on exit"
        (let [captured (atom nil)]
          (is (= 2 (folder/with-folder "inbox" test-store
                     (reset! captured folder/current-folder)
                     (is (.isOpen ^Folder folder/current-folder))
                     (folder/message-count folder/current-folder))))
          (is (not (.isOpen ^Folder @captured)))))
      (testing "open-folder rejects invalid permission level with clear exception"
        (is (thrown? IllegalArgumentException
                     (open-folder test-store "inbox" :invalid-mode))))
      (finally
        (close-store test-store)))))

(deftest folder-operations-test
  (let [port (fxt/get-server-port :imap)
        test-store (store "imap" ["localhost" port] "user1" "password1")]
    (try
      (testing "folder listing and metadata"
        (let [default-f (get-default-folder test-store)]
          (is (sub-folder? default-f))
          (is (seq (folder/list default-f)))
          (is (some #{"INBOX"} (flatten (folders test-store))))))
      (testing "folder accessors and fetch profiles"
        (with-folder [f test-store "INBOX" :readonly]
          (is (= "INBOX" (folder/full-name f)))
          (is (pos? (get-folder-uid-validity f)))
          (is (= 2 (folder/message-count f)))
          (is (= 2 (folder/unread-message-count f)))
          (is (integer? (folder/new-message-count f)))
          (let [m1 (folder/get-message f 1)
                uid1 (message/uid m1)
                range-msgs (folder/get-messages f 1 1)]
            (is (= "From user2 to user1" (message/subject m1)))
            (is (= (message/subject m1) (message/subject (folder/get-message-by-uid f uid1))))
            (is (= 1 (count range-msgs)))
            (doseq [profile [:message :headers :flags :envelope :content-info :size]]
              (folder/fetch-messages f range-msgs :fetch-profile profile)))))
      (finally
        (close-store test-store)))))

(deftest search-inbox-test
  (let [port (fxt/get-server-port :imap)
        test-store (store "imap" ["localhost" port] "user1" "password1")]
    (try
      (testing "search-inbox finds messages by subject"
        (is (= ["Another e-mail from user2"]
               (mapv message/subject (search-inbox test-store :subject "Another")))))
      (testing "search-inbox supports single string, multi-condition and OR vectors"
        (is (= 2 (count (search-inbox test-store "random text"))))
        (is (= ["Another e-mail from user2"]
               (mapv message/subject
                     (search-inbox test-store :from "user2@localhost" :body "More random" :-seen?))))
        (is (= 2 (count (search-inbox test-store :subject ["From user2" "Another"])))))
      (finally
        (close-store test-store)))))

(deftest connection-listener-integration-test
  (let [port (fxt/get-server-port :imap)
        test-store (store "imap" ["localhost" port] "user1" "password1")
        closed-promise (promise)]
    (try
      (let [f (open-folder test-store "inbox" :readonly)]
        (events/add-connection-listener f {:closed #(deliver closed-promise %)})
        (close-folder f)
        (let [evt (deref closed-promise 2000 :timeout)]
          (is (not= :timeout evt))
          (is (= :closed (:type evt)))
          (is (= f (:source evt)))))
      (finally
        (close-store test-store)))))
