(ns zil.lower-test
  (:require [clojure.test :refer [deftest is testing]]
            [zil.lower :as zl]))

(deftest service-lowering-emits-dependency-facts-test
  (let [facts (zl/declaration->facts
               {:kind :service
                :name "api"
                :attrs {:depends ["db" "cache"]
                        :criticality "HIGH"}})
        triples (set (map (juxt :object :relation :subject) facts))]
    (is (contains? triples ["service:api" :kind "entity:service"]))
    (is (contains? triples ["service:api" :uses "service:db"]))
    (is (contains? triples ["service:api" :depends_on "service:db"]))
    (is (contains? triples ["service:db" :used_by "service:api"]))
    (is (contains? triples ["service:cache" :used_by "service:api"]))
    (is (contains? triples ["service:api" :criticality "value:high"]))))

(deftest declaration-validation-rules-test
  (testing "Datasource enum validation rejects unsupported type"
    (is (thrown? clojure.lang.ExceptionInfo
                 (zl/validate-declaration!
                  {:kind :datasource :name "bad" :attrs {:type "ftp"}}))))

  (testing "Service dependency references must exist"
    (is (thrown? clojure.lang.ExceptionInfo
                 (zl/validate-declarations!
                  [{:kind :service
                    :name "api"
                    :attrs {:uses ["missing-service"]}}]))))

  (testing "Service dependency cycles are rejected"
    (is (thrown? clojure.lang.ExceptionInfo
                 (zl/validate-declarations!
                  [{:kind :service :name "a" :attrs {:uses ["b"]}}
                   {:kind :service :name "b" :attrs {:uses ["a"]}}]))))

  (testing "Metric source must reference a datasource declaration"
    (is (thrown? clojure.lang.ExceptionInfo
                 (zl/validate-declarations!
                  [{:kind :service :name "svc" :attrs {}}
                   {:kind :metric :name "lat" :attrs {:source "service:svc"}}]))))

  (testing "Valid dependency and metric references pass"
    (is (true? (zl/validate-declarations!
                [{:kind :service :name "svc" :attrs {:uses ["svc-db"]}}
                 {:kind :service :name "svc-db" :attrs {}}
                 {:kind :datasource :name "ds" :attrs {:type "rest"}}
                 {:kind :metric :name "lat" :attrs {:source "datasource:ds"}}])))))

(deftest tm-atom-validation-and-lowering-test
  (let [tm {:kind :tm_atom
            :name "parity"
            :attrs {:states #{'q0 'qa 'qr}
                    :alphabet #{'0 '_}
                    :blank '_
                    :initial 'q0
                    :accept #{'qa}
                    :reject #{'qr}
                    :transitions {['q0 '0] ['q0 '0 :R]
                                  ['q0 '_] ['qa '_ :N]}}}
        facts (zl/declaration->facts tm)
        transition-facts (filter #(= :transition (:relation %)) facts)]
    (is (= 2 (count transition-facts)))
    (is (every? #(contains? #{:R :N} (get-in % [:attrs :move])) transition-facts)))

  (testing "TM_ATOM must be complete for non-halting states"
    (is (thrown? clojure.lang.ExceptionInfo
                 (zl/validate-declaration!
                  {:kind :tm_atom
                   :name "bad_tm"
                   :attrs {:states #{'q0 'qa 'qr}
                           :alphabet #{'0 '_}
                           :blank '_
                           :initial 'q0
                           :accept #{'qa}
                           :reject #{'qr}
                           :transitions {['q0 '0] ['q0 '0 :R]}}}))))
  (testing "TM_ATOM move must be L|R|N"
    (is (thrown? clojure.lang.ExceptionInfo
                 (zl/validate-declaration!
                  {:kind :tm_atom
                   :name "bad_move"
                   :attrs {:states #{'q0 'qa 'qr}
                           :alphabet #{'0 '_}
                           :blank '_
                           :initial 'q0
                           :accept #{'qa}
                           :reject #{'qr}
                           :transitions {['q0 '0] ['q0 '0 :X]
                                         ['q0 '_] ['qa '_ :N]}}})))))

(deftest lts-atom-validation-and-lowering-test
  (let [lts {:kind :lts_atom
             :name "service_flow"
             :attrs {:states #{'idle 'running 'failed}
                     :initial 'idle
                     :transitions {['idle 'start] ['running]
                                   ['running 'fail] ['failed 'alert_ops]}}}
        facts (zl/declaration->facts lts)
        edge-facts (filter #(= :edge (:relation %)) facts)]
    (is (= 2 (count edge-facts)))
    (is (= #{"start" "fail"}
           (set (map #(get-in % [:attrs :label]) edge-facts)))))

  (testing "LTS_ATOM initial must belong to states"
    (is (thrown? clojure.lang.ExceptionInfo
                 (zl/validate-declaration!
                  {:kind :lts_atom
                   :name "bad_initial"
                   :attrs {:states #{'idle}
                           :initial 'running
                           :transitions {['idle 'start] ['idle]}}}))))
  (testing "LTS_ATOM transition values must be [next] or [next effect]"
    (is (thrown? clojure.lang.ExceptionInfo
                 (zl/validate-declaration!
                  {:kind :lts_atom
                   :name "bad_transition"
                   :attrs {:states #{'idle 'running}
                           :initial 'idle
                           :transitions {['idle 'start] ['running 'notify 'extra]}}})))))
