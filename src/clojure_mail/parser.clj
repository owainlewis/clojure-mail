(ns clojure-mail.parser)

;; Utils for parsing data from emails

;; Test data to be removed. Only for dev purposes

(defonce fixture-path "/Users/owainlewis/Projects/clojure/clojure-mail/test/clojure_mail/fixtures")

(defn read-fixture
  "reads in a fixture file from the test directory"
  [f-name]
  (slurp (format "%s/%s" fixture-path f-name)))
