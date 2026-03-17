(ns zil.runtime.adapters.file
  "File adapter skeleton."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [zil.runtime.adapters.core :as ac]))

(defn- parse-lines
  [path]
  (->> (slurp path)
       str/split-lines
       (map-indexed (fn [idx line]
                      {:line_number (inc idx)
                       :line line}))
       vec))

(defn read-file
  [datasource _opts]
  (let [attrs (:attrs datasource)
        path (or (:path attrs) (:file attrs))
        mode (ac/normalize-type (or (:mode attrs) :lines))]
    (when-not path
      (throw (ex-info "FILE datasource requires :path"
                      {:datasource datasource})))
    (case mode
      :lines (parse-lines path)
      :text [{:text (slurp path)}]
      :edn (let [v (edn/read-string (slurp path))]
             (if (sequential? v) (vec v) [v]))
      (throw (ex-info "Unsupported FILE mode"
                      {:mode mode :datasource datasource})))))

(ac/register-adapter! :file read-file)
