(ns clojure-mail.parser-test
  (:require [clojure.test :refer :all]
            [clojure-mail.parser :refer :all]))

(deftest html->text-test
  (testing "should convert HTML to plain text"
    (is (= "foo" (html->text "<h1><span>foo</span></h1>")))))
