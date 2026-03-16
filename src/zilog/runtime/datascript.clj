(ns zilog.runtime.datascript
  "Draft DataScript runtime scaffold for Zilog v0.1.

  This namespace intentionally stays small and explicit:
  - one connection creator,
  - tuple-fact and causal-edge transaction mappers,
  - snapshot extraction at a revision frontier,
  - causal order helpers via recursive Datalog rules."
  (:require [datascript.core :as d]))

(def zilog-schema
  "DataScript schema used by the draft runtime profile.

  Composite tuples enforce identity and simplify lookups:
  - :zilog/fact-key = [object relation subject]
  - :zilog/fact-at-rev = [object relation subject revision]
  - :zilog/before-key = [left-event right-event]"
  {:zilog/object {:db/index true}
   :zilog/relation {:db/index true}
   :zilog/subject {:db/index true}
   :zilog/revision {:db/index true}
   :zilog/event {:db/index true}
   :zilog/op {:db/index true}
   :zilog/fact-key {:db/tupleAttrs [:zilog/object :zilog/relation :zilog/subject]
                    :db/unique :db.unique/identity}
   :zilog/fact-at-rev {:db/tupleAttrs [:zilog/object :zilog/relation :zilog/subject :zilog/revision]
                       :db/unique :db.unique/identity}
   :zilog/event-left {:db/index true}
   :zilog/event-right {:db/index true}
   :zilog/before-key {:db/tupleAttrs [:zilog/event-left :zilog/event-right]
                      :db/unique :db.unique/identity}})

(defn make-conn
  "Create a DataScript connection for Zilog facts.
  Optional `extra-schema` may extend the base schema."
  ([] (make-conn {}))
  ([extra-schema]
   (d/create-conn (merge zilog-schema extra-schema))))

(defn fact->tx
  "Convert a canonical Zilog fact map into a DataScript transaction entity.

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
  (cond-> {:zilog/object object
           :zilog/relation relation
           :zilog/subject subject
           :zilog/attrs attrs
           :zilog/revision revision
           :zilog/op op}
    event (assoc :zilog/event event)))

(defn before-edge->tx
  "Convert a causal edge into a DataScript transaction entity.

  {:left :e1 :right :e2} means before(e1, e2)."
  [{:keys [left right]}]
  {:pre [(keyword? left) (keyword? right)]}
  {:zilog/event-left left
   :zilog/event-right right})

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
         [?e :zilog/object ?o]
         [?e :zilog/relation ?r]
         [?e :zilog/subject ?s]
         [?e :zilog/revision ?rev]
         [(<= ?rev ?frontier)]
         [?e :zilog/op ?op]
         [?e :zilog/attrs ?attrs]]
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
     [?e :zilog/event-left ?x]
     [?e :zilog/event-right ?y]]
    [(before* ?x ?y)
     [?e :zilog/event-left ?x]
     [?e :zilog/event-right ?z]
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
