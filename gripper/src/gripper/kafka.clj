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

(ns gripper.kafka
  (:require
    [clojure.core.async       :refer
      [alts! chan go thread timeout
       >! >!! <! <!! go-loop  ]                               ]
    [clojure.tools.logging    :as log                         ]
    [metrics.ring.instrument  :refer [instrument]             ]
    [metrics.ring.expose      :refer [expose-metrics-as-json] ]
    [clojure.data.json        :as json                        ]
    [shovel.producer          :as sh-producer                 ]
    )
  (:import
    [java.io File]
    [java.util UUID]
    [clojure.lang PersistentHashMap PersistentArrayMap]
    [clojure.core.async.impl.channels ManyToManyChannel]
    [com.codahale.metrics MetricRegistry]
    )
  (:gen-class))

;; helpers

(def config {  
    :metadata.broker.list           "localhost:9092"
    :serializer.class               "kafka.serializer.StringEncoder"
    :request.required.acks          "1"
  })


(defn json-producer
  [config]
  (let [producer-connection (sh-producer/producer-connector config)]
    (doseq [n (range 100)]
      (sh-producer/produce
        producer-connection
        ;move this to config
        (sh-producer/message "topic" "asd" (str "this is my message" n))))))


