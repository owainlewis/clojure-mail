(ns clojure-mail.folder
  (:refer-clojure :exclude [list])
  (:import [javax.mail.search SearchTerm OrTerm AndTerm SubjectTerm BodyTerm RecipientStringTerm FromStringTerm FlagTerm ReceivedDateTerm SentDateTerm]
           (com.sun.mail.imap IMAPFolder IMAPFolder$FetchProfileItem IMAPMessage)
           (java.text SimpleDateFormat)
           (java.util Calendar)
           (javax.mail FetchProfile FetchProfile$Item Flags)))

;; note that the get folder fn is part of the store namespace

(def ^:dynamic current-folder)

(defmacro with-folder [folder store & body]
  `(let [fd# (doto (.getFolder ~store ~folder) (.open IMAPFolder/READ_ONLY))]
     (binding [current-folder fd#]
       (do ~@body))))

(defn get-folder
  "Returns an IMAPFolder instance"
  [store folder-name]
  (.getFolder store folder-name))

(defn full-name [f]
  (.getFullName f))

(defn new-message-count
  "Get number of new messages in folder f"
  [f]
  (.getNewMessageCount f))

(defn message-count
  "Get total number of messages in folder f"
  [f]
  (.getMessageCount f))

(defn unread-message-count
  "Get number of unread messages in folder f"
  [f]
  (.getUnreadMessageCount f))

(defn get-message-by-uid [f id]
  (.getMessageByUID f id))

(defn get-message [f id]
  (.getMessage f id))

(defn fetch-messages
  "Pre-fetch message attributes for a given fetch profile.
  Messages are retrieved as light weight objects and individual fields such as headers or body are populated lazily.
  When bulk fetching messages you can pre-fetch these items based on a com.sun.mail.imap.FetchProfileItem
  f - the folder from which to fetch the messages
  ms - the messages to fetch
  :fetch-profile - optional fetch profile, defaults to entire message. fetch profiles are:

      :message
      :headers
      :flags
      :envelope
      :content-info
      :size
      "
  [f ms & {:keys [fetch-profile] :or {fetch-profile :message}}]
  (let [fp (FetchProfile.)
        item (condp = fetch-profile
               :message IMAPFolder$FetchProfileItem/MESSAGE
               :headers IMAPFolder$FetchProfileItem/HEADERS
               :flags IMAPFolder$FetchProfileItem/FLAGS
               :envelope IMAPFolder$FetchProfileItem/ENVELOPE
               :content-info IMAPFolder$FetchProfileItem/CONTENT_INFO
               :size FetchProfile$Item/SIZE)
        _ (.add fp item)]
    (.fetch f (into-array IMAPMessage ms) fp)))

(defn get-messages
  "Gets all messages from folder f or get the Message objects for message numbers ranging from start through end,
  both start and end inclusive. Note that message numbers start at 1, not 0."
  ([folder]
   (.getMessages folder))
  ([folder start end]
   (.getMessages folder start end)))

(defn to-recipient-type 
  "Converts keyword to recipient type"
  [rt]
  (case rt
    :to javax.mail.Message$RecipientType/TO
    :cc javax.mail.Message$RecipientType/CC
    :bcc javax.mail.Message$RecipientType/BCC))

(defn to-flag
  "Converts a string to message flag"
  [fl]
  (case fl
    (:-answered? :answered?) javax.mail.Flags$Flag/ANSWERED
    (:-deleted? :deleted?) javax.mail.Flags$Flag/DELETED
    (:flagged? :flagged) javax.mail.Flags$Flag/FLAGGED
    (:-draft? :draft?) javax.mail.Flags$Flag/DRAFT
    (:-recent? :recent?) javax.mail.Flags$Flag/RECENT
    (:-seen? :seen?.) javax.mail.Flags$Flag/SEEN))

(defn to-date-comparison
  "Returns the correct comparison term for search"
  [dt]
  (cond 
    (.contains (str dt) "-before") javax.mail.search.ComparisonTerm/LE
    (.contains (str dt) "-after") javax.mail.search.ComparisonTerm/GE
    (.contains (str dt) "-on") javax.mail.search.ComparisonTerm/EQ))

(def date-formats ["yyyy-MM-dd" "yyyy.MM.dd"])

(defn to-date
  "Parses a date string to date"
  [ds]
    (cond 
      (string? ds) (first (remove nil? (map #(try (.parse (SimpleDateFormat. %) ds) (catch Exception e nil)) date-formats))) 
      (instance? java.util.Date ds) ds
      (= ds :today) (.getTime (Calendar/getInstance))
      (= ds :yesterday) (let [d (Calendar/getInstance)]
                          (.add d Calendar/DAY_OF_MONTH -1)
                          (.getTime d))))


(defn- class-inst 
  "Instantiates a java class with parameters"
  [a & parameters] 
  `(new ~a ~@parameters))

