(ns clojure-mail.search-test
  (:require [clojure.test :refer :all]
            [clojure-mail.folder :as folder] :reload))

(defprotocol FolderSearch (search [this ^javax.mail.search.SearchTerm st]))

(deftest search-terms
  (testing "default: string parameter searches within body and subject"
    (let [q (folder/search (reify FolderSearch (search [this st] st)) "query")]
      (is (= (.getPattern (first (.getTerms q))) "query"))
      (is (= (.getPattern (second (.getTerms q))) "query"))
      (is (= (type (second (.getTerms q))) javax.mail.search.BodyTerm))))

  (doall (map #(testing (str "message part condition " %)
    (let [st (folder/build-search-terms % "query")]
      (is (= (.getPattern st) "query")))) [:body :subject :from]))

  (doall (map #(testing (str "message part condition " %)
    (let [st (folder/build-search-terms % "foo@example.com")]
      (is (= (.getRecipientType st) (folder/to-recipient-type %))) 
      (is (= (.getPattern st) "foo@example.com")))) [:to :cc :bcc])))
