;;Copyright 2015 Istvan Szukacs

;;Licensed under the Apache License, Version 2.0 (the "License");
;;you may not use this file except in compliance with the License.
;;You may obtain a copy of the License at

;;    http://www.apache.org/licenses/LICENSE-2.0

;;Unless required by applicable law or agreed to in writing, software
;;distributed under the License is distributed on an "AS IS" BASIS,
;;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;;See the License for the specific language governing permissions and
;;limitations under the License

(ns gripper.http
  (:require
    [aleph.http               :as aleph-http                  ]
    [clojure.edn              :as edn                         ]
    [clojure.core.async       :refer
      [alts!! chan go thread timeout
       >! >!! <! <!! go-loop  ]                               ]
    [clojure.tools.logging    :as log                         ]
    [metrics.ring.instrument  :refer [instrument]             ]
    [metrics.ring.expose      :refer [expose-metrics-as-json] ]
    [clojure.data.json        :as     json                    ]
    [compojure.core           :refer :all                     ]
    [compojure.route          :as route                       ]
    [ring.util.response       :refer [response content-type 
                                      status]                 ]
    )
  (:import
    [java.io File ByteArrayInputStream]
    [java.util UUID]
    [clojure.lang PersistentHashMap PersistentArrayMap]
    [clojure.core.async.impl.channels ManyToManyChannel]
    [com.codahale.metrics MetricRegistry]
    [aleph.http.core HeaderMap NettyRequest]
    )
  (:gen-class))

;; helpers

(defn json-response
  "Return ring.util.response with content-type set to application/json"
  [data]
  (-> (json/write-str data)
       response
      (content-type "application/json; charset=utf-8")))

(defn system-properties
  "Returns system-properties as a hashmap {}"
  ^PersistentHashMap [] 
  (reduce (fn [x [y z]] (assoc x y z)) {} (System/getProperties)))

(def ^ManyToManyChannel work-chan (chan 256))
(def ^ManyToManyChannel stat-chan (chan 256))

(defn parse-json-body
  "Parses http request body as it is JSON, if fails returns an {:error....}"
  ^PersistentHashMap [^ByteArrayInputStream body] 
  (try
    {:ok (json/read-str (slurp body))}
  (catch Exception e
    {:error "Exception" :fn "parse-json-body" :exception (.getMessage e) })))

;; controller functions

;/api/v1/echo
(defn echo-get
  "Displays the request headers"
  [^HeaderMap headers _ ^String uri]
  (log/debug headers uri)
  (log/debug (json-response headers))
  (json-response headers))

(defn echo-post
    "Reads the http body and parses the JSON into a map"
    [^NettyRequest req]
    (log/debug req)
    (let [  ^PersistentHashMap    headers (:headers req) 
            ^ByteArrayInputStream body (:body req)
            ^PersistentHashMap    parsed-body (parse-json-body body) ]

    (log/debug "sending echo-post : " parsed-body)
    (cond
      (contains? parsed-body :ok)
        ;alts!! is chosing between delivering the message or reaching the timeout
        (let [[ret _] (alts!! [[work-chan parsed-body] (timeout 150)])] 
          (cond ret 
              (json-response {:ok :ok})
            :else 
              (status (json-response {:error "Sending messages to Kafka is timing out..."}) 503)))
      ;if parsed-body is not happy
      :else
        (status (json-response {:error "Input is not JSON"}) 400))))

;/api/v1/system/:id
(defn system
  ":id -> returns the value of the 
  :id key from the System/getProperties"
  ([]   (json-response (system-properties)))
  ([id] (json-response {(keyword id) (-> (System/getProperties) (.get id))})))

;; routes

(defroutes api-routes
  "/api/v1"
  ; echo
  (GET "/echo"    
    {headers :headers body :body uri :uri}
    (echo-get headers body uri))
  ; system
  ; returns the the JVM system properties
  (GET "/system"      []    (system))
  ; returns a specific JVM property with the :id
  (GET "/system/:id"  [id]  (system id))
  ; servers
  ; returns something
  (POST "/servers" request (echo-post request)))

(defroutes app-routes
  ;api - json
  (context "/api/v1"    [] api-routes)
  ;static resources
  (route/resources "/")
  ;404 -html
  (route/not-found "404 Not Found"))

(def app
  (-> app-routes
    (instrument)
    (expose-metrics-as-json "/metrics/http/")))

(defn start 
  [port]
  (log/info "Starting up HTTP server on port : " port)
  (aleph-http/start-server app {:port port}))

