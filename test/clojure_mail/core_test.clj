(ns clojure-mail.core-test
  (:require [clojure.test :refer :all]
            [clojure-mail.core :refer :all]))

(deftest server->host-port-test
  (testing "non sequential server returns default ports"
    (are [protocol server port] (= [server port] (server->host-port protocol server))
                                "imap" "localhost" 143
                                "imaps" "localhost" 943
                                :imap "localhost" 143
                                :imaps "localhost" 943))
  (testing "sequential, one element server returns default ports"
    (are [protocol server port] (= [server port] (server->host-port protocol [server]))
                                "imap" "localhost" 143
                                "imaps" "localhost" 943
                                :imap "localhost" 143
                                :imaps "localhost" 943))
  (testing "sequential, two element server returns non-default ports"
    (are [protocol server port] (= [server port] (server->host-port protocol [server port]))
                                "imap" "localhost" 1234
                                "imaps" "localhost" 1234
                                :imap "localhost" 1234
                                :imaps "localhost" 1234)))
