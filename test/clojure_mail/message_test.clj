(ns clojure-mail.message-test
  (:require [clojure.test :refer :all]
            [clojure-mail.message :refer :all]
            [clojure-mail.core :refer [file->message]]
            [clojure.java.io :as io]))

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
  (testing "should return the message sender"))

(deftest message-content-type-test
  (testing "should return the message content-type")
  (is (= (mime-type (content-type fixture)) :multipart)))
