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

(ns gripper.helpers
  (:require
    [clojure.edn            :as edn           ]
    [clojure.tools.logging  :as log           ]
    )
  (:import 
    [java.io File]
    [clojure.lang PersistentHashMap PersistentArrayMap]
    )
  (:gen-class))

(defn read-file
  "Returns {:ok string } or {:error...}"
  [^String file]
  (try
    (cond
      (.isFile (File. file))
        {:ok (slurp file) }                         ; if .isFile is true {:ok string}
      :else
        (throw (Exception. "Input is not a file"))) ;the input is not a file, throw exception
  (catch Exception e
    {:error "Exception" :fn "read-file" :exception (.getMessage e) }))) ; catch all exceptions
  
(defn parse-edn-string
  "Returns the Clojure data structure representation of s"
  [s]
  (try
    {:ok (clojure.edn/read-string s)}
  (catch Exception e
    {:error "Exception" :fn "parse-config" :exception (.getMessage e)})))

(defn read-config
  "Returns the Clojure hashmap version of the config file"
  [file]
  (let 
    [ file-string (read-file file) ]
    (cond
      (contains? file-string :ok)
        ;this return the {:ok} or {:error} from parse-edn-string
        (parse-edn-string (file-string :ok))
      :else
        ;the read-file operation returned an error
        file-string)))

(defn exit [n] 
  (log/info "init :: stop")
  (System/exit n))

(defn config-ok [config]
  (log/info "config [ok]") 
  (log/debug config))

(defn config-err
  [config]
  (log/error "config [error]") 
  (log/error config)
  (exit 1))

(def ^PersistentHashMap config (read-config "conf/app.edn"))
