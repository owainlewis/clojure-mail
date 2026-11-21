(ns clojure-mail.message-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [clojure-mail.core :refer [file->message]]
            [clojure-mail.message :as message :refer [add-field answered? attachment? attachments
                                                      bcc cc content-type date-received date-sent
                                                      deleted? draft? encoding flagged? from
                                                      has-flag? id in-reply-to mark-answered
                                                      mark-deleted mark-draft mark-flagged
                                                      mark-read mark-recent message-body
                                                      message-headers message-parts mime-type
                                                      read-message read? recent? save-attachment
                                                      sender subject to uid]])
  (:import [java.io File]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.util Date Properties]
           [javax.activation DataHandler]
           [javax.mail Flags Flags$Flag Message$RecipientType Part Session]
           [javax.mail.internet InternetAddress MimeBodyPart MimeMessage MimeMultipart MimeUtility]
           [javax.mail.util ByteArrayDataSource]))

(defn load-fixture [fixture]
  (->> (str "emails/" fixture) io/resource io/file file->message))

(def fixture (load-fixture "25"))
(def fixture-26 (load-fixture "26"))

(defn- make-test-message-with-attachments
  []
  (let [session (Session/getInstance (Properties.))
        msg (MimeMessage. session)
        root (MimeMultipart. "mixed")
        alt (MimeMultipart. "alternative")
        alt-part (MimeBodyPart.)
        plain-part (doto (MimeBodyPart.)
                     (.setText "Hello plain" "utf-8"))
        html-part (doto (MimeBodyPart.)
                    (.setContent "<p>Hello html</p>" "text/html; charset=utf-8"))
        inline-img (doto (MimeBodyPart.)
                     (.setDataHandler (DataHandler. (ByteArrayDataSource. (.getBytes "img-bytes" "UTF-8") "image/png")))
                     (.setDisposition Part/INLINE)
                     (.setFileName "logo.png"))
        att-1 (doto (MimeBodyPart.)
                (.setDataHandler (DataHandler. (ByteArrayDataSource. (.getBytes "PDF-CONTENT-1" "UTF-8") "application/pdf")))
                (.setDisposition Part/ATTACHMENT)
                (.setFileName "report.pdf"))
        att-2 (doto (MimeBodyPart.)
                (.setDataHandler (DataHandler. (ByteArrayDataSource. (.getBytes "UTF8-ATTACHMENT" "UTF-8") "text/plain; charset=utf-8")))
                (.setDisposition Part/ATTACHMENT)
                (.setFileName (MimeUtility/encodeText "résumé 2026.txt" "UTF-8" nil)))
        att-traversal (doto (MimeBodyPart.)
                        (.setDataHandler (DataHandler. (ByteArrayDataSource. (.getBytes "SAFE-CONTENT" "UTF-8") "text/plain")))
                        (.setDisposition Part/ATTACHMENT)
                        (.setFileName "../../evil/secret.txt"))]
    (.addBodyPart alt plain-part)
    (.addBodyPart alt html-part)
    (.setContent alt-part alt)
    (.addBodyPart root alt-part)
    (.addBodyPart root inline-img)
    (.addBodyPart root att-1)
    (.addBodyPart root att-2)
    (.addBodyPart root att-traversal)
    (doto msg
      (.setFrom (InternetAddress. "alice@example.com" "Alice"))
      (.setSender (InternetAddress. "sender@example.com" "Sender"))
      (.setRecipient Message$RecipientType/TO (InternetAddress. "bob@example.com" "Bob"))
      (.setRecipient Message$RecipientType/CC (InternetAddress. "carol@example.com" "Carol"))
      (.setRecipient Message$RecipientType/BCC (InternetAddress. "dave@example.com" "Dave"))
      (.setHeader "In-Reply-To" "<prev-123@example.com>")
      (.setSubject "Quarterly Report")
      (.setSentDate (Date.))
      (.setContent root)
      (.saveChanges))))

(deftest mime-types-test
  (testing "should parse correct mime-type on email message"
    (let [types {:plain "TEXT/PLAIN; charset=utf-8; format=fixed"
                 :html  "TEXT/HTML; charset=utf-8"}]
      (is (= :plain (mime-type (:plain types))))
      (is (= :html (mime-type (:html types)))))))

(deftest message-to-test
  (testing "should return a sequence of message receivers"
    (is (= (count (to fixture)) 1))
    (is (= (first (to fixture)) {:address "zaphrauk@gmail.com" :name nil}))))

(deftest message-subject-test
  (testing "should return an email message subject"
    (is (= (subject fixture) "Request to share ContractsBuilder"))
    (is (= (subject fixture-26) "Your AdSense Payment is on its way!"))))

