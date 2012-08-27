# clojure-mail

** IN PROGRESS **

Please excuse the clunky DSL and lack of tests. Still mocking this up : )

[![Build Status](https://secure.travis-ci.org/owainlewis/clojure-mail.png?branch=master)](http://travis-ci.org/owainlewis/clojure-mail)

A clojure library mainly aimed at parsing, downloading and reading email.

## Usage

IMAP 

```clojure

;; Create a mail store 

(def store (mail-store gmail "user@gmail.com" "password"))

;; Get all folders

;; Get the inbox folder

(get-folder store "INBOX")

(take 10 (messages store "INBOX"))

```

Print out a list of all the folders for an IMAP session

```clojure

(def s (mail-store gmail "user@gmail.com" "pass"))
(folders s)

```

Get 5 most recent messages from the spam folder

```
(take 5 (reverse (all-messages s gmail-spam)))
```

## Extracting data from messages

```clojure

(def s (mail-store gmail "user@gmail.com" "pass"))

(def msg (first (all-messages s "INBOX")))

(from msg)

(subject msg)

;; Extract the body content for each message part

(message-body-map msg)

```

PS!!!

Don't do stuff like this. It will bring Emacs to a crashing halt. (having said that it did work but took about 2 minutes to sort the data)

```clojure
(def new-messages (take 10 (reverse (messages store "INBOX"))))
```

I'll look into sorting by newer messages first or chunking or something to reduce the memory overhead. You can search for individual GUID's if you know them. 


FIXME

## License

Copyright © 2012 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
