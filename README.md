# Clojure-mail


```
[clojure-mail "0.1.4"]
```

A clojure library mainly aimed at parsing, downloading and reading email from Gmail servers (it works for private domains as well).

Possible uses for this library include machine learning corpus generation and command line mail clients.

There are currently some issues with handling large volumes of mail (i.e hundreds or thousands of messages)

## Quick example

Require clojure-mail

```clojure
(require '[clojure-mail.core :refer :all])
```

Quick example of how to read a message from your inbox

```clojure
(def my-inbox (inbox "user@gmail.com" "password"))

(read-message (first my-inbox))

;; Returns the first message from my gmail account

{:to ("you+someAlias@gmail.com" "you+anotherAlias@gmail.com" "someone@gmail.com"),
 :from "EuroDNS Customer Services <support@eurodns.com>",
 :subject "Re: Domain",
 :sender "EuroDNS Customer Services <support@eurodns.com>",
 :date-sent "Thu Apr 04 20:33:42 BST 2013",
 :date-recieved "Thu Apr 04 20:33:44 BST 2013",
 :multipart? false,
 :content-type "TEXT/PLAIN; charset=utf-8",
 :body [{"TEXT/PLAIN; charset=utf-8" "Dear Customer...}]}
```

## Examples

In this example we'll log into a Gmail account and read messages from the inbox and spam folders.

The first thing we need to do is create a mail store that acts as a gateway to your gmail account.

```clojure
(def store (gen-store "USER@GMAIL.COM" "PASSWORD"))
```

Alternativley some methods make use of authentication settings held in a `ref` to avoid having to pass the mail store around.  The ref can be set like so,

```clojure
(auth! "USER@GMAIL.COM" "PASSWORD")
```

Now we can get 5 messages from our inbox

```clojure
(def my-messages (take 5 (get-inbox)))
```

We'll need the `message` namespace for reading individual emails. Require it with

```clojure
(require '[clojure-mail.message :as msg])
```

To read the first message in our inbox

```clojure
(msg/read-message (first (get-inbox)))
```

Or read a message from the spam folder

```clojure
(def spam (take 5 (get-spam)))
```

Count the number of messages in our inbox

```clojure
(count (all-messages store "INBOX"))
```

It's important to note that mail is ordered the wrong way. If you want to get your most recent mail you'll need
to do something like this

```clojure
(def recent
  (->> (all-messages store "INBOX")
        reverse
        (take 5)))
```

There's a helper function to get the lastest n messages from your inbox

```clojure
(def messages (get-recent store 10))

;; Read the subjects
(map (comp :subject msg/read-message) m)

```

Want a summary of the last 5 messages in your inbox?yg

```clojure
(message-list store 5)
```

## Message parsing

Get all headers from an email

```clojure

;; Get an email message from our inbox

(def m (first (get-inbox)))

;; Returns all headers as a map

(msg/message-headers m)

;; Get the sender

(msg/sender m)

;; Get the from address

(msg/from m)

```

## Listing Mail Folders

You can return a list of mail folders easily

```clojure
(folders store)

;; (("INBOX") ("Important Info") ("Zero" ("Group") ("Newsletter")
;;  ("Notification") ("Registration")) ("[Gmail]" ("All Mail")
;;  ("Drafts") ("Important") ("Sent Mail") ("Spam") ("Starred")
;;  ("Trash")) ("[Mailbox]" ("Later") ("To Buy") ("To Read") ("To Watch")))
```

## Unread messages

Fetch unread emails

```clojure

(unread-messages "INBOX")

```

## License

Copyright © 2012 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
