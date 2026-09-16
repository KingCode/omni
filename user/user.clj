(ns user
  (:require [clj-reload.core :as reload]))

(reload/init {:dirs ["modules/build-utils/src"
                     "modules/build-utils/test"]})

(defn reload []
  (reload/reload {:throw false}))
