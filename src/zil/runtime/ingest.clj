(ns zil.runtime.ingest
  "Ingest pipeline for datasource declarations.

  Phase 3 scope:
  - adapter registry dispatch
  - pull-mode read of datasource records
  - lowering records into canonical tuple facts
  - DataScript transact integration"
  (:require [clojure.string :as str]
            [zil.lower :as zl]
            [zil.runtime.adapters.command]
            [zil.runtime.adapters.core :as ac]
            [zil.runtime.adapters.file]
            [zil.runtime.adapters.rest]
            [zil.runtime.datascript :as zr]))

(defn datasource-declarations
  [compiled]
  (->> (:declarations compiled)
       (filter #(= :datasource (:kind %)))
       vec))

(defn declaration->datasource-spec
  [{:keys [name] :as decl}]
  (let [decl* (zl/normalized-declaration decl)]
    {:id (zl/entity-id :datasource name)
     :name name
     :kind :datasource
     :type (get-in decl* [:attrs :type])
     :attrs (:attrs decl*)}))

(defn- metric-id
  [v]
  (let [s (cond
            (string? v) v
            (keyword? v) (name v)
            :else (str v))]
    (if (str/includes? s ":")
      s
      (str "metric:" s))))

(defn- relation-key
  [k]
  (cond
    (keyword? k) k
    (string? k) (keyword (str/replace (str/lower-case k) #"[^a-z0-9_:\-\.]" "_"))
    :else (keyword (str k))))

(defn- envelope-fact
  [datasource-id record ingest-ts]
  {:object datasource-id
   :relation :ingested_record
   :subject "value:record"
   :attrs {:ingest_ts ingest-ts
           :payload record}})

(defn- metric-observation-facts
  [datasource-id record ingest-ts]
  (let [metric (:metric record)
        value (:value record)
        metrics (:metrics record)]
    (cond
      (and metric (contains? record :value))
      [{:object (metric-id metric)
        :relation :observed_from
        :subject datasource-id
        :attrs {:value value
                :ingest_ts ingest-ts}}]

      (map? metrics)
      (for [[mk mv] metrics]
        {:object (metric-id mk)
         :relation :observed_from
         :subject datasource-id
         :attrs {:value mv
                 :ingest_ts ingest-ts}})

      :else [])))

(defn- scalar-field-facts
  [datasource-id record ingest-ts]
  (for [[k v] record
        :when (and (not= k :metric)
                   (not= k :value)
                   (not= k :metrics))]
    {:object datasource-id
     :relation (relation-key k)
     :subject (zl/subject-value v)
     :attrs {:ingest_ts ingest-ts}}))

(defn record->facts
  [datasource-id record ingest-ts]
  (let [record* (if (map? record) record {:value record})]
    (vec
     (concat
      [(envelope-fact datasource-id record* ingest-ts)]
      (metric-observation-facts datasource-id record* ingest-ts)
      (when (map? record*)
        (scalar-field-facts datasource-id record* ingest-ts))))))

(defn ingest-datasource-once!
  "Run one datasource adapter once and transact resulting facts."
  ([conn datasource-spec] (ingest-datasource-once! conn datasource-spec {}))
  ([conn datasource-spec {:keys [revision event]
                          :or {revision nil}}]
   (let [ingest-ts (System/currentTimeMillis)
         records (ac/read-records datasource-spec {})
         facts (mapcat #(record->facts (:id datasource-spec) % ingest-ts) records)
         revision* (or revision ingest-ts)
         event* (or event
                    (keyword
                     (str "ingest_"
                          (str/replace (:id datasource-spec) #":" "_"))))
         tx-facts (mapv #(assoc % :revision revision* :event event*) facts)]
     (zr/transact-facts! conn tx-facts)
     {:datasource (:id datasource-spec)
      :type (:type datasource-spec)
      :records (count records)
      :facts (count tx-facts)})))

(defn ingest-all!
  "Run all DATASOURCE declarations from a compiled program."
  ([conn compiled] (ingest-all! conn compiled {}))
  ([conn compiled opts]
   (let [sources (mapv declaration->datasource-spec (datasource-declarations compiled))
         by-source (mapv #(ingest-datasource-once! conn % opts) sources)]
     {:sources (count sources)
      :records (reduce + 0 (map :records by-source))
      :facts (reduce + 0 (map :facts by-source))
      :by_source by-source})))

(defn poll-mode
  [datasource-spec]
  (ac/normalize-type
   (or (get-in datasource-spec [:attrs :poll_mode])
       :event)))

(defn poll-interval-ms
  [datasource-spec]
  (long
   (or (get-in datasource-spec [:attrs :poll_every_ms])
       (get-in datasource-spec [:attrs :interval_ms])
       5000)))

(defn- next-revision
  [state]
  (swap! state inc))

(defn start-poller!
  "Start ingest for one datasource.

  For `poll_mode=event`, runs once and returns a completed handle.
  For `poll_mode=interval`, starts a background future."
  ([conn datasource-spec] (start-poller! conn datasource-spec {}))
  ([conn datasource-spec {:keys [revision event on-error initial_revision]
                          :or {initial_revision (System/currentTimeMillis)}}]
   (let [mode (poll-mode datasource-spec)
         stats (atom {:runs 0 :errors 0 :last_error nil :last_result nil})
         stop? (atom false)]
     (if (= mode :interval)
       (let [interval-ms (poll-interval-ms datasource-spec)
             revision-state (atom (long initial_revision))
             fut (future
                   (while (not @stop?)
                     (try
                       (let [result (ingest-datasource-once!
                                     conn
                                     datasource-spec
                                     {:revision (or revision (next-revision revision-state))
                                      :event event})]
                         (swap! stats (fn [s]
                                        (-> s
                                            (update :runs inc)
                                            (assoc :last_result result)
                                            (assoc :last_error nil)))))
                       (catch Exception e
                         (swap! stats (fn [s]
                                        (-> s
                                            (update :errors inc)
                                            (assoc :last_error (.getMessage e)))))
                         (when on-error
                           (on-error e datasource-spec))))
                     (Thread/sleep interval-ms)))]
         {:datasource (:id datasource-spec)
          :mode :interval
          :interval_ms interval-ms
          :stats stats
          :stop (fn []
                  (reset! stop? true)
                  (future-cancel fut)
                  true)
          :future fut})
       (let [result (ingest-datasource-once!
                     conn
                     datasource-spec
                     {:revision revision
                      :event event})]
         (swap! stats assoc :runs 1 :last_result result)
         {:datasource (:id datasource-spec)
          :mode :event
          :interval_ms nil
          :stats stats
          :stop (fn [] true)
          :future nil})))))

(defn start-all-pollers!
  "Start pollers for all datasource declarations in a compiled program."
  ([conn compiled] (start-all-pollers! conn compiled {}))
  ([conn compiled opts]
   (let [sources (mapv declaration->datasource-spec (datasource-declarations compiled))
         handles (mapv #(start-poller! conn % opts) sources)]
     {:sources (count sources)
      :handles handles})))

(defn stop-poller!
  [handle]
  ((:stop handle)))

(defn stop-all-pollers!
  [handles]
  (doseq [h handles]
    (stop-poller! h))
  true)
