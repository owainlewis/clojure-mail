(defproject io.forward/clojure-mail "1.1.0-SNAPSHOT"
  :description "A Clojure library for parsing, downloading and reading email from IMAP servers"
  :url "https://github.com/owainlewis/clojure-mail"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git"
        :url "https://github.com/owainlewis/clojure-mail"}
  :min-lein-version "2.9.0"
  :dependencies [[org.clojure/clojure "1.11.4"]
                 [org.jsoup/jsoup "1.21.2"] ;; for cleaning up messy html messages
                 [com.sun.mail/jakarta.mail "1.6.7"]]
  :global-vars {*warn-on-reflection* true}
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]]
  :plugins [[lein-cljfmt "0.9.2"]]
  :profiles {:dev {:dependencies [[com.icegreen/greenmail "1.6.15"]
                                  [org.slf4j/slf4j-nop "1.7.36"]]}
             :clojure-1.12 {:dependencies [[org.clojure/clojure "1.12.3"]]}}
  :aliases {"ci" ["do" "check," "test," "cljfmt" "check"]})
