;; The mail helper
;;
;; Copyright 2010 by ICM Consulting Pty Ltd,
;; 3 Mark Place
;; Bilgola Plateau NSW 2107
;; Australia
;; All rights reserved.
;; This software is the confidential and proprietary information
;; of ICM. ("Confidential Information"). You
;; shall not disclose such Confidential Information and shall use
;; it only in accordance with the terms of the license agreement
;; you entered into with ICM.

(ns clojure-mail.mail
  (:use [clojure-mail.core])
  (:require [clojure-mail.message :as msg] )
  )

(defn pollingMail []
  ;; Create your auth creds
  (auth! "payreqbox@gmail.com" "1payreq$")
  ;; Read unread-messages in inbox
  (let [msgs (map msg/read-msg (unread-messages "INBOX"))]
    (println (map :subject msgs)))
  )



;; Get unread messages every 5 sec from the spam folder
;; (every 5000 testfn my-pool)
(pollingMail)