(ns clojure-mail.folder
  (:refer-clojure :exclude [list])
  (:import [javax.mail.search SearchTerm OrTerm AndTerm SubjectTerm HeaderTerm BodyTerm RecipientStringTerm FromStringTerm FlagTerm ReceivedDateTerm SentDateTerm ComparisonTerm]
           (com.sun.mail.imap IMAPFolder IMAPFolder$FetchProfileItem)
           (java.text SimpleDateFormat)
           (java.time Instant LocalDate ZoneId)
           (java.util Calendar Date)
           (javax.mail FetchProfile FetchProfile$Item Flags Flags$Flag Folder Message$RecipientType Store)))

;; Note that the get folder fn is part of the store namespace

(def ^:dynamic current-folder nil)

(defmacro with-folder
  "Opens `folder` read-only on `store`, binds `current-folder` to the open
   folder while evaluating `body`, and closes the folder on exit."
  [folder store & body]
  `(let [^Store store# ~store
         ^Folder fd# (.getFolder store# ~folder)]
     (.open fd# IMAPFolder/READ_ONLY)
     (try
       (binding [current-folder fd#]
         ~@body)
       (finally
         (when (.isOpen fd#)
           (.close fd# false))))))

(defn get-folder
  "Returns an IMAPFolder instance"
  [^javax.mail.Store store ^String folder-name]
  (.getFolder store folder-name))

(defn full-name [^javax.mail.Folder f]
  (.getFullName f))

(defn new-message-count
  "Get number of new messages in folder f"
  [^javax.mail.Folder f]
  (.getNewMessageCount f))

(defn message-count
  "Get total number of messages in folder f"
  [^javax.mail.Folder f]
  (.getMessageCount f))

(defn unread-message-count
  "Get number of unread messages in folder f"
  [^javax.mail.Folder f]
  (.getUnreadMessageCount f))

(defn get-message-by-uid [^javax.mail.UIDFolder f id]
  (.getMessageByUID f (long id)))

(defn get-message [^javax.mail.Folder f id]
  (.getMessage f (int id)))

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
        ^FetchProfile$Item item (condp = fetch-profile
                                  :message IMAPFolder$FetchProfileItem/MESSAGE
                                  :headers IMAPFolder$FetchProfileItem/HEADERS
                                  :flags IMAPFolder$FetchProfileItem/FLAGS
                                  :envelope IMAPFolder$FetchProfileItem/ENVELOPE
                                  :content-info IMAPFolder$FetchProfileItem/CONTENT_INFO
                                  :size FetchProfile$Item/SIZE)
        _ (.add fp item)]
    (.fetch ^javax.mail.Folder f ^"[Ljavax.mail.Message;" (into-array javax.mail.Message ms) fp)))

(defn get-messages
  "Gets all messages from folder f or get the Message objects for message numbers ranging from start through end,
  both start and end inclusive. Note that message numbers start at 1, not 0."
  ([^javax.mail.Folder folder]
   (.getMessages folder))
  ([^javax.mail.Folder folder start end]
   (.getMessages folder (int start) (int end))))

(defn to-recipient-type
  "Converts keyword to recipient type"
  [rt]
  (case rt
    :to Message$RecipientType/TO
    :cc Message$RecipientType/CC
    :bcc Message$RecipientType/BCC))

(defn to-flag
  "Converts a string to message flag"
  [fl]
  (case fl
    (:-answered? :answered?) Flags$Flag/ANSWERED
    (:-deleted? :deleted?) Flags$Flag/DELETED
    (:-flagged? :flagged?) Flags$Flag/FLAGGED
    (:-draft? :draft?) Flags$Flag/DRAFT
    (:-recent? :recent?) Flags$Flag/RECENT
    (:-seen? :seen?) Flags$Flag/SEEN))

(defn to-date-comparison
  "Returns the correct comparison term for search"
  [dt]
  (cond
    (.contains (str dt) "-before") ComparisonTerm/LE
    (.contains (str dt) "-after") ComparisonTerm/GE
    (.contains (str dt) "-on") ComparisonTerm/EQ))

(def date-formats ["yyyy-MM-dd" "yyyy.MM.dd"])

(defn to-date
  "Parses a date string (`yyyy-MM-dd` or `yyyy.MM.dd`), `java.util.Date`,
   `java.time.Instant`, `java.time.LocalDate`, `:today`, or `:yesterday`
   into a `java.util.Date`."
  [ds]
  (cond
    (string? ds) (first (remove nil? (map #(try (.parse (SimpleDateFormat. ^String %) ^String ds)
                                                (catch Exception _ nil))
                                          date-formats)))
    (instance? Date ds) ds
    (instance? Instant ds) (Date/from ^Instant ds)
    (instance? LocalDate ds) (Date/from (.toInstant (.atStartOfDay ^LocalDate ds (ZoneId/systemDefault))))
    (= ds :today) (.getTime (Calendar/getInstance))
    (= ds :yesterday) (let [d (Calendar/getInstance)]
                        (.add d Calendar/DAY_OF_MONTH -1)
                        (.getTime d))))

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
   (build-search-terms query [] true))
  ([query terms-so-far and-join]
   (let [ft (first query)
         or-term-builder-1 (fn [ctor params]
                             (if (coll? params)
                               (let [terms (map ctor params)]
                                 (case (count terms)
                                   0 (throw (IllegalArgumentException. "Search condition collection must not be empty"))
                                   1 (first terms)
                                   (OrTerm. ^"[Ljavax.mail.search.SearchTerm;" (into-array SearchTerm terms))))
                               (ctor params)))
         or-term-builder-2 (fn [ctor params]
                             (let [pairs (partition 2 params)]
                               (case (count pairs)
                                 0 (throw (IllegalArgumentException. "Header search requires at least one name/value pair"))
                                 1 (let [[p1 p2] (first pairs)] (ctor p1 p2))
                                 (OrTerm. ^"[Ljavax.mail.search.SearchTerm;"
                                  (into-array SearchTerm (map (fn [[p1 p2]] (ctor p1 p2)) pairs))))))
         parse-date (fn [v]
                      (or (to-date v)
                          (throw (IllegalArgumentException. (str "Invalid search date: " (pr-str v))))))
         rec-call (fn [skip-n term] (build-search-terms (nthnext query skip-n) (conj terms-so-far term) and-join))]
     (case ft
       :body (rec-call 2 (or-term-builder-1 #(BodyTerm. ^String %) (second query)))
       :from (rec-call 2 (or-term-builder-1 #(FromStringTerm. ^String %) (second query)))
       (:to :cc :bcc) (let [rt (to-recipient-type ft)]
                        (rec-call 2 (or-term-builder-1 #(RecipientStringTerm. rt ^String %) (second query))))
       (:answered? :deleted? :draft? :recent? :seen? :flagged?) (rec-call 1 (FlagTerm. (Flags. ^Flags$Flag (to-flag ft)) true))
       (:-answered? :-deleted? :-draft? :-recent? :-seen? :-flagged?) (rec-call 1 (FlagTerm. (Flags. ^Flags$Flag (to-flag ft)) false))
       :header (if (coll? (second query))
                 (rec-call 2 (or-term-builder-2 #(HeaderTerm. ^String %1 ^String %2) (second query)))
                 (rec-call 3 (or-term-builder-2 #(HeaderTerm. ^String %1 ^String %2) [(second query) (nth query 2)])))
       (:received-before :received-after :received-on) (rec-call 2 (ReceivedDateTerm. (to-date-comparison ft) (parse-date (second query))))
       (:sent-before :sent-after :sent-on) (rec-call 2 (SentDateTerm. (to-date-comparison ft) (parse-date (second query))))
       :subject (rec-call 2 (or-term-builder-1 #(SubjectTerm. ^String %) (second query)))
       nil (cond
             (empty? terms-so-far) nil ; probably an error
             (= (count terms-so-far) 1) (first terms-so-far)
             :else (if and-join
                     (AndTerm. (into-array SearchTerm terms-so-far))
                     (OrTerm. (into-array SearchTerm terms-so-far))))
        ; default case
       (if (coll? ft)
         (if-let [sub-term (build-search-terms ft [] (not and-join))]
           (rec-call 1 sub-term)
           (build-search-terms (next query) terms-so-far and-join))
          ; skip otherwise
         (build-search-terms (next query) terms-so-far and-join))))))

(defn search
  "Search folder f. With a single string, matches subject or body.
   Otherwise `query` is a sequence of conditions; see `build-search-terms`."
  [f & query]
  (let [search-term (if (string? (first query))
                      (OrTerm. (SubjectTerm. (first query)) (BodyTerm. (first query)))
                      (build-search-terms query))]
    (if (instance? javax.mail.Folder f)
      (.search ^javax.mail.Folder f ^SearchTerm search-term)
      (clojure.lang.Reflector/invokeInstanceMethod f "search" (object-array [search-term])))))

(defn list
  "List all folders under folder f"
  [^javax.mail.Folder f]
  (.list f))
