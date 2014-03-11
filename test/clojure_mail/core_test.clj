(ns clojure-mail.core-test
  (:use clojure.test
        clojure-mail.core))

(deftest auth-test
  (testing "should set authentication information"
    (auth! "user@gmail.com" "password")
    (let [[email password] (vals @settings)]
      (is (= "user@gmail.com" email)
      (is (= "password" password))))))