(defn build-search-terms
  "This creates a search condition. Input is a sequence of message part conditions or flags or header conditions.
   Possible message part condititon is: (:from|:cc|:bcc|:to|:subject|:body) value or date condition.
   Date condition is: (:received-before|:received-after|:received-on|:sent-before|:sent-after|:sent-on) date. 
   Header condition is: :header (header-name-string header-value, ...)
   Supported flags are: :answered?, :deleted?, :draft?, :recent?, :flagged? :seen?. Minus sign at the beginning of flag tests for negated flag value (ex. :-answered? not answered messages).

   Terms on the same level is connected with and-ed, if value is a sequence, then those values are or-ed. 
    
   Examples: 
    (:body \"foo\" :body \"bar\") - body should match both values.
    (:body [\"foo\" \"bar\"]) - body should match one of the values.
    (:body \"foo\" :from \"john@exmaple.com\") - body should match foo and email is sent by john."
  ([query]
   (build-search-terms query []))
  ([query terms-so-far]
    (let [ft (first query)
          inst (fn [a & params] (eval `(new ~a ~@params)))
          or-term-builder (fn[cl params]
                            (if (coll? params)
                              (OrTerm. (into-array (map #(inst cl %) params)))
                              (inst cl params)))
          rec-call (fn[skip-n term] (build-search-terms (nthnext query skip-n) (conj terms-so-far term)))]
      (case ft
        :body (rec-call 2 (or-term-builder BodyTerm (second query)))
        :from (rec-call 2 (or-term-builder FromStringTerm (second query)))
        (:to :cc :bcc) (rec-call 2 (RecipientStringTerm. (to-recipient-type ft) (second query)))
        (:answered? :deleted? :draft? :recent? :seen? :flagged?) (rec-call 1 (FlagTerm. (Flags. (to-flag ft)) true))
        (:-answered? :-deleted? :-draft? :-recent? :-seen? :-flagged?) (rec-call 1 (FlagTerm. (Flags. (to-flag ft)) false))
        (:received-before :received-after :received-on) (rec-call 2 (ReceivedDateTerm. (to-date-comparison ft) (to-date (second query))))
        (:sent-before :sent-after :sent-on) (rec-call 2 (SentDateTerm. (to-date-comparison ft) (to-date (second query)))) 
        :subject (rec-call 2 (or-term-builder SubjectTerm (second query)))
        nil (cond 
              (empty? terms-so-far) nil ; probably an error
              (= (count terms-so-far) 1) (first terms-so-far)
              :else (AndTerm. (into-array SearchTerm terms-so-far)))))))

(defn search [f & query]
  (let [search-term (if (string? (first query))
                      (OrTerm. (SubjectTerm. (first query)) (BodyTerm. (first query)))
                      (build-search-terms query))]
    (.search f search-term)))

(defn list
  "List all folders under folder f"
  [f]
  (.list f))
