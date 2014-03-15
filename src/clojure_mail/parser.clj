(ns clojure-mail.parser
  (:import [org.jsoup.nodes Document Element]))

(defn parse [html]
  (org.jsoup.Jsoup/parse html "UTF-8"))

(defn html->text
  "Given email message HTML return only the actual text"
  [html]
  (.text (parse html)))

(defn clean-non-utf8
  "All kinds of junk gets sent in emails so forcibly remove
   anything that isn't utf8"
  ([text]
    (clean-non-utf8 text ""))
  ([text replacement]
    (.replaceAll text "[^\\p{L}\\p{Nd}]" replacement)))
