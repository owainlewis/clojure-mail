(ns clojure-mail.parser
  (:import [org.jsoup.nodes Document Element]))

(defn parse [html]
  (org.jsoup.Jsoup/parse html "UTF-8"))

(defn html->text
  "Given email message HTML return only the actual text"
  [html]
  (.text (parse html)))
