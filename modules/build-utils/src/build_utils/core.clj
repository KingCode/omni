(ns build-utils.core
  (:require [build-utils.util :as u]
            [build-utils.config :as c]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

