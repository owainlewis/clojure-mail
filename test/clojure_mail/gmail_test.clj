(ns clojure-mail.gmail-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure-mail.core :as mail]
            [clojure-mail.gmail :as gmail])
  (:import [javax.mail Session]))

(deftest folder->folder-name-test
  (testing "resolves known Gmail folder keywords"
    (is (= "INBOX" (gmail/folder->folder-name :inbox)))
    (is (= "[Gmail]/All Mail" (gmail/folder->folder-name :all)))
    (is (= "[Gmail]/Drafts" (gmail/folder->folder-name :drafts)))
    (is (= "[Gmail]/Important" (gmail/folder->folder-name :important)))
    (is (= "[Gmail]/Sent Mail" (gmail/folder->folder-name :sent)))
    (is (= "[Gmail]/Spam" (gmail/folder->folder-name :spam)))
    (is (= "[Gmail]/Starred" (gmail/folder->folder-name :starred)))
    (is (= "[Gmail]/Trash" (gmail/folder->folder-name :trash))))
  (testing "passes through string folder names unchanged"
    (is (= "INBOX" (gmail/folder->folder-name "INBOX")))
    (is (= "Work/Receipts" (gmail/folder->folder-name "Work/Receipts"))))
  (testing "passes through unknown keywords unchanged"
    (is (= :custom (gmail/folder->folder-name :custom)))))

(deftest get-session-test
  (testing "returns a Session configured for imaps"
    (let [^Session session (gmail/get-session)]
      (is (instance? Session session))
      (is (= "imaps" (.getProperty session "mail.store.protocol")))
      (is (= "true" (.getProperty session "mail.imaps.usesocketchannels"))))))

(deftest open-folder-delegation-test
  (testing "translates Gmail keywords before calling clojure-mail.core/open-folder"
    (let [calls (atom [])]
      (with-redefs [mail/open-folder (fn [& args] (swap! calls conj args) :opened)]
        (is (= :opened (gmail/open-folder :sent :readonly)))
        (is (= :opened (gmail/open-folder :fake-store :spam :readwrite)))
        (is (= :opened (gmail/open-folder :fake-store "CustomLabel" :readonly)))
        (is (= [["[Gmail]/Sent Mail" :readonly]
                [:fake-store "[Gmail]/Spam" :readwrite]
                [:fake-store "CustomLabel" :readonly]]
               @calls))))))
