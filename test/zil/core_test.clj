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

(defn -main
  [& _]
  (let [{:keys [fail error]} (run-tests 'zil.core-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
