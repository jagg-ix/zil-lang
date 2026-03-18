(ns zil.runtime.datascript
  "Draft DataScript runtime scaffold for Zil v0.1.

  This namespace intentionally stays small and explicit:
  - one connection creator,
  - tuple-fact and causal-edge transaction mappers,
  - snapshot extraction at a revision frontier,
  - causal order helpers via recursive Datalog rules."
  (:require [clojure.set :as cset]
            [clojure.string :as str]
            [datascript.core :as d]))

(def zil-schema
  "DataScript schema used by the draft runtime profile.

  Composite tuples enforce identity and simplify lookups:
  - :zil/fact-key = [object relation subject]
  - :zil/fact-at-rev = [object relation subject revision]
  - :zil/before-key = [left-event right-event]"
  {:zil/object {:db/index true}
   :zil/relation {:db/index true}
   :zil/subject {:db/index true}
   :zil/revision {:db/index true}
   :zil/event {:db/index true}
   :zil/op {:db/index true}
   :zil/fact-key {:db/tupleAttrs [:zil/object :zil/relation :zil/subject]
                    :db/unique :db.unique/identity}
   :zil/fact-at-rev {:db/tupleAttrs [:zil/object :zil/relation :zil/subject :zil/revision]
                       :db/unique :db.unique/identity}
   :zil/event-left {:db/index true}
   :zil/event-right {:db/index true}
   :zil/before-key {:db/tupleAttrs [:zil/event-left :zil/event-right]
                      :db/unique :db.unique/identity}})

(defn make-conn
  "Create a DataScript connection for Zil facts.
  Optional `extra-schema` may extend the base schema."
  ([] (make-conn {}))
  ([extra-schema]
   (d/create-conn (merge zil-schema extra-schema))))

(defn fact->tx
  "Convert a canonical Zil fact map into a DataScript transaction entity.

  Expected shape:
  {:object \"...\"
   :relation :keyword
   :subject \"...\"
   :attrs {...}        ; optional
   :revision 1
   :event :e1          ; optional but recommended
   :op :assert}        ; optional, defaults to :assert

  `:op :retract` provides snapshot-level retraction semantics."
  [{:keys [object relation subject attrs revision event op]
    :or {attrs {}
         op :assert}}]
  {:pre [(some? object)
         (keyword? relation)
         (some? subject)
         (integer? revision)]}
  (cond-> {:zil/object object
           :zil/relation relation
           :zil/subject subject
           :zil/attrs attrs
           :zil/revision revision
           :zil/op op}
    event (assoc :zil/event event)))

(defn before-edge->tx
  "Convert a causal edge into a DataScript transaction entity.

  {:left :e1 :right :e2} means before(e1, e2)."
  [{:keys [left right]}]
  {:pre [(keyword? left) (keyword? right)]}
  {:zil/event-left left
   :zil/event-right right})

(defn transact-facts!
  "Transact one or more canonical fact maps."
  [conn facts]
  (d/transact! conn (mapv fact->tx facts)))

(defn transact-before-edges!
  "Transact one or more causal-edge maps."
  [conn edges]
  (d/transact! conn (mapv before-edge->tx edges)))

(defn rows-at-or-before
  "Internal row pull for all fact revisions <= `frontier`."
  [db frontier]
  (d/q '[:find ?o ?r ?s ?rev ?op ?attrs
         :in $ ?frontier
         :where
         [?e :zil/object ?o]
         [?e :zil/relation ?r]
         [?e :zil/subject ?s]
         [?e :zil/revision ?rev]
         [(<= ?rev ?frontier)]
         [?e :zil/op ?op]
         [?e :zil/attrs ?attrs]]
       db frontier))

(defn latest-fact-state
  "Pick the latest row by revision per logical fact key [object relation subject]."
  [rows]
  (reduce
   (fn [acc [o r s rev op attrs]]
     (let [k [o r s]
           prev (get acc k)]
       (if (or (nil? prev) (> rev (:revision prev)))
         (assoc acc k {:object o
                       :relation r
                       :subject s
                       :revision rev
                       :op op
                       :attrs attrs})
         acc)))
   {}
   rows))

(defn facts-at-or-before
  "Materialize snapshot semantics at frontier revision.

  Rule:
  1. consider all rows with revision <= frontier
  2. keep only the highest-revision row per logical key
  3. include it iff its :op is :assert"
  [db frontier]
  (->> (rows-at-or-before db frontier)
       latest-fact-state
       vals
       (filter #(= :assert (:op %)))
       (sort-by (juxt :object :relation :subject))
       vec))

(def before-rules
  "Recursive Datalog rules for transitive closure of before(e1, e2)."
  '[[(before* ?x ?y)
     [?e :zil/event-left ?x]
     [?e :zil/event-right ?y]]
    [(before* ?x ?y)
     [?e :zil/event-left ?x]
     [?e :zil/event-right ?z]
     (before* ?z ?y)]])

(defn before?
  "True when left-event causally precedes right-event."
  [db left-event right-event]
  (boolean
   (d/q '[:find ?x .
          :in $ % ?left ?right
          :where
          (before* ?left ?right)
          [(identity true) ?x]]
        db before-rules left-event right-event)))

(defn concurrent?
  "True when events are not ordered by happens-before in either direction."
  [db e1 e2]
  (and (not= e1 e2)
       (not (before? db e1 e2))
       (not (before? db e2 e1))))

(defn q
  "Pass-through query helper."
  [query db & inputs]
  (apply d/q query db inputs))

(defn- normalize-vc-counter
  [v]
  (cond
    (integer? v) (long v)
    (number? v) (long v)
    (string? v) (Long/parseLong (str/trim v))
    :else (throw (ex-info "Vector clock counter must be numeric"
                          {:value v}))))

(defn normalize-vector-clock
  "Normalize vector-clock maps into {actor-string -> long-counter}."
  [vc]
  (when-not (map? vc)
    (throw (ex-info "Vector clock must be a map" {:value vc})))
  (reduce-kv
   (fn [out k v]
     (let [actor (cond
                   (keyword? k) (name k)
                   (string? k) k
                   :else (str k))]
       (assoc out actor (normalize-vc-counter v))))
   {}
   vc))

(defn vector-clock-before?
  "True iff vc-left happens-before vc-right under vector-clock ordering."
  [vc-left vc-right]
  (let [l (normalize-vector-clock vc-left)
        r (normalize-vector-clock vc-right)
        actors (cset/union (set (keys l)) (set (keys r)))
        non-greater? (every? (fn [a] (<= (get l a 0) (get r a 0))) actors)
        strictly-smaller? (some (fn [a] (< (get l a 0) (get r a 0))) actors)]
    (and non-greater? (boolean strictly-smaller?))))

(defn vector-clock-concurrent?
  "True iff vector clocks are incomparable in either direction."
  [vc-a vc-b]
  (and (not (vector-clock-before? vc-a vc-b))
       (not (vector-clock-before? vc-b vc-a))))

(defn derive-before-edges-from-vector-clocks
  "Derive causal edges from event+vector-clock records.

  Input records shape:
  {:event :event_key
   :vector_clock {\"actorA\" 2 \"actorB\" 1}}"
  [events]
  (->> (for [{le :event lvc :vector_clock} events
             {re :event rvc :vector_clock} events
             :when (and (keyword? le)
                        (keyword? re)
                        (not= le re)
                        (vector-clock-before? lvc rvc))]
         {:left le :right re})
       distinct
       vec))
