(ns clojure-mail.core-test
  (:require [clojure.test :refer :all]
            [clojure-mail.core :refer :all]))

;; Load a fixture from disk and return a MimeMessage

(defn load-fixture [fixture]
  (file->message
    (str "test/clojure_mail/fixtures/" fixture)))

(def fixture (load-fixture "25"))

(deftest auth-test
  (testing "should set authentication information"
    (auth! "user@gmail.com" "password")
    (let [[email password] (vals @settings)]
      (is (= "user@gmail.com" email)
      (is (= "password" password))))))

(deftest mime-types-test
  (testing "should parse correct mime-type on email message"
    (let [types {:plain "TEXT/PLAIN; charset=utf-8; format=fixed"
                 :html  "TEXT/HTML; charset=utf-8"}]
      (is (= :plain (mime-type (:plain types))))
      (is (= :html (mime-type (:html types)))))))

(deftest message-to-test
  (testing "should return a sequence of message receievers"
    (is (= (count (to fixture)) 1))
    (is (= (first (to fixture)) "zaphrauk@gmail.com"))))

(deftest message-subject-test
  (testing "should return an email message subject"
    (is (= (subject fixture) "Request to share ContractsBuilder"))))

(deftest message-from-test
  (testing "should return the message sender"))

(deftest message-content-type-test
  (testing "should return the message content-type")
    (is (= (mime-type (content-type fixture)) :multipart)))
