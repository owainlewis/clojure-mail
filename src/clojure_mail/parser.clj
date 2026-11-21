(ns clojure-mail.parser
  (:import [org.jsoup Jsoup]
           [org.jsoup.nodes Document]))

(defn parse
  "Parse an HTML string into a Jsoup Document."
  ^Document [^String html]
  (Jsoup/parse html))

(defn html->text
  "Given email message HTML return only the actual text"
  [^String html]
  (.text (parse html)))

(defn clean-non-utf8
  "All kinds of junk gets sent in emails so forcibly remove
   anything that isn't utf8"
  ([^String text]
   (clean-non-utf8 text ""))
  ([^String text ^String replacement]
   (.replaceAll text "[^\\p{L}\\p{Nd}]" replacement)))
