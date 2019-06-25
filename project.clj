(defproject io.forward/clojure-mail "1.0.8"
  :description "Clojure Email Library"
  :url "https://github.com/forward/clojure-mail"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.jsoup/jsoup "1.8.3"] ;; for cleaning up messy html messages
                 [com.sun.mail/javax.mail "1.5.5"]
                 [medley "1.0.0"]]
  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :creds :gpg}]]
  :plugins [[lein-cljfmt "0.3.0"]]
  :profiles {:dev {:dependencies [[com.icegreen/greenmail "1.4.1"]]}})
