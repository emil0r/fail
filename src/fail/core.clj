(ns fail.core
  (:require [com.stuartsierra.component :as component]
            [crux.api :as crux]
            [me.raynes.fs :as fs]
            [taoensso.timbre :as log]
            [tick.alpha.api :as t]))


(defn save-data [db data]
  (let [data (assoc data :crux.db/id (:data/id data))]
    (if (empty? (crux/q (crux/db (:node db))
                        {:find '[?e]
                         :where '[[?e :crux.db/id ?data-id]]
                         :args [{'?data-id (:data/id data)}]}))
      (crux/await-tx (:node db)  (crux/submit-tx (:node db) [[:crux.tx/put data]]) #time/duration "PT1S")
      (let [entity (crux/entity (crux/db (:node db)) (:crux.db/id data))]
        (when-not (= entity data)
          (crux/await-tx (:node db) (crux/submit-tx (:node db) [[:crux.tx/put data]]) #time/duration "PT1S")))))
  ;; return our data
  data)

(defn list-data [db {:keys [offset limit] :or {offset 0 limit 100}}]
  (->> {:find '[?e]
        :where '[[?e :data/id ?n]
                 [?n :data/name]]}
       (crux/q (crux/db (:node db)))
       (map (fn [[eid]] (crux/entity (crux/db (:node db)) eid)))))

(defn load-data [db id]
  (crux/entity (crux/db (:node db)) id))

(defn delete-data [db id]
  (let [ids (crux/q (crux/db (:node db))
                    {:find '[?e]
                     :where '[[?id :data/id data-id]
                              [?e :crux.db/id ?id]]
                     :args [{'data-id id}]})]
    (crux/submit-tx (:node db) (map #(cons :crux.tx/evict %) ids))))



(defrecord Database [started? node]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting database")
          (fs/mkdirs "data")
          (let [node (crux/start-node
                      {:crux.node/topology :crux.jdbc/topology
                       :crux.jdbc/dbtype "h2"
                       :crux.jdbc/dbname "cruxdb"
                       :crux.kv/db-dir "data"})]
            (assoc this
                   :node node
                   :started? true)))))
  (stop [this]
    (if-not started?
      this
      (do
        (log/info "Stopping database")
        (.close node)
        (assoc this
               :started? false
               :node nil)))))


(defn database [settings]
  (map->Database settings))


(defn start-db []
  (let [db (-> {}
               (database)
               (component/start))]
    db))

(defn stop-db [db]
  (component/stop db))


(do
  (log/info "database")
  (let [db (start-db)
        expected-result {:crux.db/id :test
                         :data/id :foobar
                         :data/test [:test :data :here "in" "various" ::forms]}
        data (dissoc expected-result :crux.db/id)]
    (try
      (log/info "save data")
      (if (= (save-data db data)
             data)
        (log/info "Success: save data")
        (log/warn "Failure: save data"))
      (log/info "list data")
      (if (= (list-data db nil)
             [expected-result])
        (log/info "Success: list data")
        (log/warn "Failure: list data"))
      (log/info "load data")
      (if (= (load-data db (:data/id data))
             expected-result)
        (log/info "Success: load data")
        (log/warn "Failure: load data"))
      (log/info "delete data")
      (if (= (do (crux/await-tx (:node db) (delete-data db (:data/id data)) #time/duration "PT1S")
                 (list-data db nil))
             [])
        (log/info "Success: delete data")
        (log/warn "Failure: delete data"))
      (finally (stop-db db)))))

