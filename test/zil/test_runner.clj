(ns zil.test-runner
  (:require [clojure.test :refer [run-tests]]
            [zil.bridge-lean4-test]
            [zil.bridge-tla-test]
            [zil.core-test]
            [zil.lower-test]
            [zil.model-exchange-test]
            [zil.runtime-ingest-test]))

(defn -main
  [& _]
  (let [{:keys [fail error]} (run-tests 'zil.bridge-lean4-test
                                        'zil.bridge-tla-test
                                        'zil.core-test
                                        'zil.lower-test
                                        'zil.model-exchange-test
                                        'zil.runtime-ingest-test)]
    (System/exit (if (pos? (+ fail error)) 1 0))))
