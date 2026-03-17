(ns zil.core
  "Core parser/evaluator for Zil v0.1.

  Supported surface syntax:
  - MODULE declarations
  - tuple facts: object#relation@subject [attrs].
  - rules with IF/THEN and AND/NOT conjunctions
  - queries with FIND ... WHERE ..."
  (:require [clojure.edn :as edn]
            [clojure.set :as cset]
            [clojure.string :as str]
            [zil.runtime.datascript :as zr]))

(defn variable-token?
  "True when `x` is a query/rule variable (e.g. \"?x\")."
  [x]
  (and (string? x)
       (str/starts-with? x "?")
       (> (count x) 1)))

(defn parse-scalar
  "Parse scalar literals used in attrs:
  - quoted strings
  - booleans
  - ints / doubles
  - fallback raw string token."
  [s]
  (let [t (str/trim s)]
    (cond
      (re-matches #"\"(?:\\.|[^\"])*\"" t) (edn/read-string t)
      (re-matches #"(?i)true|false" t) (Boolean/parseBoolean (str/lower-case t))
      (re-matches #"-?\d+" t) (Long/parseLong t)
      (re-matches #"-?\d+\.\d+" t) (Double/parseDouble t)
      :else t)))

(defn parse-term
  "Parse an object/subject/attr value token."
  [s]
  (parse-scalar (str/trim s)))

(defn parse-attrs
  "Parse attrs payload from inside `[...]`."
  [attrs-str]
  (if (or (nil? attrs-str) (str/blank? attrs-str))
    {}
    (let [m (re-matcher #"\s*([A-Za-z0-9_:\-\.]+)\s*=\s*(\"(?:\\.|[^\"])*\"|[^,\]]+)\s*(?:,|$)"
                        attrs-str)]
      (loop [out {}]
        (if (.find m)
          (recur (assoc out (keyword (.group m 1)) (parse-term (.group m 2))))
          out)))))

(def atom-re
  #"^\s*(.+?)#([A-Za-z0-9_:\-\.]+)@(.+?)(?:\s*\[(.*)\])?\s*$")

(defn trim-trailing-dot
  [s]
  (-> s str/trim (str/replace #"\.\s*$" "")))

(defn parse-atom
  "Parse one tuple atom (without NOT)."
  [s]
  (let [raw (trim-trailing-dot s)]
    (when-not (str/includes? raw "#")
      (throw (ex-info "Invalid atom: missing `#` separator" {:text s})))
    (when-not (str/includes? raw "@")
      (throw (ex-info "Invalid atom: missing `@` separator" {:text s})))
    (let [[_ object relation subject attrs] (or (re-matches atom-re raw)
                                                (throw (ex-info "Invalid atom syntax" {:text s})))]
      {:object (parse-term object)
       :relation (keyword (str/trim relation))
       :subject (parse-term subject)
       :attrs (parse-attrs attrs)})))

(defn split-and
  [s]
  (->> (str/split s #"(?i)\s+AND\s+")
       (map str/trim)
       (remove str/blank?)
       vec))

(defn parse-literal
  "Parse body literal; supports optional `NOT` prefix."
  [s]
  (let [t (str/trim s)
        neg? (boolean (re-find #"(?i)^NOT\s+" t))
        atom-s (if neg?
                 (str/replace-first t #"(?i)^NOT\s+" "")
                 t)]
    (assoc (parse-atom atom-s) :neg? neg?)))

(defn parse-rule-body
  [s]
  (->> (split-and s)
       (mapv parse-literal)))

(defn parse-rule-head
  [s]
  (->> (split-and s)
       (mapv parse-atom)))

(defn parse-find-line
  [line]
  (let [[_ vars where] (or (re-matches #"(?i)^FIND\s+(.+?)\s+WHERE\s+(.+)\.\s*$" (str/trim line))
                           (throw (ex-info "Invalid QUERY FIND syntax" {:line line})))
        find-vars (->> (str/split (str/trim vars) #"\s+")
                       (mapv str/trim))]
    {:find find-vars
     :where (parse-rule-body where)}))

(defn preprocess-lines
  "Remove line comments and blank lines."
  [text]
  (->> (str/split-lines text)
       (map #(str/replace % #"\s*//.*$" ""))
       (map str/trim)
       (remove str/blank?)
       vec))

(defn parse-program
  "Parse Zil source text into canonical structural IR."
  [text]
  (let [lines (preprocess-lines text)
        n (count lines)]
    (loop [i 0
           out {:module nil :facts [] :rules [] :queries []}]
      (if (>= i n)
        out
        (let [line (nth lines i)]
          (cond
            (re-matches #"(?i)^MODULE\s+[A-Za-z0-9_.:-]+\.\s*$" line)
            (let [[_ mod] (re-matches #"(?i)^MODULE\s+([A-Za-z0-9_.:-]+)\.\s*$" line)]
              (recur (inc i) (assoc out :module mod)))

            (re-matches #"(?i)^RULE\s+[A-Za-z0-9_.:-]+\s*:\s*$" line)
            (let [[_ name] (re-matches #"(?i)^RULE\s+([A-Za-z0-9_.:-]+)\s*:\s*$" line)
                  if-line (or (get lines (inc i))
                              (throw (ex-info "RULE missing IF line" {:rule name})))
                  then-line (or (get lines (+ i 2))
                                (throw (ex-info "RULE missing THEN line" {:rule name})))
                  _ (when-not (re-matches #"(?i)^IF\s+.+$" if-line)
                      (throw (ex-info "RULE IF line has invalid syntax" {:rule name :line if-line})))
                  _ (when-not (re-matches #"(?i)^THEN\s+.+\.\s*$" then-line)
                      (throw (ex-info "RULE THEN line has invalid syntax" {:rule name :line then-line})))
                  if-body (str/replace-first if-line #"(?i)^IF\s+" "")
                  then-body (str/replace-first then-line #"(?i)^THEN\s+" "")
                  rule {:name name
                        :if (parse-rule-body if-body)
                        :then (parse-rule-head then-body)}]
              (recur (+ i 3) (update out :rules conj rule)))

            (re-matches #"(?i)^QUERY\s+[A-Za-z0-9_.:-]+\s*:\s*$" line)
            (let [[_ name] (re-matches #"(?i)^QUERY\s+([A-Za-z0-9_.:-]+)\s*:\s*$" line)
                  find-line (or (get lines (inc i))
                                (throw (ex-info "QUERY missing FIND line" {:query name})))
                  q (assoc (parse-find-line find-line) :name name)]
              (recur (+ i 2) (update out :queries conj q)))

            (re-matches #".+\.\s*$" line)
            (recur (inc i) (update out :facts conj (parse-atom line)))

            :else
            (throw (ex-info "Unrecognized line while parsing program" {:line line :index i}))))))))

(defn literal-vars
  [{:keys [object subject attrs]}]
  (into #{}
        (concat (when (variable-token? object) [object])
                (when (variable-token? subject) [subject])
                (for [[_ v] attrs :when (variable-token? v)] v))))

(defn- ensure-rule-safety!
  [{:keys [name if then]}]
  (let [pos-lits (filterv (complement :neg?) if)
        neg-lits (filterv :neg? if)
        pos-vars (into #{} (mapcat literal-vars) pos-lits)
        neg-vars (into #{} (mapcat literal-vars) neg-lits)
        head-vars (into #{} (mapcat literal-vars) then)]
    (when (not (cset/subset? neg-vars pos-vars))
      (throw (ex-info "Unsafe negation: NOT literal uses unbound variables"
                      {:rule name :negative-vars neg-vars :positive-vars pos-vars})))
    (when (not (cset/subset? head-vars pos-vars))
      (throw (ex-info "Rule head contains variables not bound in positive body"
                      {:rule name :head-vars head-vars :positive-vars pos-vars})))))

(defn- rule-relations
  [{:keys [if then]}]
  {:head (into #{} (map :relation) then)
   :pos (into #{} (map :relation) (remove :neg? if))
   :neg (into #{} (map :relation) (filter :neg? if))})

(defn- relax-strata
  [strata edges]
  (reduce
   (fn [m [from to w]]
     (let [cand (+ (get m from 0) w)]
       (if (> cand (get m to 0))
         (assoc m to cand)
         m)))
   strata
   edges))

(defn stratify-rules
  "Compute stratum index per relation.
  Throws when rules are not stratifiable."
  [rules]
  (let [rels (into #{}
                   (mapcat (fn [r]
                             (let [{:keys [head pos neg]} (rule-relations r)]
                               (concat head pos neg)))
                           rules))
        base (zipmap rels (repeat 0))
        edges (mapcat (fn [r]
                        (let [{:keys [head pos neg]} (rule-relations r)]
                          (concat
                           (for [h head, p pos] [p h 0])
                           (for [h head, n neg] [n h 1]))))
                      rules)
        n (count rels)
        sN (nth (iterate #(relax-strata % edges) base) (max 1 n))
        sN1 (relax-strata sN edges)]
    (when (not= sN sN1)
      (throw (ex-info "Program is not stratifiable (negative cycle detected)" {})))
    sN))

(defn compile-program
  "Parse + validate + stratify a Zil program."
  [text]
  (let [{:keys [module facts rules queries] :as parsed} (parse-program text)]
    (when (str/blank? module)
      (throw (ex-info "Program must define MODULE <name>." {})))
    (doseq [r rules] (ensure-rule-safety! r))
    (let [strata (stratify-rules rules)
          rules* (mapv (fn [r]
                         (let [{:keys [head]} (rule-relations r)
                               body (vec (concat (remove :neg? (:if r))
                                                 (filter :neg? (:if r))))]
                           (assoc r
                                  :if body
                                  :stratum (apply max 0 (map #(get strata % 0) head)))))
                       rules)]
      (assoc parsed :rules rules* :strata strata :queries queries :facts facts))))

(defn- bind-var
  [env var val]
  (if (contains? env var)
    (when (= (get env var) val) env)
    (assoc env var val)))

(defn- match-term
  [env pattern value]
  (if (variable-token? pattern)
    (bind-var env pattern value)
    (when (= pattern value) env)))

(defn- match-attrs
  [env pat-attrs fact-attrs]
  (reduce-kv
   (fn [acc k v]
     (when acc
       (when (contains? fact-attrs k)
         (match-term acc v (get fact-attrs k)))))
   env
   pat-attrs))

(defn- match-positive
  [facts-by-rel literal env]
  (let [candidates (get facts-by-rel (:relation literal) [])]
    (for [fact candidates
          :let [e1 (match-term env (:object literal) (:object fact))]
          :when e1
          :let [e2 (match-term e1 (:subject literal) (:subject fact))]
          :when e2
          :let [e3 (match-attrs e2 (:attrs literal) (:attrs fact))]
          :when e3]
      e3)))

(defn- has-match?
  [facts-by-rel literal env]
  (boolean (seq (match-positive facts-by-rel literal env))))

(defn- eval-body
  [pos-facts-by-rel neg-facts-by-rel body]
  (loop [envs [{}]
         lits body]
    (if (empty? lits)
      envs
      (let [{:keys [neg?] :as lit} (first lits)
            next-envs (if neg?
                        (filterv #(not (has-match? neg-facts-by-rel (dissoc lit :neg?) %))
                                 envs)
                        (vec (mapcat #(match-positive pos-facts-by-rel lit %) envs)))]
        (recur next-envs (rest lits))))))

(defn- ground-term
  [env t]
  (if (variable-token? t)
    (or (get env t)
        (throw (ex-info "Unbound variable while grounding fact" {:var t :env env})))
    t))

(defn- ground-fact
  [env {:keys [object relation subject attrs]}]
  {:object (ground-term env object)
   :relation relation
   :subject (ground-term env subject)
   :attrs (reduce-kv (fn [m k v] (assoc m k (ground-term env v))) {} attrs)})

(defn- facts->index
  [facts]
  (group-by :relation facts))

(defn- apply-rule
  [rule pos-index neg-index]
  (let [envs (eval-body pos-index neg-index (:if rule))]
    (into #{}
          (mapcat (fn [env] (map #(ground-fact env %) (:then rule))) envs))))

(defn- eval-stratum
  [base-facts rules]
  (loop [current (set base-facts)]
    (let [pos-index (facts->index current)
          neg-index (facts->index base-facts)
          derived (into #{}
                        (mapcat #(apply-rule % pos-index neg-index) rules))
          new-facts (cset/difference derived current)]
      (if (empty? new-facts)
        (cset/difference current (set base-facts))
        (recur (into current new-facts))))))

(defn run-query
  "Evaluate one query against a fact set.
  Returns {:vars [...] :rows [[...]]}."
  [facts {:keys [find where]}]
  (let [index (facts->index facts)
        envs (eval-body index index where)
        rows (->> envs
                  (mapv (fn [env] (mapv #(get env %) find)))
                  distinct
                  vec)]
    {:vars find :rows rows}))

(defn execute-compiled
  "Execute compiled program and return final facts + query results."
  [{:keys [module facts rules queries]}]
  (let [strata (sort (distinct (map :stratum rules)))
        by-stratum (group-by :stratum rules)
        final-facts (reduce
                     (fn [acc s]
                       (let [derived (eval-stratum acc (get by-stratum s []))]
                         (into acc derived)))
                     (set facts)
                     strata)
        query-results (into {}
                            (for [q queries]
                              [(:name q) (run-query final-facts q)]))]
    {:module module
     :facts (->> final-facts
                 (sort-by (juxt :object :relation :subject))
                 vec)
     :queries query-results}))

(defn execute-program
  "Parse + compile + execute Zil source text."
  [text]
  (-> text
      compile-program
      execute-compiled))

(defn execute-file
  "Read and execute Zil source from file path."
  [path]
  (execute-program (slurp path)))

(defn materialize-datascript
  "Execute program and materialize resulting facts in a DataScript conn.

  Returns {:conn ..., :result ...}."
  ([text] (materialize-datascript text {}))
  ([text {:keys [revision event] :or {revision 0}}]
   (let [result (execute-program text)
         conn (zr/make-conn)
         tx-facts (mapv (fn [{:keys [object relation subject attrs]}]
                          (cond-> {:object object
                                   :relation relation
                                   :subject subject
                                   :attrs attrs
                                   :revision revision}
                            event (assoc :event event)))
                        (:facts result))]
     (zr/transact-facts! conn tx-facts)
     {:conn conn :result result})))
