(ns zil.core-test
  (:require [clojure.test :refer [deftest is run-tests testing]]
            [zil.core :as core]))

(deftest query-exec-smoke-test
  (let [program "MODULE demo.
app:a#mirrored_by@app:b.
QUERY mirrors:
FIND ?x WHERE ?x#mirrored_by@?y.
"
        out (core/execute-program program)]
    (is (= "demo" (:module out)))
    (is (= [["app:a"]]
           (get-in out [:queries "mirrors" :rows])))))

(deftest rule-negation-derivation-test
  (testing "Negated literal derives fact when negated atom is absent"
    (let [program "MODULE demo.
app:a#depends_on@service:db.
RULE degrade:
IF app:a#depends_on@service:db AND NOT service:db#available@value:true
THEN app:a#status@value:degraded.
QUERY status:
FIND ?s WHERE app:a#status@?s.
"
          out (core/execute-program program)]
      (is (= [["value:degraded"]]
             (get-in out [:queries "status" :rows])))))
  (testing "Negated literal blocks derivation when atom is present"
    (let [program "MODULE demo.
app:a#depends_on@service:db.
service:db#available@value:true.
RULE degrade:
IF app:a#depends_on@service:db AND NOT service:db#available@value:true
THEN app:a#status@value:degraded.
QUERY status:
FIND ?s WHERE app:a#status@?s.
"
          out (core/execute-program program)]
      (is (= []
             (get-in out [:queries "status" :rows]))))))

(deftest stratification-detects-negative-cycle-test
  (let [program "MODULE demo.
RULE bad:
IF NOT x#a@y
THEN x#a@y.
"]
    (is (thrown? clojure.lang.ExceptionInfo
                 (core/compile-program program)))))

(deftest native-macro-expansion-test
  (testing "Macro emits facts with parameter substitution"
    (let [program "MODULE demo.
MACRO link_pair(a,b):
EMIT {{a}}#linked_to@{{b}}.
EMIT {{b}}#linked_to@{{a}}.
ENDMACRO.
USE link_pair(node:a, node:b).
QUERY q:
FIND ?x WHERE ?x#linked_to@node:b.
"
          out (core/execute-program program)]
      (is (= [["node:a"]]
             (get-in out [:queries "q" :rows])))))
  (testing "Nested macro expansion works without Clojure macros"
    (let [program "MODULE demo.
MACRO one(a,b):
EMIT {{a}}#edge@{{b}}.
ENDMACRO.
MACRO both(a,b):
EMIT USE one({{a}}, {{b}}).
EMIT USE one({{b}}, {{a}}).
ENDMACRO.
USE both(n:1, n:2).
QUERY q:
FIND ?x WHERE ?x#edge@n:2.
"
          out (core/execute-program program)]
      (is (= [["n:1"]]
             (get-in out [:queries "q" :rows]))))))

(deftest unknown-macro-fails-fast-test
  (let [program "MODULE demo.
USE missing_macro(x,y).
"]
    (is (thrown? clojure.lang.ExceptionInfo
                 (core/execute-program program)))))

(deftest stdlib-declaration-lowering-test
  (let [program "MODULE demo.
SERVICE payment [env=prod, tier=critical].
HOST host1 [environment=prod, timezone=\"America/Mexico_City\"].
DATASOURCE app_metrics [type=rest, format=json].
METRIC latency [source=datasource:app_metrics, unit=ms].
POLICY latency_guard [condition=\"latency > 120\", criticality=HIGH].
EVENT deploy [start_time=\"2026-03-16T10:00:00-06:00\", labels=[\"ops\", \"deploy\"]].
QUERY services:
FIND ?s WHERE ?s#kind@entity:service.
QUERY source_type:
FIND ?t WHERE datasource:app_metrics#type@?t.
QUERY policy_criticality:
FIND ?c WHERE policy:latency_guard#criticality@?c.
QUERY event_labels:
FIND ?l WHERE event:deploy#labels@?l.
"
        out (core/execute-program program)]
    (is (= [["service:payment"]]
           (get-in out [:queries "services" :rows])))
    (is (= [["value:rest"]]
           (get-in out [:queries "source_type" :rows])))
    (is (= [["value:high"]]
           (get-in out [:queries "policy_criticality" :rows])))
    (is (= #{"value:ops" "value:deploy"}
           (set (map first (get-in out [:queries "event_labels" :rows])))))))

(deftest stdlib-duplicate-declaration-fails-test
  (let [program "MODULE demo.
SERVICE payment [env=prod].
SERVICE payment [env=qa].
"]
    (is (thrown? clojure.lang.ExceptionInfo
                 (core/compile-program program)))))

(deftest attrs-supports-nested-edn-values-test
  (let [program "MODULE demo.
app:a#cfg@value:ok [meta={:foo 1, :bar [1 2]}, tags=#{:blue :green}].
"
        compiled (core/compile-program program)
        fact (first (:facts compiled))]
    (is (= {:foo 1 :bar [1 2]}
           (get-in fact [:attrs :meta])))
    (is (= #{:blue :green}
           (get-in fact [:attrs :tags])))))

(deftest service-declaration-dependency-semantics-test
  (let [program "MODULE demo.
SERVICE api [depends=[db], criticality=LOW].
SERVICE db [env=prod].
QUERY uses:
FIND ?x WHERE service:api#uses@?x.
QUERY used_by:
FIND ?x WHERE service:db#used_by@?x.
QUERY depends_on:
FIND ?x WHERE service:api#depends_on@?x.
QUERY crit:
FIND ?c WHERE service:api#criticality@?c.
"
        out (core/execute-program program)]
    (is (= [["service:db"]]
           (get-in out [:queries "uses" :rows])))
    (is (= [["service:api"]]
           (get-in out [:queries "used_by" :rows])))
    (is (= [["service:db"]]
           (get-in out [:queries "depends_on" :rows])))
    (is (= [["value:low"]]
           (get-in out [:queries "crit" :rows])))))

(deftest tm-atom-declaration-query-test
  (let [program "MODULE tm.demo.
TM_ATOM parity [states=#{q0 qa qr}, alphabet=#{0 _}, blank=_, initial=q0, accept=#{qa}, reject=#{qr}, transitions={[q0 0] [q0 0 :R], [q0 _] [qa _ :N]}].
QUERY transitions:
FIND ?t WHERE tm_atom:parity#transition@?t.
"
        out (core/execute-program program)]
    (is (= 2 (count (get-in out [:queries "transitions" :rows]))))))

(deftest lts-atom-declaration-query-test
  (let [program "MODULE lts.demo.
LTS_ATOM deploy_flow [states=#{draft reviewing approved rejected}, initial=draft, transitions={[draft submit] [reviewing], [reviewing approve] [approved], [reviewing reject] [rejected notify_author]}].
QUERY edges:
FIND ?e WHERE lts_atom:deploy_flow#edge@?e.
"
        out (core/execute-program program)]
    (is (= 3 (count (get-in out [:queries "edges" :rows]))))))

(defn -main
  [& _]
  (let [{:keys [fail error]} (run-tests 'zil.core-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
