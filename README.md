# Clojure-mail


```
[clojure-mail "0.1.5"]
```

A clojure library mainly aimed at parsing, downloading and reading
email from Gmail servers (it works for private domains as well).

Possible uses for this library include machine learning corpus generation and
command line mail clients.

There are currently some issues with handling large volumes of mail
(i.e hundreds or thousands of messages)


## Setup

```clojure
(:require [clojure-mail.core :refer :all])
```

## Authentication

```clojure
(auth! "username@gmail.com" "password")
```

## Reading email messages

Let's fetch the last 3 messages from our Gmail inbox

```clojure

(def inbox-messages (inbox 3))

;; Lets fetch the subject of the latest message

(:subject (first inbox-messages))

;; => "Booking confirmed (MLC35TJ4): Table for 2 at The Potted Pig 22 March 2014 - at 13:30"

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
(html->text "<h1>I HATE HTML EMAILS</h1>")

;; => "I HATE HTML EMAILS"

```

## License

Copyright © 2014 Owain Lewis

Distributed under the Eclipse Public License, the same as Clojure.
