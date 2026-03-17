(ns zil.runtime-ingest-test
  (:require [clojure.test :refer [deftest is]]
            [zil.core :as core]
            [zil.runtime.adapters.core :as ac]
            [zil.runtime.datascript :as zr]
            [zil.runtime.ingest :as ingest])
  (:import [com.sun.net.httpserver HttpHandler HttpServer]
           [java.net InetSocketAddress]))

(defn- start-json-server
  []
  (let [counter (atom 0)
        server (HttpServer/create (InetSocketAddress. "127.0.0.1" 0) 0)
        handler (reify HttpHandler
                  (handle [_ exchange]
                    (let [n (swap! counter inc)
                          body (str "{\"metric\":\"metric:latency\",\"value\":" n "}")
                          bytes (.getBytes body "UTF-8")]
                      (.add (.getResponseHeaders exchange) "Content-Type" "application/json")
                      (.sendResponseHeaders exchange 200 (alength bytes))
                      (with-open [os (.getResponseBody exchange)]
                        (.write os bytes)))))]
    (.createContext server "/metrics" handler)
    (.start server)
    {:server server
     :counter counter
     :url (str "http://127.0.0.1:" (.getPort (.getAddress server)) "/metrics")}))

(defn- wait-until
  [pred timeout-ms]
  (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (cond
        (pred) true
        (> (System/currentTimeMillis) deadline) false
        :else (do (Thread/sleep 50)
                  (recur))))))

(deftest ingest-pipeline-smoke-test
  (let [tmp (java.io.File/createTempFile "zil-ingest" ".txt")
        _ (.deleteOnExit tmp)
        _ (spit tmp "alpha\nbeta\n")
        path (.getAbsolutePath tmp)
        program (str "MODULE ingest.demo.
DATASOURCE ds_rest [type=rest, mock_responses=[{:metric \"metric:latency\", :value 123}, {:metrics {:error_rate 0.01}}]].
DATASOURCE ds_file [type=file, path=\"" path "\", mode=lines].
DATASOURCE ds_cmd [type=command, command=\"printf 'hello-from-cmd'\"].
")
        compiled (core/compile-program program)
        conn (zr/make-conn)
        summary (ingest/ingest-all! conn compiled {:revision 7})
        snapshot (zr/facts-at-or-before @conn 7)
        relations (set (map :relation snapshot))]
    (is (every? (set (ac/supported-types)) [:rest :file :command]))
    (is (= 3 (:sources summary)))
    (is (pos? (:records summary)))
    (is (pos? (:facts summary)))
    (is (contains? relations :ingested_record))
    (is (contains? relations :observed_from))
    (is (some #(and (= "metric:latency" (:object %))
                    (= :observed_from (:relation %)))
              snapshot))
    (is (some #(= "datasource:ds_cmd" (:object %)) snapshot))))

(deftest rest-http-and-interval-polling-test
  (let [{:keys [server url counter]} (start-json-server)]
    (try
      (let [program (str "MODULE poll.demo.
DATASOURCE live [type=rest, url=\"" url "\", format=json, poll_mode=interval, poll_every_ms=120].
")
            compiled (core/compile-program program)
            conn (zr/make-conn)
            {:keys [handles]} (ingest/start-all-pollers! conn compiled {:initial_revision 100})
            converged? (wait-until
                        #(let [runs (-> handles first :stats deref :runs)]
                           (and (>= @counter 2)
                                (>= runs 2)))
                        3000)
            _ (ingest/stop-all-pollers! handles)
            snap (zr/facts-at-or-before @conn Long/MAX_VALUE)
            latest (first (filter #(and (= "metric:latency" (:object %))
                                        (= :observed_from (:relation %)))
                                  snap))
            polled (-> handles first :stats deref :runs)]
        (is converged?)
        (is (>= @counter 2))
        (is (>= polled 2))
        (is (some? latest))
        (is (>= (long (get-in latest [:attrs :value])) 2)))
      (finally
        (.stop ^HttpServer server 0)))))
