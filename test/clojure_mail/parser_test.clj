(ns clojure-mail.parser-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure-mail.parser :refer [clean-non-utf8 html->text parse]])
  (:import [org.jsoup.nodes Document]))

(deftest parse-test
  (testing "returns a Jsoup Document with empty baseUri"
    (let [doc (parse "<html><head><title>Hi</title></head><body><p>Body</p></body></html>")]
      (is (instance? Document doc))
      (is (= "Hi" (.title doc)))
      (is (= "" (.baseUri doc))))))

(deftest html->text-test
  (testing "converts HTML to plain text"
    (is (= "foo" (html->text "<h1><span>foo</span></h1>")))
    (is (= "Hello there" (html->text "<h1>Hello <b>there</b></h1>")))
    (is (= "a & b < c" (html->text "<p>a &amp; b &lt; c</p>")))
    (is (= "" (html->text "")))))

(deftest clean-non-utf8-test
  (testing "strips non-letter/non-digit characters by default"
    (is (= "Hello123" (clean-non-utf8 "Hello, 123!!")))
    (is (= "café" (clean-non-utf8 "café!"))))
  (testing "replaces non-letter/non-digit characters with custom replacement"
    (is (= "Hello-world" (clean-non-utf8 "Hello world" "-")))
    (is (= "a_b_c" (clean-non-utf8 "a!b?c" "_")))))
