# Clojure-mail

![](https://travis-ci.org/owainlewis/clojure-mail.svg?branch=master)

[![Clojars Project](http://clojars.org/io.forward/clojure-mail/latest-version.svg#)](http://clojars.org/io.forward/clojure-mail)

A clojure library for parsing, downloading and reading email from IMAP servers.

## Quickstart

This is a complete example showing how to read the subject of your latest Gmail inbox message

```clojure
(ns myproject.core
  (:require [clojure-mail.core :refer :all]
            [clojure-mail.gmail :as gmail]
            [clojure-mail.message :refer (read-message)]))

(def gstore (gmail/store "user@gmail.com" "password"))

(def inbox-messages (inbox gstore))

;; to convert a javamail message into a clojure message we need to call read-message

(def latest (read-message (first inbox-messages)))

;; Let's read the subject of our latest inbox message
(:subject latest)

(keys latest)
;; => (:id :to :cc :bcc :from :sender :subject :date-sent :date-recieved :multipart? :content-type :body :headers)

```

## Use

We need to require clojure-mail.core before we begin.

```clojure
(:require [clojure-mail.core :refer :all]
          [clojure-mail.message :as message])
```

The first thing we need is a mail store which acts as a gateway to our IMAP account.

```clojure
(def store (store "imap.gmail.com" "user@gmail.com" "password"))
```

You can also authenticate using an Oauth token.

```clojure
(def store (xoauth2-store "imap.gmail.com" "user@gmail.com" "user-oauth-token"))
```

Now we can fetch email messages easily.

```clojure
(def my-inbox-messages (take 5 (all-messages store "inbox")))

(def first-message (first my-inbox-messages))

(message/subject first-message) ;; => "Hi! Here are your new links from the weekend"
```

Note that the messages returned are Java mail message objects.


## Reading email messages

```clojure

(def javamail-message (first inbox-messages))

;; To read the entire message as a clojure map
(def message (read-message javamail-message))

;; There are also individual methods available in the message namespace. I.e to read the subject
;; of a javax.mail message
(message/subject javamail-message)

;; You can also select only the fields you require
(def message (read-message javamail-message :fields [:id :to :subject]))

```

An email message returned as a Clojure map from read-message looks something like this:

```clojure

{:subject "Re: Presents for Dale's baby",
 :from {:address "<someone@aol.com>" :name "Someone"}
 :date-recieved "Tue Mar 11 12:54:41 GMT 2014",
 :to ({:address "owain@owainlewis.com" :name "Owain Lewis"}),
 :cc (),
 :bcc (),
 :multipart? true,
 :content-type "multipart/ALTERNATIVE",
 :sender {:address "<someone@aol.com>" :name "Someone"},
 :date-sent #inst "2015-10-23T12:19:33.838-00:00"
 :date-received #inst "2015-10-23T12:19:33.838-00:00"
 :body [{:content-type "text/plain" :body "..."}
        {:content-type "text/html"  :body "..."}]
 :headers {"Subject" "Re: Presents for Dale's baby" .......}

```

## Searching your inbox

You can easily search your inbox for messages

```clojure
(def s (gen-store "user@gmail.com" "password"))
(def results (search-inbox s "projects"))
(def results (search-inbox s [:body "projects" :subject "projects"]))
(def results (search-inbox s :body "projects" :received-before :yesterday))
(def results (search-inbox s :body "projects" :from "john@example.com"))

(->> results first subject) ;; => "Open Source Customisation Projects"
```

## Parser

HTML emails are evil. There is a simple HTML -> Plain text parser provided if you need to
do any machine learning type processing on email messages.

```clojure
(require '[clojure-mail.parser :refer :all])

(html->text "<h1>I HATE HTML EMAILS</h1>")

;; => "I HATE HTML EMAILS"

```

## Watching a folder

Some IMAP servers allow the use of the IDLE command to receive push notifications when a folder changes.

```clojure
(require '[clojure-mail.events :as events])

;; Create a manager and start listening to the inbox, printing the subject of new messages
(def manager
  (let [s (get-session "imaps")
        gstore (store "imaps" s "imap.gmail.com" "me@gmail.com" "mypassword")
        folder (open-folder gstore "inbox" :readonly)
        im (events/new-idle-manager s)]
    (add-message-count-listener (fn [e]
                                  (prn "added" (->> e
                                                    :messages
                                                    (map read-message)
                                                    (map :subject))))
                                #(prn "removed" %)
                                folder
                                im)
    im))
;; now we wait...

"added" ("added" ("test!")
"added" ("added" ("another test!")

;; we received some messages and printed them, now we can stop the manager as we are finished
(events/stop manager)

```

## Reading emails from disk

Clojure mail can be used to parse existing email messages from file. Take a look in dev-resources/emails to see some example messages. To read one of these messages we can do something like this


```clojure

(def message (file->message "test/clojure_mail/fixtures/25"))

(read-message message)

;; =>
;; {:subject "Request to share ContractsBuilder",
;; :from nil, :date-recieved nil,
;; :to "zaphrauk@gmail.com",
;; :multipart? true,
;; :content-type "multipart/alternative; boundary=90e6ba1efefc44ffe804a5e76c56",
;; :sender nil,
;; :date-sent "Fri Jun 17 13:21:19 BST 2011" ..............

```

## License

Copyright © 2017 Owain Lewis

Distributed under the Eclipse Public License, the same as Clojure.
