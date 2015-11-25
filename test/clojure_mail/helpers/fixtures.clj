(ns clojure-mail.helpers.fixtures
  "Namespace with functions to reduce the boilerplate of fixture creation."
  (:require [clojure-mail.helpers.greenmail :as gh]))

(def ^:dynamic *the-gm*)

(defn make-gm-fixture
  "Creates a fixture (so returns a function) that, before any test,
  creates an instance of GreenMail, binds it
  to `*the-gm*` and starts it. Once the tests are executed, it shuts it down.
  As a paramteer `svc` can be :imap, :imaps or :all-imap.

  Specifically designed as a `:once` fixture maker."
  [svc]
  (fn [f]
    (binding [*the-gm* (gh/make-greenmail :server svc)]
      (try
        (gh/start-greenmail! *the-gm*)
        (f)
        (finally
          (when *the-gm*
            (gh/reset-greenmail! *the-gm*)
            (gh/stop-greenmail! *the-gm*)))))))

(defn make-custom-fixture-from-config
  "Creates a fixture (so returns a function) that
  resets and configures the GreenMail instance pointed by `*the-gm*`to
  have the users and messages the `cfg` map defines. Then
  it executes any tests received.
  This configuration map has the shape:
  `[{:email :login :pass :messages [{:from :subject :body}{...}]
  { ... }]`

  Speficically designed as a `:each` fixture maker."
  [cfg]
  (fn [f]
    (gh/reset-greenmail! *the-gm*)
    (doseq [cfg-item (seq cfg)
            :let [login (:login cfg-item)
                  pass (:pass cfg-item)
                  email (:email cfg-item)
                  msgs (map (comp gh/make-direct-text-message #(merge {:to email} %)) (:messages cfg-item))]]
      (apply gh/make-greenmail-user! *the-gm* email login pass msgs))
    (f)))

(defn get-message-map-from-config
  "Given a configuration map as the one used by make-custom-fixture-from-config in
  `cfg`, a user index (starting by 0) and a message index (starting by 0), it returns
  a map equal to the one that can be used as a parameter to `make-direct-text-message`.
  "
  [cfg user-idx message-idx]
  (let [partial-m (-> cfg (get user-idx) :messages (get message-idx))
        to (-> cfg (get user-idx) :email)]
    (merge {:to to} partial-m)))

(defn get-credentials-from-config
  "Given a configuration map and a user-index returns the credentials as a vector
  `[login pass]. Specifically designed to be used with `(apply clojure-mail.core/store`"
  [cfg user-idx]
  [(:login (get cfg user-idx)) (:pass (get cfg user-idx))])

(defn get-server-port
  "Return the server port that *the-gm* is using. See gh/get-greenmail-ports to see restrictions.
  `server-type` needs to be `:imap` or `:imaps`."
  [server-type]
  (server-type (gh/get-greenmail-ports *the-gm*)))
