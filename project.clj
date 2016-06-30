(defproject io.forward/clojure-mail "1.0.5"
  :description "Clojure Email Library"
  :url "https://github.com/forward/clojure-mail"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.jsoup/jsoup "1.8.3"] ;; for cleaning up messy html messages
                 [com.sun.mail/javax.mail "1.5.4"]
                 [medley "0.7.0"]]
  :plugins [[lein-cljfmt "0.3.0"]]
  :profiles {:dev {:dependencies [[com.icegreen/greenmail "1.4.1"]]}})
