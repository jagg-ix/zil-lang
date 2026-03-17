(ns zil.runtime.adapters.command
  "Command adapter skeleton."
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]
            [zil.runtime.adapters.core :as ac]))

(defn read-command
  [datasource _opts]
  (let [attrs (:attrs datasource)
        cmd (or (:command attrs) (:command_path attrs))]
    (when-not (and (string? cmd) (not (str/blank? cmd)))
      (throw (ex-info "COMMAND datasource requires :command or :command_path"
                      {:datasource datasource})))
    (let [{:keys [out err exit]} (sh/sh "bash" "-lc" cmd)]
      [{:exit exit
        :stdout out
        :stderr err}])))

(ac/register-adapter! :command read-command)
