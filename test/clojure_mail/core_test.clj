(ns clojure-mail.core-test
  (:require [clojure.test :refer :all]
            [clojure-mail.core :refer :all]))

(deftest auth-test
  (testing "should set authentication information"
    (auth! "user@gmail.com" "password")
    (let [[email password] (vals @settings)]
      (is (= "user@gmail.com" email)
      (is (= "password" password))))))
