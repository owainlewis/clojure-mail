# clojure-mail

** IN PROGRESS **

A clojure library mainly aimed at parsing, downloading and reading email.

## Usage

IMAP 

```clojure

;; Create a mail store 

(def store (mail-store gmail "zaphrauk@gmail.com" "password"))

;; Get all folders

;; Get the inbox folder

(get-folder store "INBOX")

```

FIXME

## License

Copyright © 2012 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
