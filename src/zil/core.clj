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
            [zil.lower :as zl]
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
  - keywords / collections via safe EDN parse
  - fallback raw string token."
  [s]
  (let [t (str/trim s)]
    (cond
      (re-matches #"\"(?:\\.|[^\"])*\"" t) (edn/read-string t)
      (re-matches #"(?i)true|false" t) (Boolean/parseBoolean (str/lower-case t))
      (re-matches #"-?\d+" t) (Long/parseLong t)
      (re-matches #"-?\d+\.\d+" t) (Double/parseDouble t)
      (= "nil" t) nil
      (or (str/starts-with? t ":")
          (str/starts-with? t "[")
          (str/starts-with? t "{")
          (str/starts-with? t "(")
          (str/starts-with? t "#{"))
      (try
        (edn/read-string t)
        (catch Exception _
          t))
      :else t)))

(defn parse-term
  "Parse an object/subject/attr value token."
  [s]
  (parse-scalar (str/trim s)))

(defn split-top-level-comma
  "Split comma-separated text while respecting nested delimiters and quoted strings."
  [s context]
  (let [in (str/trim (or s ""))]
    (if (str/blank? in)
      []
      (let [final
            (loop [chs (seq in)
                   acc []
                   token ""
                   depth 0
                   in-string? false
                   escape? false]
              (if-let [c (first chs)]
                (cond
                  escape?
                  (recur (next chs) acc (str token c) depth in-string? false)

                  in-string?
                  (cond
                    (= c \\) (recur (next chs) acc (str token c) depth in-string? true)
                    (= c \") (recur (next chs) acc (str token c) depth false false)
                    :else (recur (next chs) acc (str token c) depth in-string? false))

                  (= c \")
                  (recur (next chs) acc (str token c) depth true false)

                  (#{\( \[ \{} c)
                  (recur (next chs) acc (str token c) (inc depth) in-string? false)

                  (#{\) \] \}} c)
                  (do
                    (when (zero? depth)
                      (throw (ex-info "Unbalanced closing delimiter"
                                      {:context context :text s})))
                    (recur (next chs) acc (str token c) (dec depth) in-string? false))

                  (and (= c \,) (zero? depth))
                  (recur (next chs) (conj acc (str/trim token)) "" depth in-string? false)

                  :else
                  (recur (next chs) acc (str token c) depth in-string? false))
                (do
                  (when in-string?
                    (throw (ex-info "Unterminated string literal"
                                    {:context context :text s})))
                  (when (not (zero? depth))
                    (throw (ex-info "Unbalanced delimiters"
                                    {:context context :text s})))
                  (conj acc (str/trim token)))))]
        (when (some str/blank? final)
          (throw (ex-info "Empty entry in comma-separated list"
                          {:context context :text s})))
        final))))

(defn parse-attrs
  "Parse attrs payload from inside `[...]`."
  [attrs-str]
  (if (or (nil? attrs-str) (str/blank? attrs-str))
    {}
    (reduce
     (fn [out token]
       (let [[_ k raw-v] (or (re-matches #"\s*([A-Za-z0-9_:\-\.]+)\s*=\s*(.+?)\s*$" token)
                             (throw (ex-info "Invalid attr entry, expected key=value"
                                             {:entry token :attrs attrs-str})))]
         (assoc out (keyword k) (parse-term raw-v))))
     {}
     (split-top-level-comma attrs-str "attrs"))))

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
  (letfn [(strip-line-comment [line]
            (loop [chs (seq line)
                   out ""
                   in-string? false
                   escape? false]
              (if-let [c (first chs)]
                (cond
                  escape?
                  (recur (next chs) (str out c) in-string? false)

                  in-string?
                  (cond
                    (= c \\) (recur (next chs) (str out c) in-string? true)
                    (= c \") (recur (next chs) (str out c) false false)
                    :else (recur (next chs) (str out c) in-string? false))

                  (= c \")
                  (recur (next chs) (str out c) true false)

                  (and (= c \/) (= (first (next chs)) \/))
                  out

                  :else
                  (recur (next chs) (str out c) false false))
                out)))]
    (->> (str/split-lines text)
         (map strip-line-comment)
         (map str/trim)
         (remove str/blank?)
         vec)))

(def macro-header-re
  #"(?i)^MACRO\s+([A-Za-z0-9_.:-]+)\s*\((.*)\)\s*:\s*$")

(def macro-end-re
  #"(?i)^ENDMACRO\.\s*$")

(def macro-emit-re
  #"(?i)^EMIT\s+(.+)$")

(def macro-use-re
  #"(?i)^USE\s+([A-Za-z0-9_.:-]+)\s*\((.*)\)\.\s*$")

(defn split-arguments
  "Split a comma-separated argument list, respecting quoted strings and bracket depth."
  [s]
  (split-top-level-comma s "macro arguments"))

(def stdlib-declaration-re
  #"(?i)^(SERVICE|HOST|DATASOURCE|METRIC|POLICY|EVENT|TM_ATOM|LTS_ATOM)\s+([A-Za-z0-9_.:-]+)(?:\s*\[(.*)\])?\.\s*$")

(defn parse-stdlib-declaration
  "Parse one standard-library declaration line.
  Example:
  SERVICE payment [env=prod, criticality=high]."
  [line]
  (when-let [[_ kind name attrs] (re-matches stdlib-declaration-re line)]
    {:kind (keyword (str/lower-case kind))
     :name name
     :attrs (parse-attrs attrs)}))

(defn parse-macro-params
  [s]
  (let [params (split-arguments s)]
    (doseq [p params]
      (when-not (re-matches #"[A-Za-z_][A-Za-z0-9_:\-\.]*" p)
        (throw (ex-info "Invalid macro parameter name" {:param p}))))
    (when (not= (count params) (count (distinct params)))
      (throw (ex-info "Duplicate macro parameter name" {:params params})))
    (vec params)))

(defn collect-macro-definitions
  "Collect `MACRO ... ENDMACRO.` definitions and return:
  {:macros {name {:params [...] :emit [...]}} :payload [...non-definition lines...]}"
  [lines]
  (let [n (count lines)]
    (loop [i 0
           macros {}
           payload []]
      (if (>= i n)
        {:macros macros :payload payload}
        (let [line (nth lines i)]
          (if-let [[_ name params-s] (re-matches macro-header-re line)]
            (let [params (parse-macro-params params-s)
                  _ (when (contains? macros name)
                      (throw (ex-info "Duplicate macro definition" {:macro name})))
                  [next-i emit-lines]
                  (loop [j (inc i)
                         emit-lines []]
                    (when (>= j n)
                      (throw (ex-info "Unterminated macro definition; missing ENDMACRO." {:macro name})))
                    (let [body-line (nth lines j)]
                      (cond
                        (re-matches macro-end-re body-line)
                        [(inc j) emit-lines]

                        :else
                        (if-let [[_ emit] (re-matches macro-emit-re body-line)]
                          (recur (inc j) (conj emit-lines emit))
                          (throw (ex-info "Invalid macro body line (expected EMIT or ENDMACRO.)"
                                          {:macro name :line body-line}))))))]
              (recur next-i
                     (assoc macros name {:params params :emit emit-lines})
                     payload))
            (recur (inc i) macros (conj payload line))))))))

(defn instantiate-macro
  [macros name args-s]
  (let [{:keys [params emit] :as m} (get macros name)]
    (when-not m
      (throw (ex-info "Unknown macro invocation" {:macro name})))
    (let [args (split-arguments args-s)]
      (when (not= (count args) (count params))
        (throw (ex-info "Macro arity mismatch"
                        {:macro name :expected (count params) :actual (count args)})))
      (let [sub-map (zipmap params args)]
        (mapv (fn [line]
                (reduce-kv (fn [acc p v]
                             (str/replace acc (str "{{" p "}}") v))
                           line
                           sub-map))
              emit)))))

(defn expand-macro-uses
  "Expand `USE macro(args...).` lines, recursively."
  [lines macros]
  (loop [queue (seq lines)
         out []
         steps 0]
    (if-let [line (first queue)]
      (if-let [[_ macro-name args-s] (re-matches macro-use-re line)]
        (let [next-steps (inc steps)]
          (when (> next-steps 10000)
            (throw (ex-info "Macro expansion exceeded safe step limit (possible recursion loop)"
                            {:steps next-steps})))
          (let [expanded (instantiate-macro macros macro-name args-s)]
            (recur (concat expanded (rest queue)) out next-steps)))
        (recur (next queue) (conj out line) steps))
      (vec out))))

(defn expand-macros
  "Expand native Zil macros. This is a language-level feature and does not use Clojure macros.

  Macro syntax:
  - `MACRO name(p1, p2):`
  - body lines: `EMIT ...`
  - end: `ENDMACRO.`
  - invocation: `USE name(arg1, arg2).`
  - placeholders in EMIT lines: `{{p1}}`."
  [text]
  (let [lines (preprocess-lines text)
        {:keys [macros payload]} (collect-macro-definitions lines)]
    (expand-macro-uses payload macros)))

(defn parse-program
  "Parse Zil source text into canonical structural IR."
  [text]
  (let [lines (expand-macros text)
        n (count lines)]
    (loop [i 0
           out {:module nil :facts [] :rules [] :queries [] :declarations []}]
      (if (>= i n)
        out
        (let [line (nth lines i)
              decl (parse-stdlib-declaration line)]
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

            decl
            (let [lowered (zl/declaration->facts decl)]
              (recur (inc i)
                     (-> out
                         (update :declarations conj decl)
                         (update :facts into lowered))))

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
  (let [{:keys [module facts rules queries declarations] :as parsed} (parse-program text)]
    (when (str/blank? module)
      (throw (ex-info "Program must define MODULE <name>." {})))
    (zl/validate-declarations! declarations)
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
      (assoc parsed
             :rules rules*
             :strata strata
             :queries queries
             :facts facts
             :declarations declarations))))

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
  [{:keys [module facts rules queries declarations]}]
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
     :declarations declarations
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