(deftest message-from-test
  (testing "should return the message sender"
    (is (seq (from fixture)))
    (is (every? #(contains? % :address) (from fixture)))
    (is (= [{:address "adsense-noreply@google.com" :name "Google AdSense"}]
           (vec (from fixture-26))))))

(deftest message-fields-test
  (let [msg (make-test-message-with-attachments)]
    (is (= [{:address "bob@example.com" :name "Bob"}] (vec (to msg))))
    (is (= [{:address "carol@example.com" :name "Carol"}] (vec (cc msg))))
    (is (= [{:address "dave@example.com" :name "Dave"}] (vec (bcc msg))))
    (is (= {:address "sender@example.com" :name "Sender"} (sender msg)))
    (is (= "<prev-123@example.com>" (in-reply-to msg)))
    (is (nil? (in-reply-to fixture-26)))
    (is (string? (id msg)))
    (is (instance? Date (date-sent msg)))
    (is (nil? (date-received msg)))
    (is (nil? (uid msg)))
    (is (nil? (encoding msg)))
    (is (vector? (message-headers msg)))))

(deftest message-content-type-test
  (testing "should return the message content-type"
    (is (= (mime-type (content-type fixture)) :multipart))))

(deftest mime-type-test
  (is (= :multipart (mime-type "multipart/mixed; boundary=x")))
  (is (= :multipart (mime-type "multipart/related; boundary=x")))
  (is (= :multipart (mime-type "multipart/signed; protocol=pgp")))
  (is (= :plain (mime-type "text/plain ; charset=utf-8")))
  (is (nil? (mime-type "application/pdf")))
  (is (nil? (mime-type nil))))

(deftest has-flag-test
  (let [^MimeMessage msg (load-fixture "25")]
    (testing "a fresh message has no flags"
      (is (false? (read? msg)))
      (is (false? (answered? msg)))
      (is (false? (deleted? msg)))
      (is (false? (draft? msg)))
      (is (false? (flagged? msg)))
      (is (false? (recent? msg))))
    (testing "system flags are matched by name, keyword or Flags$Flag"
      (mark-read msg)
      (is (true? (read? msg)))
      (is (true? (has-flag? msg "seen")))
      (is (true? (has-flag? msg :seen)))
      (is (true? (has-flag? msg Flags$Flag/SEEN))))
    (testing "all system flag setters work as documented"
      (mark-answered msg)
      (mark-deleted msg)
      (mark-draft msg)
      (mark-flagged msg)
      (mark-recent msg)
      (is (true? (answered? msg)))
      (is (true? (deleted? msg)))
      (is (true? (draft? msg)))
      (is (true? (flagged? msg)))
      (is (true? (recent? msg))))
    (testing "deprecated mark-as-flagged preserves DRAFT behaviour"
      (let [^MimeMessage msg2 (load-fixture "25")]
        #_{:clj-kondo/ignore [:deprecated-var]}
        (message/mark-as-flagged msg2)
        (is (true? (draft? msg2)))
        (is (false? (flagged? msg2)))))
    (testing "user flags are matched by name"
      (.setFlags msg (Flags. "important") true)
      (is (true? (has-flag? msg "important")))
      (is (false? (has-flag? msg "other"))))))

(deftest read-message-test
  (testing "reads all fields by default"
    (let [m (read-message fixture)]
      (is (= "Request to share ContractsBuilder" (:subject m)))
      (is (true? (:multipart? m)))
      (is (vector? (:headers m)))
      (is (nil? (:errors m)))))
  (testing "only the requested fields are returned"
    (is (= #{:id :subject} (set (keys (read-message fixture :fields [:id :subject]))))))
  (testing "multipart bodies are read as a sequence of parts"
    (let [body (message-body fixture)]
      (is (seq body))
      (is (every? :content-type (flatten body)))))
  (testing "single-part message returns a map from message-body and nil from message-parts"
    (let [session (Session/getInstance (Properties.))
          msg (doto (MimeMessage. session)
                (.setText "Plain text body" "utf-8")
                (.saveChanges))]
      (is (nil? (message-parts msg)))
      (is (= "Plain text body" (:body (message-body msg))))
      (is (false? (:multipart? (read-message msg))))))
  (testing "add-field collects exceptions under :errors"
    (let [m (add-field fixture {} :boom (fn [_] (throw (ex-info "boom" {}))))]
      (is (not (contains? m :boom)))
      (is (= 1 (count (:errors m))))
      (is (= "boom" (.getMessage ^Exception (first (:errors m))))))))

(deftest attachments-test
  (let [msg (make-test-message-with-attachments)
        atts (attachments msg)]
    (testing "lists attachments and ignores inline/body parts"
      (is (= 3 (count atts)))
      (is (= ["report.pdf" "résumé 2026.txt" "../../evil/secret.txt"]
             (mapv :filename atts)))
      (is (every? #(attachment? (:part %)) atts))
      (is (empty? (attachments fixture-26))))
    (testing "save-attachment writes to a directory with sanitized filename and to an explicit file"
      (let [tmp-dir (.toFile (Files/createTempDirectory "clojure-mail-att-test"
                                                        (into-array FileAttribute [])))
            explicit-file (io/file tmp-dir "nested" "custom-name.pdf")]
        (try
          (let [^File f1 (save-attachment (first atts) tmp-dir)
                ^File f2 (save-attachment (nth atts 2) tmp-dir)
                ^File f3 (save-attachment (:part (first atts)) explicit-file)]
            (is (= (io/file tmp-dir "report.pdf") f1))
            (is (= "PDF-CONTENT-1" (slurp f1)))
            ;; Path traversal stripped to safe basename inside tmp-dir
            (is (= (io/file tmp-dir "secret.txt") f2))
            (is (= (.getParentFile f2) tmp-dir))
            (is (= "SAFE-CONTENT" (slurp f2)))
            ;; Explicit file path creates missing parent directories
            (is (= explicit-file f3))
            (is (= "PDF-CONTENT-1" (slurp f3))))
          (finally
            (doseq [^File f (reverse (file-seq tmp-dir))]
              (.delete f))))))))
