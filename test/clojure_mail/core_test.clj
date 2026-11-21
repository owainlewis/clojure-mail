(ns clojure-mail.core-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [are deftest is testing]]
            [clojure-mail.core :refer [*store* as-properties dump file->message get-session
                                       save-message-to-file server->host-port with-store]]
            [clojure-mail.message :as message])
  (:import [java.io File]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.util Properties]
           [javax.mail Session]
           [javax.mail.internet MimeMessage]))

(deftest server->host-port-test
  (testing "non sequential server returns default ports"
    (are [protocol server port] (= [server port] (server->host-port protocol server))
      "imap" "localhost" 143
      "imaps" "localhost" 993
      :imap "localhost" 143
      :imaps "localhost" 993
      42 "localhost" 42
      0 "localhost" 993
      -1 "localhost" 993
      65536 "localhost" 993
      "other" "localhost" 993))
  (testing "sequential, one element server returns default ports"
    (are [protocol server port] (= [server port] (server->host-port protocol [server]))
      "imap" "localhost" 143
      "imaps" "localhost" 993
      :imap "localhost" 143
      :imaps "localhost" 993
      42 "localhost" 42
      65536 "localhost" 993
      "other" "localhost" 993))
  (testing "sequential, two element server returns non-default ports"
    (are [protocol server port] (= [server port] (server->host-port protocol [server port]))
      "imap" "localhost" 1234
      "imaps" "localhost" 1234
      :imap "localhost" 1234
      :imaps "localhost" 1234
      42 "localhost" 42
      65536 "localhost" 993
      "other" "localhost" 993))
  (testing "empty sequential server throws IllegalArgumentException"
    (is (thrown? IllegalArgumentException (server->host-port "imaps" [])))))

(deftest as-properties-test
  (let [^Properties props (as-properties {"mail.store.protocol" "imaps"
                                          "mail.imaps.port" 993})]
    (is (instance? Properties props))
    (is (= "imaps" (.getProperty props "mail.store.protocol")))
    (is (= "993" (.getProperty props "mail.imaps.port")))))

(deftest get-session-test
  (let [^Session s (get-session "imap")]
    (is (instance? Session s))
    (is (= "imap" (.getProperty s "mail.store.protocol")))
    (is (= "true" (.getProperty s "mail.imap.usesocketchannels")))))

(deftest with-store-test
  (is (nil? *store*))
  (is (= :my-store (with-store :my-store *store*)))
  (is (nil? *store*)))

(deftest save-and-load-message-file-test
  (let [orig (->> "emails/25" io/resource io/file file->message)
        tmp-dir (.toFile (Files/createTempDirectory "clojure-mail-core-test"
                                                    (into-array FileAttribute [])))
        dest (io/file tmp-dir "saved.eml")]
    (try
      (let [returned (save-message-to-file orig dest)
            reloaded (file->message returned)]
        (is (= (.getPath dest) returned))
        (is (instance? MimeMessage reloaded))
        (is (= (message/subject orig) (message/subject reloaded)))
        (is (= (message/id orig) (message/id reloaded))))
      (finally
        (when (.exists dest) (.delete dest))
        (.delete ^File tmp-dir)))))

(deftest dump-test
  (testing "dump realizes all futures and returns saved filenames"
    (let [saved (atom [])]
      (with-redefs [save-message-to-file (fn [m]
                                           (swap! saved conj m)
                                           (str "file-" m))]
        (let [res (dump [:a :b :c])]
          (is (= ["file-:a" "file-:b" "file-:c"] (vec res)))
          (is (= #{:a :b :c} (set @saved))))))))
