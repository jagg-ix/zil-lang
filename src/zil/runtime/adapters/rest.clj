(ns zil.runtime.adapters.rest
  "REST adapter with real HTTP support plus local/mock fallbacks.

  Supported attrs:
  - :url                       ; real HTTP endpoint
  - :method                    ; get|post|put|patch|delete (default: get)
  - :headers                   ; map of headers
  - :body                      ; request body for non-GET methods
  - :timeout_ms                ; request timeout (default: 5000)
  - :format                    ; json|edn|text (default: json for :url, text otherwise)
  - :mock_response / :mock_responses
  - :path                      ; local payload file"
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [zil.runtime.adapters.core :as ac])
  (:import [java.net URI]
           [java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers]
           [java.time Duration]
           [java.util.concurrent Executors ThreadFactory]))

(defonce ^:private http-client
  (delay
    (let [daemon-factory
          (reify ThreadFactory
            (newThread [_ runnable]
              (doto (Thread. runnable)
                (.setName "zil-rest-http")
                (.setDaemon true))))
          executor (Executors/newCachedThreadPool daemon-factory)]
      (-> (HttpClient/newBuilder)
          (.executor executor)
          (.build)))))

(defn- ->format
  [attrs]
  (ac/normalize-type
   (or (:format attrs)
       (when (:url attrs) :json)
       :text)))

(defn- parse-body
  [body format]
  (case format
    :json (json/read-str body :key-fn keyword)
    :edn (edn/read-string body)
    :text {:raw body}
    {:raw body}))

(defn- to-records
  [payload]
  (cond
    (nil? payload) []
    (sequential? payload) (vec payload)
    :else [payload]))

(defn- request-body
  [attrs]
  (let [body (:body attrs)
        fmt (->format attrs)]
    (cond
      (nil? body) ""
      (string? body) body
      (= fmt :json) (json/write-str body)
      :else (pr-str body))))

(defn- build-request
  [attrs]
  (let [url (:url attrs)
        _ (when-not (and (string? url) (not (str/blank? url)))
            (throw (ex-info "REST datasource requires non-empty :url for HTTP mode"
                            {:attrs attrs})))
        method (str/upper-case (name (ac/normalize-type (or (:method attrs) :get))))
        timeout-ms (long (or (:timeout_ms attrs) 5000))
        headers (or (:headers attrs) {})
        body (request-body attrs)
        builder (-> (HttpRequest/newBuilder (URI/create url))
                    (.timeout (Duration/ofMillis timeout-ms)))]
    (doseq [[k v] headers]
      (.header builder (name k) (str v)))
    (-> builder
        (.method method
                 (if (= method "GET")
                   (HttpRequest$BodyPublishers/noBody)
                   (HttpRequest$BodyPublishers/ofString body)))
        (.build))))

(defn- http-read
  [attrs]
  (let [req (build-request attrs)
        resp (.send ^HttpClient @http-client req (HttpResponse$BodyHandlers/ofString))
        status (.statusCode resp)
        body (.body resp)]
    (when (>= status 400)
      (throw (ex-info "HTTP request failed"
                      {:status status :body body})))
    body))

(defn read-rest
  [datasource _opts]
  (let [attrs (:attrs datasource)
        format (->format attrs)
        mock-many (:mock_responses attrs)
        mock-one (:mock_response attrs)
        path (:path attrs)]
    (cond
      mock-many (to-records mock-many)
      mock-one (to-records mock-one)
      (:url attrs) (-> attrs
                       http-read
                       (parse-body format)
                       to-records)
      path (-> (slurp path)
               (parse-body format)
               to-records)
      :else [])))

(ac/register-adapter! :rest read-rest)
