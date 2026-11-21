(ns clojure-mail.search-test
  (:require [clojure.test :refer [are deftest is testing]]
            [clojure-mail.folder :as folder])
  (:import [java.time Instant LocalDate]
           [java.util Calendar Date]
           [javax.mail.search AndTerm BodyTerm ComparisonTerm DateTerm FlagTerm
            HeaderTerm OrTerm ReceivedDateTerm RecipientStringTerm SearchTerm
            SentDateTerm StringTerm]))

(defprotocol FolderSearch
  (search [this ^SearchTerm st]))

(def search-stub
  (reify FolderSearch
    (search [_ st] st)))

(defn- h-cal-field
  [^Date dt ^long field]
  (let [c (Calendar/getInstance)]
    (.setTime c dt)
    (.get c (int field))))

(deftest search-terms
  (testing "default: string parameter searches within body and subject"
    (let [^OrTerm q (folder/search search-stub "query")
          terms (.getTerms q)]
      (is (= (.getPattern ^StringTerm (first terms)) "query"))
      (is (= (.getPattern ^StringTerm (second terms)) "query"))
      (is (= (type (second terms)) BodyTerm))))

  (doseq [k [:body :subject :from]]
    (testing (str "message part condition " k)
      (let [^StringTerm st (folder/build-search-terms (list k "query"))]
        (is (= (.getPattern st) "query")))))

  (doseq [k [:to :cc :bcc]]
    (testing (str "message part condition " k)
      (let [^RecipientStringTerm st (folder/build-search-terms (list k "foo@example.com"))]
        (is (= (.getRecipientType st) (folder/to-recipient-type k)))
        (is (= (.getPattern st) "foo@example.com"))))
    (testing (str "recipient vector is OR-ed for " k)
      (let [^OrTerm q (folder/search search-stub k ["a@example.com" "b@example.com"])
            terms (.getTerms q)]
        (is (= OrTerm (type q)))
        (is (= "a@example.com" (.getPattern ^RecipientStringTerm (first terms))))
        (is (= "b@example.com" (.getPattern ^RecipientStringTerm (second terms))))
        (is (= (folder/to-recipient-type k) (.getRecipientType ^RecipientStringTerm (first terms)))))))

  (testing "search value can be a vector which is OR-ed"
    (let [^OrTerm q (folder/search search-stub :body ["foo" "bar"])
          terms (.getTerms q)]
      (is (= (type q) OrTerm))
      (is (= (.getPattern ^StringTerm (first terms)) "foo"))
      (is (= (.getPattern ^StringTerm (second terms)) "bar"))))

  (testing "single-element vector unwraps to single term and empty vector throws"
    (let [^StringTerm q (folder/search search-stub :subject ["only"])]
      (is (= "only" (.getPattern q))))
    (is (thrown? IllegalArgumentException
                 (folder/build-search-terms [:subject []]))))

  (testing "date support"
    (doseq [ds ["2016.01.01" "2016-01-01" "2016-01-01 12:00:00" "2016.01.01 12:00"]]
      (let [^DateTerm q (folder/search search-stub :sent-before ds)]
        (is (= (type q) SentDateTerm))
        (is (= 2016 (h-cal-field (.getDate q) Calendar/YEAR)))
        (is (= (.getComparison q) ComparisonTerm/LE))))

    (let [^DateTerm q-after (folder/search search-stub :sent-after (LocalDate/of 2026 5 10))
          ^DateTerm q-on (folder/search search-stub :sent-on (Date/from (Instant/parse "2026-06-15T00:00:00Z")))
          ^DateTerm q-inst (folder/search search-stub :received-before (Instant/parse "2026-07-20T00:00:00Z"))]
      (is (= ComparisonTerm/GE (.getComparison q-after)))
      (is (= 2026 (h-cal-field (.getDate q-after) Calendar/YEAR)))
      (is (= ComparisonTerm/EQ (.getComparison q-on)))
      (is (= ComparisonTerm/LE (.getComparison q-inst)))
      (is (= ReceivedDateTerm (type q-inst))))

    (is (thrown? IllegalArgumentException
                 (folder/build-search-terms [:received-after "not-a-date"])))

    (let [^DateTerm q (folder/search search-stub :received-on :today)]
      (is (= (type q) ReceivedDateTerm))
      (is (= (h-cal-field (.getDate q) Calendar/DAY_OF_WEEK)
             (.get (Calendar/getInstance) Calendar/DAY_OF_WEEK)))
      (is (= (.getComparison q) ComparisonTerm/EQ)))

    (let [^DateTerm q (folder/search search-stub :received-on :yesterday)
          d (Calendar/getInstance)]
      (.add d Calendar/DAY_OF_MONTH -1)
      (is (= (h-cal-field (.getDate q) Calendar/DAY_OF_WEEK)
             (.get d Calendar/DAY_OF_WEEK)))))

  (testing "multiple criteria"
    (let [^AndTerm q (folder/search search-stub :body "foo" :received-on :today)
          terms (.getTerms q)]
      (is (= (type q) AndTerm))
      (is (= (.getPattern ^StringTerm (first terms)) "foo"))
      (is (= (type (second terms)) ReceivedDateTerm))))

  (testing "multiple criteria, vec is OR-ed, empty nested vec is safely ignored"
    (let [^OrTerm q (folder/search search-stub [:body "foo" :received-on :today])
          terms (.getTerms q)]
      (is (= (type q) OrTerm))
      (is (= (.getPattern ^StringTerm (first terms)) "foo"))
      (is (= (type (second terms)) ReceivedDateTerm)))
    (let [^StringTerm q (folder/search search-stub [] :subject "hello")]
      (is (= "hello" (.getPattern q)))))

  (testing "header support"
    (let [q (folder/search search-stub :header "name" "value")]
      (is (= (type q) HeaderTerm)))
    (let [^OrTerm q (folder/search search-stub :header ["name1" "value1" "name2" "value2"])
          terms (.getTerms q)]
      (is (= (type q) OrTerm))
      (is (= (type (first terms)) HeaderTerm)))
    (is (thrown? IllegalArgumentException
                 (folder/build-search-terms [:header []]))))

  (testing "flag support"
    (are [flag set?]
         (let [^FlagTerm q (folder/search search-stub flag)]
           (is (= set? (.getTestSet q)))
           (is (= FlagTerm (type q))))
      :answered? true :-answered? false
      :deleted?  true :-deleted?  false
      :flagged?  true :-flagged?  false
      :draft?    true :-draft?    false
      :recent?   true :-recent?   false
      :seen?     true :-seen?     false)))




