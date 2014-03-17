(defproject clojure-mail "0.1.6"
  :description "Clojure Email Library"
  :url "https://github.com/forward/clojure-mail"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.jsoup/jsoup "1.7.3"] ;; for cleaning up messy html messages
                 [javax.mail/mail "1.4.4"]]
  :plugins [[lein-swank "1.4.4"]])
