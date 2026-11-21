(ns clojure-mail.helpers.greenmail
  "Some functions to make easier to create integration testing with GreenMail."
  (:import [com.icegreen.greenmail.imap ImapServer]
           [com.icegreen.greenmail.user GreenMailUser]
           [com.icegreen.greenmail.util GreenMail ServerSetupTest]
           [javax.mail Message$RecipientType Session]
           [javax.mail.internet InternetAddress MimeMessage MimeMessage$RecipientType]
           [java.util Properties]))

(defn start-greenmail!
  "starts an already existing GreenMail instance."
  [^GreenMail gm]
  (.start gm)
  gm)

(defn stop-greenmail!
  "stops an already existing and started GreenMail instance."
  [^GreenMail gm]
  (.stop gm)
  gm)

(defn reset-greenmail!
  "resets an already existing GreenMail instance."
  [^GreenMail gm]
  (.reset gm))

(defn make-greenmail
  "Creates a GreenMail server with `options`. Options are:
  * :server [:imap|:imaps|:all-imap]. Default: :imap
  * :start true|false. Starts the server after creating it. Default false.
  "
  [& {:keys [server start] :or {server :imap start false}}]
  (let [gm (case server
             :imap (GreenMail. ServerSetupTest/IMAP)
             :imaps (GreenMail. ServerSetupTest/IMAPS)
             :all-imap (GreenMail.))]
    (if start
      (start-greenmail! gm)
      gm)))

(defn get-greenmail-ports
  "Returns a map with the ports for each service configured to run in `gm`. Because
  of the GreenMail internals, the instance needs to have been started. If not,
  this function will raise IllegalStateException.

  Examples of returned values would be:

  * For a Greenmail instance configured to run only IMAP: `{:imap 1244 :imaps nil}`
  * For an instance with only IMAPS: `{:imap nil :imaps 1234}`
  * For an instance with both: `{:imap 1234 :imaps 4567}`.
  "
  [^GreenMail gm]
  (try
    (let [^ImapServer imap-server (.getImap gm)
          ^ImapServer imaps-server (.getImaps gm)]
      {:imap  (when imap-server (.getPort imap-server))
       :imaps (when imaps-server (.getPort imaps-server))})
    (catch NullPointerException _
      (throw (IllegalStateException. "GreenMail instance not started")))))

(defn make-greenmail-user!
  "In the `gm` GreenMail instance, it creates a user with `login` as username,
  `pass` as password and `email` as address. If there are any messages in `msgs`, they are
  delivered to the user right after it is created.
  Returns the created `GreenMailUser` instance."
  [^GreenMail gm ^String email ^String login ^String pass & msgs]
  (let [^GreenMailUser usr (.setUser gm email login pass)]
    (doseq [^MimeMessage m msgs]
      (.deliver usr m))
    usr))

(defn make-direct-text-message
  "Creates a single-recipient, text message to be delivered directly to a GreenMail user without using an MTA.
  Returns an instance of MimeMessage. Arguments can be passed also as a map."
  ([^String from ^String to ^String subject body]
   (let [sess (Session/getInstance (Properties.))
         msg (doto (MimeMessage. sess)
               (.setFrom (InternetAddress. from))
               (.setRecipient Message$RecipientType/TO (InternetAddress. to))
               (.setSubject subject)
               (.setContent body "text/plain"))]
     msg))
  ([email-map]
   (make-direct-text-message (:from email-map)
                             (:to email-map)
                             (:subject email-map)
                             (:body email-map))))

(defn direct-text-message->map
  "Given a MimeMessage instance created by make-direct-text-message, turns it into a map with the
  same structure (:from, :to, :subject, :body)."
  [^MimeMessage m]
  {:body    (.getContent m)
   :subject (.getSubject m)
   :from    (.getAddress ^InternetAddress (first (.getFrom m)))
   :to      (.getAddress ^InternetAddress (first (.getRecipients m MimeMessage$RecipientType/TO)))})

(defn get-greenmail-user-by-login
  "Given `gm`, a GreenMail instance and `login`, the login-name of one of its
  _existing_ users, returns the corresponding `GreenMailUser` object."
  [^GreenMail gm ^String login]
  (let [usr-mgr (.. gm (getManagers) (getUserManager))]
    (.getUser usr-mgr login)))

(defn get-greenmail-user-by-email
  "Given `gm`, a GreenMail instance and `email`, the email addr of one of its
  _existing_ users, returns the corresponding `GreenMailUser` object."
  [^GreenMail gm ^String email]
  (let [usr-mgr (.. gm (getManagers) (getUserManager))]
    (.getUserByEmail usr-mgr email)))

(defn deliver-direct-text-message!
  "Directly delivers a single sender, single recipient message to an existing
  user (identified by the `to` parameter in the `gm` GreenMail instance. No
  SMTP is used. This message will end in the user's INBOX folder."
  ([^GreenMail gm from to subject body]
   (let [^GreenMailUser usr (get-greenmail-user-by-email gm to)
         ^MimeMessage msg (make-direct-text-message from to subject body)]
     (.deliver usr msg)))
  ([gm msg-map]
   (deliver-direct-text-message!
    gm
    (:from msg-map)
    (:to msg-map)
    (:subject msg-map)
    (:body msg-map))))
