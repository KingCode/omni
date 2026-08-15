(ns test-runner
  (:refer-clojure :exclude [test])
  (:require [clojure.java.io :as io]
            [cognitect.test-runner.api :as test-runner]))

(defn- module-test-dirs
  "Finds every modules/*/test directory that actually exists on disk."
  []
  (->> (io/file "modules")
       .listFiles
       (map #(io/file % "test"))
       (filter #(.exists %))
       (map #(.getPath %))
       vec))

(defn test
  "Runs tests. With no args, runs every module's tests.
   Pass {:module \"cond-utils\"} to run just one module."
  [{:keys [module] :as opts}]
  (let [dirs (if module
               [(str "modules/" module "/test")]
               (module-test-dirs))]
    (println "Testing dirs:" dirs)
    (test-runner/test (assoc opts :dirs dirs))))


(comment
(module-test-dirs)
)
