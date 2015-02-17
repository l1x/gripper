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

(ns gripper.core
  (:require
    [gripper.http           :as gripper-http  ]
    [clojure.edn            :as edn           ]
    [clojure.core.async     :refer 
      [alts! chan go thread timeout 
       >! >!! <! <!! go-loop  ]               ]
    [clojure.tools.logging  :as log           ]
    )
  (:import 
    [java.io File]
    [java.util UUID]
    [clojure.lang PersistentHashMap PersistentArrayMap]
    [clojure.core.async.impl.channels ManyToManyChannel]
    )
  (:gen-class))

;; MAIN
;;
(defn -main
  [& args]
  ;(let [  ^PersistentHashMap config (read-config "conf/app.edn")       ]
    ;; INIT
    (log/info "init start")
    (log/info "checking config...")
    (log/info "reading config and initializing state (opening connections, etc.)")
    (log/info "spawning X kafka producer threads")

  
    (dotimes [i 8]
      (thread
        (let [ _ (str 1) ]
          (go-loop [] 
            (let [ message (<!! gripper-http/work-chan) ] (log/debug "go-loop thread : " message))
            (recur)))))
  
   (gripper-http/start 8080)
  
  )

