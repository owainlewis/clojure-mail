# clojure-mail

** IN PROGRESS **

[![Build Status](https://secure.travis-ci.org/owainlewis/clojure-mail.png?branch=master)](http://travis-ci.org/owainlewis/clojure-mail)

A clojure library mainly aimed at parsing, downloading and reading email.

## Usage

IMAP 

```clojure

;; Create a mail store 

(def store (mail-store gmail "zaphrauk@gmail.com" "password"))

;; Get all folders

;; Get the inbox folder

(get-folder store "INBOX")

(take 10 (messages store "INBOX"))

```

Get 5 most recent messages from the spam folder

```
(take 5 (reverse (all-messages s gmail-spam)))
```

## Extracting data from messages

```
(def msg (first (all-messages s "INBOX")))

(from msg)
(subject msg)

;; Extract the body content for each message part

(message-body-map msg)

```

FIXME

## License

Copyright © 2012 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
