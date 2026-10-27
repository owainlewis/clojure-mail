# clojure-mail

[![CI](https://github.com/owainlewis/clojure-mail/actions/workflows/ci.yml/badge.svg)](https://github.com/owainlewis/clojure-mail/actions/workflows/ci.yml)
[![Clojars Project](https://img.shields.io/clojars/v/io.forward/clojure-mail.svg)](https://clojars.org/io.forward/clojure-mail)

A Clojure library for reading, searching and parsing email from IMAP servers, built on Jakarta Mail.

- Connect with a password or an OAuth 2 access token (XOAUTH2)
- List folders, read messages, and search with a small query language
- Turn messages into plain Clojure maps
- Watch a folder for new mail with IMAP IDLE
- Parse `.eml` files from disk

## Installation

Leiningen:

```clojure
[io.forward/clojure-mail "1.0.8"]
```

deps.edn:

```clojure
io.forward/clojure-mail {:mvn/version "1.0.8"}
```

Requires Java 11 or newer and Clojure 1.11 or newer. The 1.x line uses the `javax.mail` package names. See [ROADMAP.md](ROADMAP.md) for the move to `jakarta.mail`.

## Quickstart

Read the subject of the newest message in a Gmail inbox:

```clojure
(ns myproject.core
  (:require [clojure-mail.core :as mail]
            [clojure-mail.gmail :as gmail]
            [clojure-mail.message :as message]))

(def store (gmail/store "user@gmail.com" "app-password"))

(def latest (message/read-message (first (mail/inbox store))))

(:subject latest)

(keys latest)
;; => (:id :to :cc :bcc :from :sender :subject :date-sent :date-received
;;     :multipart? :content-type :body :headers)

(mail/close-store store)
```

## Authentication

### Password

```clojure
(def store (mail/store "imap.example.com" "user@example.com" "password"))

;; Non-standard port: pass [host port]
(def store (mail/store "imaps" ["imap.example.com" 1993] "user@example.com" "password"))
```

`store` defaults to `imaps` on port 993. Use `"imap"` for plain IMAP on port 143.

### Gmail

Google no longer accepts your normal account password over IMAP. Use one of:

- An [app password](https://support.google.com/accounts/answer/185833) with `gmail/store`, if your account has 2-Step Verification.
- An OAuth 2 access token with the `https://mail.google.com/` scope:

```clojure
(def store (gmail/xoauth-store "user@gmail.com" access-token))
```

Your application is responsible for obtaining and refreshing the token.

### OAuth 2 for any server

```clojure
(def store (mail/xoauth2-store "outlook.office365.com" "user@example.com" access-token))
```

## Reading messages

Messages come back as `javax.mail.Message` objects. Read them lazily with the functions in `clojure-mail.message`, or convert them to a map with `read-message`.

```clojure
(def msgs (take 5 (mail/all-messages store "inbox")))  ; newest first

(message/subject (first msgs))
(message/from (first msgs))      ; => ({:address "a@example.com" :name "A"})
(message/read? (first msgs))

;; Whole message as a map
(message/read-message (first msgs))

;; Only some fields, which is much faster over IMAP
(message/read-message (first msgs) :fields [:id :from :subject])
```

A message map looks like this:

```clojure
{:id "<CAF...@mail.gmail.com>"
 :subject "Re: Presents for Dale's baby"
 :from ({:address "someone@aol.com" :name "Someone"})
 :to ({:address "owain@owainlewis.com" :name "Owain Lewis"})
 :cc ()
 :bcc ()
 :sender {:address "someone@aol.com" :name "Someone"}
 :date-sent #inst "2015-10-23T12:19:33.838-00:00"
 :date-received #inst "2015-10-23T12:19:33.838-00:00"
 :multipart? true
 :content-type "multipart/ALTERNATIVE; boundary=..."
 :body [{:content-type "text/plain; charset=UTF-8" :body "..."}
        {:content-type "text/html; charset=UTF-8" :body "..."}]
 :headers [{"Subject" "Re: Presents for Dale's baby"} ...]}
```

If reading a field fails, the exception is collected under `:errors` instead of failing the whole message.

### Only new messages

```clojure
(mail/all-messages store "inbox" :since-uid 1200)
(mail/unread-messages store "inbox")
```

### Flags

```clojure
(message/mark-read msg)
(message/mark-flagged msg)
(message/mark-deleted msg)
(message/has-flag? msg :seen)        ; system flags by keyword or name
(message/has-flag? msg "my-label")   ; user flags by name
```

Changing flags requires the folder to be open with `:readwrite`.

## Folders

```clojure
(mail/folders store)                         ; nested folder names
(mail/message-count store "inbox")
(def inbox (mail/open-folder store "inbox" :readonly))
```

Gmail folder shortcuts:

```clojure
(gmail/open-folder store :sent :readonly)   ; "[Gmail]/Sent Mail"
```

## Searching

```clojure
(mail/search-inbox store "projects")                              ; subject or body
(mail/search-inbox store :body "projects" :from "john@example.com")
(mail/search-inbox store :subject ["invoice" "receipt"])           ; vector values are OR-ed
(mail/search-inbox store :received-after "2026-01-01" :-seen?)
(mail/search-inbox store [:from "a@example.com" :from "b@example.com"]) ; nested vector is OR-ed
```

Conditions: `:subject`, `:body`, `:from`, `:to`, `:cc`, `:bcc`, `:header`, `:received-before`, `:received-after`, `:received-on`, `:sent-before`, `:sent-after`, `:sent-on`, and the flags `:seen?`, `:answered?`, `:deleted?`, `:draft?`, `:flagged?`, `:recent?` (prefix with `-` to negate, e.g. `:-seen?`). Dates accept `"yyyy-MM-dd"`, `java.util.Date`, `:today` and `:yesterday`.

To search any folder, use `clojure-mail.folder/search` with an open folder.

## Watching a folder

Servers that support IMAP IDLE can push new mail to you:

```clojure
(require '[clojure-mail.events :as events])

(def session (mail/get-session "imaps"))
(def store (mail/store "imaps" session "imap.gmail.com" "me@gmail.com" "app-password"))
(def folder (mail/open-folder store "inbox" :readonly))
(def manager (events/new-idle-manager session))

(events/add-message-count-listener
 (fn [{:keys [messages]}] (prn :added (map message/subject messages)))
 (fn [e] (prn :removed e))
 folder
 manager)

;; later
(events/stop manager)
```

## Reading emails from disk

```clojure
(def msg (mail/file->message "dev-resources/emails/25"))
(message/read-message msg)
```

## HTML to text

```clojure
(require '[clojure-mail.parser :refer [html->text]])

(html->text "<h1>Hello <b>there</b></h1>")
;; => "Hello there"
```

## Development

See [CONTRIBUTING.md](CONTRIBUTING.md). Changes are listed in [CHANGELOG.md](CHANGELOG.md).

## License

Copyright © 2012-2026 Owain Lewis and contributors.

Distributed under the Eclipse Public License, the same as Clojure.
