(ns zil.bridge-theorem-ci-test
  (:require [clojure.test :refer [deftest is]]
            [zil.bridge.theorem-ci :as btci]))

(defn- tmp-dir
  []
  (.toFile
   (java.nio.file.Files/createTempDirectory
    "zil-bridge-theorem-ci"
    (make-array java.nio.file.attribute.FileAttribute 0))))

(deftest theorem-ci-one-shot-pipeline-test
  (let [root (tmp-dir)
        model-file (java.io.File. root "incident-theorem.zc")
        out-dir (java.io.File. root "artifacts")]
    (spit model-file "MODULE theorem.ci.demo.
theorem:t_incident_guard#kind@entity:theorem.
theorem:t_incident_guard#criticality@value:medium.
theorem:t_incident_guard#requires_assumption@assumption:a_operator_present.
theorem:t_incident_guard#ensures@guarantee:incident_guard_active.
")
    (let [report (btci/run-theorem-ci
                  (.getAbsolutePath model-file)
                  {:out-dir (.getAbsolutePath out-dir)
                   :bridge-module "theorem.ci.bridge"
                   :tla-module "TheoremCIBridge"
                   :lean-namespace "Zil.Generated.Theorem.CI"})
          bridge-zc (get-in report [:artifacts :bridge_zc])
          bridge-tla (get-in report [:artifacts :bridge_tla])
          bridge-lean (get-in report [:artifacts :bridge_lean])]
      (is (:ok report))
      (is (= 1 (get-in report [:bridge :theorem_count])))
      (is (= true (get-in report [:checks :lts :ok])))
      (is (= true (get-in report [:checks :constraint :ok])))
      (is (.exists (java.io.File. bridge-zc)))
      (is (.exists (java.io.File. bridge-tla)))
      (is (.exists (java.io.File. bridge-lean)))
      (is (re-find #"criticality=medium" (slurp bridge-zc)))
      (is (re-find #"MODULE TheoremCIBridge" (slurp bridge-tla)))
      (is (re-find #"namespace Zil.Generated.Theorem.Ci" (slurp bridge-lean))))))
