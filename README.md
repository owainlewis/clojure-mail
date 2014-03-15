# Clojure-mail


```
[clojure-mail "0.1.5"]
```

A clojure library for parsing, downloading and reading
email from Gmail servers.

Possible uses for this library include machine learning corpus generation and
command line mail clients.


## Setup

```clojure
(:require [clojure-mail.core :refer :all])
```

## Authentication

There are two ways to authenticate. The first is to manually create a mail store session like this

```clojure

(auth! "username@gmail.com" "password")

(gen-store)
```

If you just want to get something from your inbox you can also pass in your gmail credentials like this


```

;; Get the last 5 messages from your Gmail inbox

(def messages (inbox "user@gmail.com" "password" 5))

(def message-subject (:subject (first messages)))

;; => "Top Stories from the last 24 hours"

```


## Reading email messages

Let's fetch the last 3 messages from our Gmail inbox

```clojure

(def inbox-messages (inbox "username@gmail.com" "password" 3))

;; Lets fetch the subject of the latest message

(:subject (first inbox-messages))

;; => "Booking confirmed (MLC35TJ4): Table for 2 at The Potted Pig 22 March 2014 - at 13:30"

;; The following keys are available on an email message

(keys (first inbox-messages))

(:subject :from :date-recieved :to :multipart? :content-type :sender :date-sent :body)

```

An email message is returned as a Clojure map that looks something like this (with body removed)

```clojure

(def m (dissoc (first (inbox 1)) :body)) ;; =>

;; =>

;; {:subject "Re: Presents for Dale's baby",
;;  :from "Someone <someone@aol.com>",
;;  :date-recieved "Tue Mar 11 12:54:41 GMT 2014",
;;  :to ("owain@owainlewis.com"),
;;  :multipart? true,
;;  :content-type "multipart/ALTERNATIVE",
;;  :sender "Someone <someone@aol.com>",
;;  :date-sent "Tue Mar 11 12:54:36 GMT 2014"}

```

## Parser

HTML emails are evil. There is a simple HTML -> Plain text parser provided if you need to
do any machine learning type processing on email messages.

```clojure
(require '[clojure-mail.parser :refer :all])

(html->text "<h1>I HATE HTML EMAILS</h1>")

;; => "I HATE HTML EMAILS"

```

## Reading emails from disk

Clojure mail can be used to parse existing email messages from file. Take a look in test/fixtures to see some example messages. To read one of these messages we can do something like this


```clojure

(def message (read-mail-from-file "test/clojure_mail/fixtures/25"))

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

Copyright © 2014 Owain Lewis

Distributed under the Eclipse Public License, the same as Clojure.
