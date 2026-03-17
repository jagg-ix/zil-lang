(ns zil.lower
  "Lower higher-level declaration forms into canonical tuple facts."
  (:require [clojure.set :as cset]
            [clojure.string :as str]))

(def stdlib-kinds
  #{:service :host :datasource :metric :policy :event :tm_atom :lts_atom})

(def required-attrs
  "Minimal required attrs for stdlib declarations."
  {:datasource #{:type}
   :metric #{:source}
   :tm_atom #{:states :alphabet :blank :initial :accept :reject :transitions}
   :lts_atom #{:states :initial :transitions}})

(def enum-values
  "Enum validation by declaration kind and attr key."
  {:service {:criticality #{:low :high}
             :environment #{:dev :qa :prod :dr :cqa}
             :env #{:dev :qa :prod :dr :cqa}}
   :host {:environment #{:dev :qa :prod :dr :cqa}
          :type #{:physical :vm :container :pod :process}}
   :datasource {:type #{:rest :file :command :socket :websocket :pipe}
                :format #{:json :text :csv :edn :kv}
                :poll_mode #{:event :interval}}
   :event {:criticality #{:low :high}}
   :policy {:criticality #{:low :high}}})

(def service-relation-aliases
  {:depends :uses
   :depended_by :used_by})

(def dependency-relations
  #{:uses :used_by})

(defn entity-id
  "Stable string id for a declaration kind/name pair."
  [kind decl-name]
  (if (str/includes? decl-name ":")
    decl-name
    (str (clojure.core/name kind) ":" decl-name)))

(defn- enum-token
  [v]
  (let [s (cond
            (keyword? v) (clojure.core/name v)
            (string? v) v
            :else nil)]
    (when s
      (keyword (str/lower-case (str/trim s))))))

(defn- attr-values
  [v]
  (if (and (coll? v) (not (map? v)))
    v
    [v]))

(defn- merge-attr-values
  [a b]
  (vec (distinct (concat (attr-values a) (attr-values b)))))

(defn- normalize-attr-key
  [kind k]
  (let [k* (if (keyword? k) k (keyword (str k)))]
    (if (= kind :service)
      (get service-relation-aliases k* k*)
      k*)))

(defn- normalize-enum-value
  [kind attr-key v]
  (let [allowed (get-in enum-values [kind attr-key])]
    (if-not allowed
      v
      (let [token (enum-token v)]
        (when-not token
          (throw (ex-info "Enum value must be a keyword-like token"
                          {:kind kind :attr attr-key :value v})))
        (when-not (contains? allowed token)
          (throw (ex-info "Invalid enum value"
                          {:kind kind :attr attr-key :value v :allowed allowed})))
        token))))

(defn- normalize-attr-value
  [kind attr-key v]
  (let [vals (attr-values v)
        normalized (mapv #(normalize-enum-value kind attr-key %) vals)]
    (if (and (coll? v) (not (map? v)))
      normalized
      (first normalized))))

(defn- normalize-attrs
  [kind attrs]
  (reduce-kv
   (fn [m raw-k raw-v]
     (let [k (normalize-attr-key kind raw-k)
           v (normalize-attr-value kind k raw-v)]
       (if (and (contains? m k) (contains? dependency-relations k))
         (assoc m k (merge-attr-values (get m k) v))
         (assoc m k v))))
   {}
   attrs))

(defn subject-value
  "Map scalar values to canonical subject tokens."
  [v]
  (cond
    (string? v)
    (if (or (str/includes? v ":")
            (str/starts-with? v "?"))
      v
      (str "value:" v))

    (keyword? v) (str "value:" (clojure.core/name v))
    (number? v) (str "value:" v)
    (boolean? v) (str "value:" v)
    (nil? v) "value:nil"
    :else (str "value:" (pr-str v))))

(defn normalize-entity-ref
  "Normalize a reference to a typed entity id. If no prefix is present,
  use `default-kind` as namespace."
  [default-kind v]
  (let [s (cond
            (string? v) v
            (keyword? v) (clojure.core/name v)
            :else (str v))]
    (if (str/includes? s ":")
      s
      (str (clojure.core/name default-kind) ":" s))))

(defn- token->name
  [v]
  (cond
    (string? v) v
    (keyword? v) (clojure.core/name v)
    (symbol? v) (clojure.core/name v)
    :else (str v)))

(defn- token->move
  [v]
  (keyword
   (str/upper-case
    (token->name v))))

(defn- as-set
  [v]
  (set (map token->name (attr-values v))))

(defn- validate-tm-atom!
  [{:keys [name attrs]}]
  (let [states (as-set (:states attrs))
        alphabet (as-set (:alphabet attrs))
        blank (token->name (:blank attrs))
        initial (token->name (:initial attrs))
        accept (as-set (:accept attrs))
        reject (as-set (:reject attrs))
        transitions (:transitions attrs)]
    (when (empty? states)
      (throw (ex-info "TM_ATOM must define at least one state" {:name name})))
    (when (empty? alphabet)
      (throw (ex-info "TM_ATOM must define at least one alphabet symbol" {:name name})))
    (when-not (contains? alphabet blank)
      (throw (ex-info "TM_ATOM blank symbol must be part of alphabet"
                      {:name name :blank blank :alphabet alphabet})))
    (when-not (contains? states initial)
      (throw (ex-info "TM_ATOM initial state must be part of states"
                      {:name name :initial initial :states states})))
    (when (empty? accept)
      (throw (ex-info "TM_ATOM must define at least one accept state" {:name name})))
    (when (empty? reject)
      (throw (ex-info "TM_ATOM must define at least one reject state" {:name name})))
    (when-not (cset/subset? accept states)
      (throw (ex-info "TM_ATOM accept states must be included in states"
                      {:name name :accept accept :states states})))
    (when-not (cset/subset? reject states)
      (throw (ex-info "TM_ATOM reject states must be included in states"
                      {:name name :reject reject :states states})))
    (when (seq (cset/intersection accept reject))
      (throw (ex-info "TM_ATOM accept and reject sets must be disjoint"
                      {:name name :overlap (cset/intersection accept reject)})))
    (when-not (map? transitions)
      (throw (ex-info "TM_ATOM transitions must be a map"
                      {:name name :transitions transitions})))
    (doseq [[k v] transitions]
      (when-not (and (vector? k) (= 2 (count k)))
        (throw (ex-info "TM_ATOM transition key must be [state symbol]"
                        {:name name :key k})))
      (when-not (and (vector? v) (= 3 (count v)))
        (throw (ex-info "TM_ATOM transition value must be [next-state write-symbol move]"
                        {:name name :key k :value v})))
      (let [[from-state read-sym] k
            [to-state write-sym move] v
            from* (token->name from-state)
            read* (token->name read-sym)
            to* (token->name to-state)
            write* (token->name write-sym)
            move* (token->move move)]
        (when-not (contains? states from*)
          (throw (ex-info "TM_ATOM transition from-state not in states"
                          {:name name :from from* :states states})))
        (when-not (contains? states to*)
          (throw (ex-info "TM_ATOM transition to-state not in states"
                          {:name name :to to* :states states})))
        (when-not (contains? alphabet read*)
          (throw (ex-info "TM_ATOM transition read-symbol not in alphabet"
                          {:name name :read read* :alphabet alphabet})))
        (when-not (contains? alphabet write*)
          (throw (ex-info "TM_ATOM transition write-symbol not in alphabet"
                          {:name name :write write* :alphabet alphabet})))
        (when-not (contains? #{:L :R :N} move*)
          (throw (ex-info "TM_ATOM move must be one of :L :R :N"
                          {:name name :move move* :allowed #{:L :R :N}})))))
    ;; Completeness: every non-halting state has a transition for each alphabet symbol.
    (let [halting (cset/union accept reject)
          non-halting (cset/difference states halting)
          expected (set (for [s non-halting a alphabet] [s a]))
          actual (set (for [[k _] transitions]
                        [(token->name (first k))
                         (token->name (second k))]))
          missing (cset/difference expected actual)
          extra (cset/difference actual expected)]
      (when (seq missing)
        (throw (ex-info "TM_ATOM transitions are incomplete"
                        {:name name :missing missing})))
      (when (seq extra)
        (throw (ex-info "TM_ATOM transitions include keys outside non-halting transition domain"
                        {:name name :extra extra}))))
    true))

(defn- validate-lts-atom!
  [{:keys [name attrs]}]
  (let [states (as-set (:states attrs))
        initial (token->name (:initial attrs))
        transitions (:transitions attrs)]
    (when (empty? states)
      (throw (ex-info "LTS_ATOM must define at least one state" {:name name})))
    (when-not (contains? states initial)
      (throw (ex-info "LTS_ATOM initial state must be part of states"
                      {:name name :initial initial :states states})))
    (when-not (map? transitions)
      (throw (ex-info "LTS_ATOM transitions must be a map"
                      {:name name :transitions transitions})))
    (when (empty? transitions)
      (throw (ex-info "LTS_ATOM must define at least one transition"
                      {:name name})))
    (doseq [[k v] transitions]
      (when-not (and (vector? k) (= 2 (count k)))
        (throw (ex-info "LTS_ATOM transition key must be [state label]"
                        {:name name :key k})))
      (when-not (and (vector? v) (<= 1 (count v) 2))
        (throw (ex-info "LTS_ATOM transition value must be [next-state] or [next-state effect]"
                        {:name name :key k :value v})))
      (let [[from-state label] k
            [to-state effect] v
            from* (token->name from-state)
            label* (token->name label)
            to* (token->name to-state)]
        (when-not (contains? states from*)
          (throw (ex-info "LTS_ATOM transition from-state not in states"
                          {:name name :from from* :states states})))
        (when-not (contains? states to*)
          (throw (ex-info "LTS_ATOM transition to-state not in states"
                          {:name name :to to* :states states})))
        (when (str/blank? label*)
          (throw (ex-info "LTS_ATOM transition label must be non-empty"
                          {:name name :key k})))
        (when (and effect (str/blank? (token->name effect)))
          (throw (ex-info "LTS_ATOM transition effect must be non-empty when provided"
                          {:name name :key k :value v})))))
    true))

(defn- required-attr-check!
  [{:keys [kind attrs name]}]
  (let [required (get required-attrs kind #{})
        missing (->> required (remove #(contains? attrs %)) vec)]
    (when (seq missing)
      (throw (ex-info "Missing required attributes for declaration"
                      {:kind kind :name name :missing missing})))))

(defn validate-declaration!
  [{:keys [kind name attrs] :as decl}]
  (when-not (contains? stdlib-kinds kind)
    (throw (ex-info "Unsupported declaration kind"
                    {:kind kind :declaration decl})))
  (when (str/blank? (str name))
    (throw (ex-info "Declaration name must be non-empty"
                    {:declaration decl})))
  (when-not (map? attrs)
    (throw (ex-info "Declaration attrs must be a map"
                    {:declaration decl})))
  ;; Normalization is run here to validate enum-compatible fields early.
  (normalize-attrs kind attrs)
  (required-attr-check! decl)
  (when (= kind :tm_atom)
    (validate-tm-atom! {:name name :attrs attrs}))
  (when (= kind :lts_atom)
    (validate-lts-atom! {:name name :attrs attrs}))
  true)

(defn normalized-declaration
  "Normalize one declaration after validating it."
  [{:keys [kind name attrs] :as decl}]
  (validate-declaration! decl)
  {:kind kind
   :name name
   :attrs (normalize-attrs kind attrs)})

(defn- declaration-index
  [declarations]
  (into {}
        (map (fn [decl]
               [(entity-id (:kind decl) (:name decl)) decl]))
        declarations))

(defn- relation-refs
  [attrs rel]
  (map #(normalize-entity-ref :service %)
       (attr-values (get attrs rel []))))

(defn- validate-service-dependencies!
  [declarations]
  (let [idx (declaration-index declarations)]
    (doseq [{:keys [kind name attrs]} declarations
            :when (= kind :service)
            :let [entity (entity-id kind name)
                  attrs* (normalize-attrs kind attrs)]
            rel dependency-relations
            ref (relation-refs attrs* rel)]
      (let [target (get idx ref)]
        (when-not target
          (throw (ex-info "Service dependency reference not found"
                          {:service entity :relation rel :target ref})))
        (when-not (= :service (:kind target))
          (throw (ex-info "Service dependency must target a SERVICE declaration"
                          {:service entity :relation rel :target ref :target-kind (:kind target)})))))))

(defn- metric-source-target
  [v]
  (when (string? v)
    (let [i (str/index-of v ":")]
      (when (and i (pos? i) (< i (dec (count v))))
        v))))

(defn- validate-metric-sources!
  [declarations]
  (let [idx (declaration-index declarations)]
    (doseq [{:keys [kind name attrs]} declarations
            :when (= kind :metric)
            :let [entity (entity-id kind name)
                  srcs (attr-values (:source (normalize-attrs kind attrs)))]
            src srcs
            :let [target-id (metric-source-target src)]
            :when target-id]
      (let [target (get idx target-id)]
        (when-not target
          (throw (ex-info "Metric source reference not found"
                          {:metric entity :source target-id})))
        (when-not (= :datasource (:kind target))
          (throw (ex-info "Metric source must reference a DATASOURCE declaration"
                          {:metric entity :source target-id :target-kind (:kind target)})))))))

(defn- service-use-edges
  [declarations]
  (let [services (filter #(= :service (:kind %)) declarations)]
    (into {}
          (map (fn [{:keys [kind name attrs]}]
                 (let [entity (entity-id kind name)
                       attrs* (normalize-attrs kind attrs)]
                   [entity (set (relation-refs attrs* :uses))])))
          services)))

(defn- has-cycle?
  [edges]
  (let [nodes (set (concat (keys edges) (mapcat identity (vals edges))))
        indeg (reduce
               (fn [m to]
                 (update m to (fnil inc 0)))
               (zipmap nodes (repeat 0))
               (mapcat identity (vals edges)))
        queue (into clojure.lang.PersistentQueue/EMPTY
                    (filter #(zero? (get indeg % 0)) nodes))]
    (loop [q queue
           deg indeg
           seen 0]
      (if (empty? q)
        (< seen (count nodes))
        (let [n (peek q)
              q* (pop q)
              [deg* q**]
              (reduce
               (fn [[d qq] to]
                 (let [d* (update d to dec)]
                   (if (zero? (get d* to))
                     [d* (conj qq to)]
                     [d* qq])))
               [deg q*]
               (get edges n #{}))]
          (recur q** deg* (inc seen)))))))

(defn- validate-service-graph!
  [declarations]
  (let [edges (service-use-edges declarations)]
    (when (has-cycle? edges)
      (throw (ex-info "Service dependency graph has at least one cycle"
                      {:edges edges}))))
  true)

(defn validate-declarations!
  [declarations]
  (doseq [decl declarations]
    (validate-declaration! decl))
  (let [dupes (->> declarations
                   (group-by (juxt :kind :name))
                   (filter (fn [[_ xs]] (> (count xs) 1)))
                   (map first)
                   vec)]
    (when (seq dupes)
      (throw (ex-info "Duplicate declarations found"
                      {:duplicates dupes}))))
  (validate-service-dependencies! declarations)
  (validate-service-graph! declarations)
  (validate-metric-sources! declarations)
  true)

(defn- dependency-facts
  [entity relation refs]
  (let [inverse (if (= relation :uses) :used_by :uses)]
    (mapcat (fn [ref]
              (cond-> [{:object entity
                        :relation relation
                        :subject ref
                        :attrs {}}
                       {:object ref
                        :relation inverse
                        :subject entity
                        :attrs {}}]
                (= relation :uses)
                (conj {:object entity
                       :relation :depends_on
                       :subject ref
                       :attrs {}})))
            refs)))

(defn- tm-transition-facts
  [entity transitions]
  (->> transitions
       (sort-by (fn [[k _]]
                  [(token->name (first k))
                   (token->name (second k))]))
       (map-indexed
        (fn [idx [k v]]
          (let [[from-state read-sym] k
                [to-state write-sym move] v]
            {:object entity
             :relation :transition
             :subject (str "tmtr:" idx)
             :attrs {:from_state (token->name from-state)
                     :read_symbol (token->name read-sym)
                     :to_state (token->name to-state)
                     :write_symbol (token->name write-sym)
                     :move (token->move move)}})))))

(defn- lts-edge-facts
  [entity transitions]
  (->> transitions
       (sort-by (fn [[k _]]
                  [(token->name (first k))
                   (token->name (second k))]))
       (map-indexed
        (fn [idx [k v]]
          (let [[from-state label] k
                [to-state effect] v]
            {:object entity
             :relation :edge
             :subject (str "ltsedge:" idx)
             :attrs (cond-> {:from_state (token->name from-state)
                             :label (token->name label)
                             :to_state (token->name to-state)}
                      effect (assoc :effect (token->name effect)))})))))

(defn declaration->facts
  "Lower one stdlib declaration map into canonical facts.

  Input shape:
  {:kind :service
   :name \"payment\"
   :attrs {:env \"prod\" :criticality \"HIGH\"}}"
  [{:keys [kind name attrs] :as decl}]
  (validate-declaration! decl)
  (let [entity (entity-id kind name)
        attrs* (normalize-attrs kind attrs)]
    (->> (concat
          [{:object entity
            :relation :kind
            :subject (str "entity:" (clojure.core/name kind))
            :attrs {}}]
          (mapcat (fn [[k v]]
                    (cond
                      (and (= kind :service)
                           (contains? dependency-relations k))
                      (dependency-facts entity k (relation-refs attrs* k))

                      (and (= kind :tm_atom) (= k :transitions))
                      (tm-transition-facts entity v)

                      (and (= kind :lts_atom) (= k :transitions))
                      (lts-edge-facts entity v)

                      :else
                      (for [sv (attr-values v)]
                        {:object entity
                         :relation k
                         :subject (subject-value sv)
                         :attrs {}})))
                  attrs*))
         distinct
         vec)))
