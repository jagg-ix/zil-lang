(ns zil.cli
  (:gen-class)
  (:require [clojure.pprint :as pp]
            [zil.core :as core]))

(defn -main
  [& args]
  (let [path (first args)]
    (when-not path
      (binding [*out* *err*]
        (println "Usage: clojure -M -m zil.cli <program.zc>"))
      (System/exit 2))
    (pp/pprint (core/execute-file path))))
