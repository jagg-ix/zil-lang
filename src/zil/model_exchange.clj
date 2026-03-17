(ns zil.model-exchange
  "Git-oriented model exchange checks."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [zil.core :as core]
            [zil.profile.z3 :as z3]))

(def supported-profiles
  #{:tm.det :lts :constraint})

(def ^:private profile-aliases
  {:tm.det :tm.det
   :tm_atom :tm.det
   :tm :tm.det
   "tm.det" :tm.det
   "tm" :tm.det
   "tm_atom" :tm.det
   :lts :lts
   :lts.core :lts
   "lts" :lts
   "lts.core" :lts
   :constraint :constraint
   :constraints :constraint
   "constraint" :constraint
   "constraints" :constraint})

(def ^:private profile-unit-kind
  {:tm.det :tm_atom
   :lts :lts_atom
   :constraint :policy})

(def ^:private commit-policy-key
  {:tm.det :tm_atom_commit
   :lts :lts_atom_commit
   :constraint :constraint_commit})

(defn resolve-profile
  [profile]
  (let [candidate (cond
                    (nil? profile) :tm.det
                    (keyword? profile) profile
                    :else (str/lower-case (str profile)))
        resolved (get profile-aliases candidate)]
    (when-not resolved
      (throw (ex-info "Unsupported model exchange profile"
                      {:profile profile
                       :supported supported-profiles})))
    resolved))

(defn- zc-file?
  [^java.io.File f]
  (and (.isFile f)
       (str/ends-with? (.getName f) ".zc")))

(defn collect-zc-files
  "Collect .zc files from a file path or recursively from a directory."
  [path]
  (let [f (io/file path)]
    (cond
      (not (.exists f)) []
      (.isFile f) (if (zc-file? f) [(.getPath f)] [])
      (.isDirectory f) (->> (file-seq f)
                            (filter zc-file?)
                            (map #(.getPath ^java.io.File %))
                            sort
                            vec)
      :else [])))

(defn- compile-one
  [path]
  (try
    {:file path
     :ok true
     :compiled (core/compile-program (slurp path))}
    (catch Exception e
     {:file path
       :ok false
       :error (.getMessage e)})))

(defn- require-files!
  [path files]
  (when (empty? files)
    (throw (ex-info "No .zc files found in bundle path" {:path path})))
  files)

(defn- compile-errors
  [results]
  (for [{:keys [file error]} (filterv (complement :ok) results)]
    {:type :compile
     :file file
     :error error}))

(defn- decls-of-kind
  [compiled kind]
  (filterv #(= kind (:kind %)) (:declarations compiled)))

(defn- count-decls-of-kind
  [compiled-results kind]
  (->> compiled-results
       (mapcat #(decls-of-kind % kind))
       count))

(defn- profile-bundle-errors
  [profile compiled-results]
  (let [unit-kind (get profile-unit-kind profile)
        unit-count (count-decls-of-kind compiled-results unit-kind)]
    (when (zero? unit-count)
      [{:type :policy
        :error (str "Bundle must include at least one " (str/upper-case (name unit-kind)) " declaration")
        :profile profile}])))

(defn- constraint-solver-errors
  [successful]
  (let [per-file-errors
        (mapcat
         (fn [{:keys [file compiled]}]
           (let [policies (decls-of-kind compiled :policy)
                 report (z3/check-policy-declarations policies {:scope :file})]
             (map #(assoc % :file file) (:errors report))))
         successful)
        all-policies (vec (mapcat #(decls-of-kind (:compiled %) :policy) successful))
        bundle-report (z3/check-policy-declarations all-policies {:scope :bundle})
        bundle-errors (:errors bundle-report)]
    (vec (concat per-file-errors bundle-errors))))

(defn- profile-solver-errors
  [profile successful]
  (case profile
    :constraint (constraint-solver-errors successful)
    []))

(defn check-bundle
  "Validate one model bundle path.

  A valid bundle currently requires:
  - all .zc files compile
  - at least one profile unit declaration"
  ([path]
   (check-bundle path {}))
  ([path {:keys [profile] :or {profile :tm.det}}]
   (let [profile* (resolve-profile profile)
         files (require-files! path (collect-zc-files path))
         results (mapv compile-one files)
         successful (filterv :ok results)
         compiled-results (mapv :compiled successful)
         errors (vec
                 (concat
                  (compile-errors results)
                  (profile-bundle-errors profile* compiled-results)
                  (profile-solver-errors profile* successful)))]
     {:ok (empty? errors)
     :path path
     :files files
      :modules (vec (map :module compiled-results))
      :profile profile*
      :unit_kind (get profile-unit-kind profile*)
      :unit_count (count-decls-of-kind compiled-results (get profile-unit-kind profile*))
      :tm_atoms (count-decls-of-kind compiled-results :tm_atom)
      :errors errors})))

(defn- commit-policy-errors
  [profile strict-units-only? {:keys [file compiled]}]
  (let [unit-kind (get profile-unit-kind profile)
        units (decls-of-kind compiled unit-kind)
        non-unit (filterv #(not= unit-kind (:kind %)) (:declarations compiled))]
    (vec
     (concat
      (when (not= 1 (count units))
        [{:type :policy
          :file file
          :profile profile
          :error (str "Commit unit must contain exactly one " (str/upper-case (name unit-kind)) " declaration")
          :unit_kind unit-kind
          :unit_count (count units)}])
      (when (and strict-units-only? (seq non-unit))
        [{:type :policy
          :file file
          :profile profile
          :error (str "Commit unit must contain only " (str/upper-case (name unit-kind)) " declarations")
          :unit_kind unit-kind
          :non_unit_kinds (vec (sort (set (map :kind non-unit))))}])))))

(defn check-commit
  "Validate commit-like model units.

  A valid commit-like path requires:
  - all .zc files compile
  - each .zc file contains exactly one profile unit declaration
  - optionally, each .zc file contains only profile unit declarations"
  ([path]
   (check-commit path {}))
  ([path {:keys [profile strict-units-only?]
          :or {profile :tm.det
               strict-units-only? true}}]
   (let [profile* (resolve-profile profile)
         files (require-files! path (collect-zc-files path))
         results (mapv compile-one files)
         successful (filterv :ok results)
         compiled-results (mapv :compiled successful)
         unit-kind (get profile-unit-kind profile*)
         errors (vec
                 (concat
                  (compile-errors results)
                  (mapcat #(commit-policy-errors profile* strict-units-only? %) successful)
                  (profile-solver-errors profile* successful)))]
     {:ok (empty? errors)
      :path path
      :files files
      :modules (vec (map :module compiled-results))
      :profile profile*
      :policy (get commit-policy-key profile*)
      :strict_units_only strict-units-only?
      :unit_kind unit-kind
      :unit_count (count-decls-of-kind compiled-results unit-kind)
      :tm_atoms (count-decls-of-kind compiled-results :tm_atom)
      :errors errors})))
