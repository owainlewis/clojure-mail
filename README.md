# Clojure-mail


```
[clojure-mail "0.1.4"]
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

## License

Copyright © 2014 Owain Lewis

Distributed under the Eclipse Public License, the same as Clojure.
