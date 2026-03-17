(ns zil.cli
  (:gen-class)
  (:require [clojure.pprint :as pp]
            [zil.bridge.lean4 :as bl]
            [zil.bridge.tla :as bt]
            [zil.core :as core]
            [zil.model-exchange :as mx]))

(defn -main
  [& args]
  (let [cmd (first args)
        cmd-args (vec (rest args))
        arg (nth cmd-args 0 nil)
        a3 (nth cmd-args 1 nil)
        a4 (nth cmd-args 2 nil)
        [profile strict-units-only?]
        (cond
          (= a3 "--allow-mixed") [nil false]
          (= a4 "--allow-mixed") [a3 false]
          :else [a3 true])
        opts (cond-> {}
               profile (assoc :profile profile)
               (and (= cmd "commit-check") (false? strict-units-only?))
               (assoc :strict-units-only? false))]
    (cond
      (or (nil? cmd) (= cmd "--help") (= cmd "-h"))
      (do
        (binding [*out* *err*]
          (println "Usage:")
          (println "  ./bin/zil <program.zc>")
          (println "  ./bin/zil bundle-check <file-or-dir> [tm.det|lts|constraint]")
          (println "  ./bin/zil commit-check <file-or-dir> [tm.det|lts|constraint] [--allow-mixed]")
          (println "  ./bin/zil export-tla <file-or-dir> [output.tla] [module_name]")
          (println "  ./bin/zil export-lean <file-or-dir> [output.lean] [namespace]")
          (println "")
          (println "  clojure -M -m zil.cli <program.zc>")
          (println "  clojure -M -m zil.cli bundle-check <file-or-dir> [tm.det|lts|constraint]")
          (println "  clojure -M -m zil.cli commit-check <file-or-dir> [tm.det|lts|constraint] [--allow-mixed]")
          (println "  clojure -M -m zil.cli export-tla <file-or-dir> [output.tla] [module_name]")
          (println "  clojure -M -m zil.cli export-lean <file-or-dir> [output.lean] [namespace]"))
        (System/exit 2))

      (= cmd "bundle-check")
      (do
        (when-not arg
          (binding [*out* *err*]
            (println "Missing path for bundle-check"))
          (System/exit 2))
        (try
          (let [report (mx/check-bundle arg opts)]
            (pp/pprint report)
            (when-not (:ok report)
              (System/exit 1)))
          (catch clojure.lang.ExceptionInfo e
            (binding [*out* *err*]
              (println (.getMessage e))
              (pp/pprint (ex-data e)))
            (System/exit 2))))

      (= cmd "commit-check")
      (do
        (when-not arg
          (binding [*out* *err*]
            (println "Missing path for commit-check"))
          (System/exit 2))
        (try
          (let [report (mx/check-commit arg opts)]
            (pp/pprint report)
            (when-not (:ok report)
              (System/exit 1)))
          (catch clojure.lang.ExceptionInfo e
            (binding [*out* *err*]
              (println (.getMessage e))
              (pp/pprint (ex-data e)))
            (System/exit 2))))

      (= cmd "export-tla")
      (do
        (when-not arg
          (binding [*out* *err*]
            (println "Missing path for export-tla"))
          (System/exit 2))
        (try
          (let [output-path (when (and a3 (not= a3 "-")) a3)
                opts (cond-> {}
                       output-path (assoc :output-path output-path)
                       a4 (assoc :module-name a4))
                report (bt/export-lts->tla arg opts)]
            (if output-path
              (pp/pprint (dissoc report :text))
              (print (:text report))))
          (catch clojure.lang.ExceptionInfo e
            (binding [*out* *err*]
              (println (.getMessage e))
              (pp/pprint (ex-data e)))
            (System/exit 2))))

      (= cmd "export-lean")
      (do
        (when-not arg
          (binding [*out* *err*]
            (println "Missing path for export-lean"))
          (System/exit 2))
        (try
          (let [output-path (when (and a3 (not= a3 "-")) a3)
                opts (cond-> {}
                       output-path (assoc :output-path output-path)
                       a4 (assoc :namespace a4))
                report (bl/export-lts->lean4 arg opts)]
            (if output-path
              (pp/pprint (dissoc report :text))
              (print (:text report))))
          (catch clojure.lang.ExceptionInfo e
            (binding [*out* *err*]
              (println (.getMessage e))
              (pp/pprint (ex-data e)))
            (System/exit 2))))

      :else
      (pp/pprint (core/execute-file cmd)))))
