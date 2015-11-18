(ns clojure-mail.imap-integration
  (:require [clojure.test :refer :all]
            [clojure-mail.core :refer :all]
            [clojure-mail.message :as message])
  (:import [com.icegreen.greenmail.util GreenMail ServerSetupTest]
           [javax.mail Session Message$RecipientType]
           [javax.mail.internet InternetAddress MimeMessage]))

(def ^:dynamic *the-gm*)
(def ^:dynamic *user1*)
(def ^:dynamic *user2*)

(defn setup-gm
  "Global fixture which sets up GreenMail"
  [f]
  (binding [*the-gm* (GreenMail. ServerSetupTest/IMAP)]
    (f)
    (.stop *the-gm*)))

(defn reset-gm
  [f]
  (.reset *the-gm*)
  (binding [*user1* (.setUser *the-gm* "user1@localhost" "user1" "password1")
            *user2* (.setUser *the-gm* "user2@localhost" "user2" "password2")]
    (f)))

(defn get-service-port
  "gets the port GreenMail is listening on for the specific service.
  service-type should be either :imap or :imaps."
  [service-type]
  (case service-type
    :imap (..  *the-gm* (getImap) (getPort))
    :imaps (.. *the-gm* (getImaps) (getPort))))

(defn create-direct-text-message
  "Creates a text message to be delivered
  directly to a GreenMail user without using an MTA"
  [from to subject body]
  (let [sess (Session/getInstance (as-properties {"mail.host" "localhost"})) ; This should not have lateral effects
        msg (doto (MimeMessage. sess)
              (.setFrom (InternetAddress. from))
              (.setRecipient Message$RecipientType/TO (InternetAddress. to))
              (.setSubject subject)
              (.setContent body, "text/plain"))]
    msg))

(use-fixtures :once setup-gm)
(use-fixtures :each reset-gm)

(deftest single-store-test
  (testing "A single store identifies itself as connected."
    (let [port (get-service-port :imap)
          test-store (store "imap" ["localhost" port] "user1" "password1")]
      (is (connected? test-store)))))

(deftest multiple-connection-test
  (testing "Multiple stores running at the same time identify themselves as connected."
    (let [port (get-service-port :imap)
          test-store-1 (store "imap" ["localhost" port] "user1" "password1")
          test-store-2 (store "imap" ["localhost" port] "user2" "password2")]
      (is (and (connected? test-store-1) (connected? test-store-2))))))

(deftest single-email-retrieval
  (testing "We can retrieve an existing message for a user with a single e-mail in its inbox."
    (let [msg-subject "User1 to User2"
          msg (create-direct-text-message "user2@localhost" "user1@localhost" msg-subject "random text.")
          _ (.deliver *user1* msg)
          port (get-service-port :imap)
          test-store-1 (store "imap" ["localhost" port] "user1" "password1")
          all-msgs (all-messages test-store-1 "inbox")
          test-subject (message/subject (first all-msgs))] ; inbox folder is case insensitive
      (is (= 1 (count all-msgs)) "One e-mail in the INBOX folder.")
      (is (= test-subject msg-subject) "Retrieved subject is equal to delivered subject."))))
