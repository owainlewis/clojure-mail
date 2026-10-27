(ns clojure-mail.message-test
  (:require [clojure.test :refer :all]
            [clojure-mail.message :refer :all]
            [clojure-mail.core :refer [file->message]]
            [clojure.java.io :as io])
  (:import [javax.mail Flags Flags$Flag]))

(defn load-fixture [fixture]
  (->> (str "emails/" fixture) io/resource io/file file->message))

(def fixture (load-fixture "25"))

(deftest mime-types-test
  (testing "should parse correct mime-type on email message"
    (let [types {:plain "TEXT/PLAIN; charset=utf-8; format=fixed"
                 :html  "TEXT/HTML; charset=utf-8"}]
      (is (= :plain (mime-type (:plain types))))
      (is (= :html (mime-type (:html types)))))))

(deftest message-to-test
  (testing "should return a sequence of message receievers"
    (is (= (count (to fixture)) 1))
    (is (= (first (to fixture)) {:address "zaphrauk@gmail.com" :name nil}))))

(deftest message-subject-test
  (testing "should return an email message subject"
    (is (= (subject fixture) "Request to share ContractsBuilder"))))

(deftest message-from-test
  (testing "should return the message sender"
    (is (seq (from fixture)))
    (is (every? #(contains? % :address) (from fixture)))))

(deftest message-content-type-test
  (testing "should return the message content-type"
    (is (= (mime-type (content-type fixture)) :multipart))))

(deftest mime-type-test
  (is (= :multipart (mime-type "multipart/mixed; boundary=x")))
  (is (= :plain (mime-type "text/plain ; charset=utf-8")))
  (is (nil? (mime-type "application/pdf"))))

(deftest has-flag-test
  (let [msg (load-fixture "25")]
    (testing "a fresh message has no flags"
      (is (false? (read? msg)))
      (is (false? (flagged? msg))))
    (testing "system flags are matched by name, keyword or Flags$Flag"
      (mark-read msg)
      (is (true? (read? msg)))
      (is (true? (has-flag? msg "seen")))
      (is (true? (has-flag? msg :seen)))
      (is (true? (has-flag? msg Flags$Flag/SEEN))))
    (testing "mark-draft sets DRAFT and mark-flagged sets FLAGGED"
      (mark-draft msg)
      (mark-flagged msg)
      (is (true? (draft? msg)))
      (is (true? (flagged? msg))))
    (testing "user flags are matched by name"
      (.setFlags msg (Flags. "important") true)
      (is (true? (has-flag? msg "important")))
      (is (false? (has-flag? msg "other"))))))

(deftest read-message-test
  (testing "reads all fields by default"
    (let [m (read-message fixture)]
      (is (= "Request to share ContractsBuilder" (:subject m)))
      (is (true? (:multipart? m)))
      (is (vector? (:headers m)))
      (is (nil? (:errors m)))))
  (testing "only the requested fields are returned"
    (is (= #{:id :subject} (set (keys (read-message fixture :fields [:id :subject]))))))
  (testing "multipart bodies are read as a sequence of parts"
    (let [body (message-body fixture)]
      (is (seq body))
      (is (every? :content-type (flatten body))))))
