(ns clojure-mail.search-test
  (:require [clojure.test :refer :all]
            [clojure-mail.folder :as folder] :reload))

(defprotocol FolderSearch (search [this ^javax.mail.search.SearchTerm st]))
(def search-stub (reify FolderSearch (search [this st] st)))

(defn h-day-of-week
  [dt]
  (let [c (java.util.Calendar/getInstance)]
    (.setTime c dt)
    (.get c java.util.Calendar/DAY_OF_WEEK)))

(deftest search-terms
  (testing "default: string parameter searches within body and subject"
    (let [q (folder/search search-stub "query")]
      (is (= (.getPattern (first (.getTerms q))) "query"))
      (is (= (.getPattern (second (.getTerms q))) "query"))
      (is (= (type (second (.getTerms q))) javax.mail.search.BodyTerm))))

  (doall (map #(testing (str "message part condition " %)
    (let [st (folder/build-search-terms (list % "query"))]
      (is (= (.getPattern st) "query")))) [:body :subject :from]))

  (doall (map #(testing (str "message part condition " %)
    (let [st (folder/build-search-terms (list % "foo@example.com"))]
      (is (= (.getRecipientType st) (folder/to-recipient-type %))) 
      (is (= (.getPattern st) "foo@example.com")))) [:to :cc :bcc]))

  (testing "search value can be an array which is or-red"
    (let [q (folder/search search-stub :body ["foo" "bar"])]
      (is (= (type q) javax.mail.search.OrTerm))
      (is (= (.getPattern (first (.getTerms q))) "foo"))
      (is (= (.getPattern (second (.getTerms q))) "bar"))))

  (testing "date support"
    (doall (map 
      #(let [q (folder/search search-stub :sent-before %)]
         (is (= (type q) javax.mail.search.SentDateTerm))
         (is (= (.. q (getDate) (getYear)) 116)) ; since 1900
         (is (= (.getComparison q) javax.mail.search.ComparisonTerm/LE)))
      ["2016.01.01" "2016-01-01" "2016-01-01 12:00:00" "2016.01.01 12:00"]))

    (doall (map 
      #(let [q (folder/search search-stub :received-on %)]
         (is (= (type q) javax.mail.search.ReceivedDateTerm))
         (is (= (h-day-of-week (.getDate q)) (.get (java.util.Calendar/getInstance) java.util.Calendar/DAY_OF_WEEK))) 
         (is (= (.getComparison q) javax.mail.search.ComparisonTerm/EQ)))
      [:today])) 

    (let [q (folder/search search-stub :received-on :yesterday)
          d (java.util.Calendar/getInstance)]
         (.add d java.util.Calendar/DAY_OF_WEEK -1)
         (is (= (h-day-of-week (.getDate q)) (.get d java.util.Calendar/DAY_OF_WEEK)))))

  (testing "flag support"
    (let [q (folder/search search-stub :answered?)]
      (is (= (.getTestSet q) true))
      (is (= (type q) javax.mail.search.FlagTerm)))
    (let [q (folder/search search-stub :-seen?)]
      (is (= (.getTestSet q) false)))))




