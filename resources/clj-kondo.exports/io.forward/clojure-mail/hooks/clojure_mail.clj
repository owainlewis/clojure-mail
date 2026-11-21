(ns hooks.clojure-mail
  (:require [clj-kondo.hooks-api :as api]))

(defn with-folder
  [{:keys [node]}]
  (let [[binding-vec & body] (rest (:children node))
        [sym & open-args] (:children binding-vec)
        new-node (api/list-node
                  (list*
                   (api/token-node 'let)
                   (api/vector-node
                    [sym (api/list-node
                          (list* (api/token-node 'clojure-mail.core/open-folder*)
                                 open-args))])
                   body))]
    {:node new-node}))
