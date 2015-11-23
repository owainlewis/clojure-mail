(ns clojure-mail.imap-integration
  (:require [clojure.test :refer :all]
            [clojure-mail.core :refer :all]
            [clojure-mail.message :as message]
            [clojure-mail.helpers.fixtures :as fxt])
  (:import (javax.mail AuthenticationFailedException)))

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
      (close-store test-store)))
  ;; Test below disabled since authentication failures seem to genearate a runaway thread
  ;; in the IMAP server
  ;; TODO Investigate GreenMail e-mail thread and enable test
  #_(testing "A javax.mail.AuthenticationFailedException is thrown if credentials are not correct."
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
      (is (= (:body target-msg) (->> sut message/message-body :body)) "The retieved message body matches.")
      (is (= (:from target-msg) (->> sut message/from first :address)) "The retrieved message from field matches.")
      (is (= (:to target-msg) (->> sut message/to first :address)) "The retrieved message to field matches.")
      (close-store test-store-2))))

(deftest seen-flag-test
  (let [user-index 1
        port (fxt/get-server-port :imap)
        credentials (fxt/get-credentials-from-config user-config user-index)]
    (testing "A just retrieved message is marked as not read."
      (let [test-store (apply store "imap" ["localhost" port] credentials)
            sut (first (all-messages test-store "inbox"))]
        (is (not (message/read? sut)))
        (close-store test-store)))
    #_(testing "A message that has being retrieved before is marked as read."
      (let [test-store (apply store "imap" ["localhost" port] credentials)
            sut (first (all-messages test-store "inbox"))]
        (is (message/read? sut))
        (close-store test-store)))))
