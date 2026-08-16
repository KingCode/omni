(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'org.clojars.kingnuscodus/omni.cond-utils)
(def version "0.1.0-PATCH")
(def class-dir "target/classes")
(def basis (delay (b/create-basis {:project "deps.edn"})))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis @basis
                :src-dirs ["src"]
                :pom-data
                [[:description "Utilities extending clojure.core/cond"]
                 [:url "https://github.com/KingCode/omni/tree/main/modules/cond-utils"]
                 [:licenses
                  [:license
                   [:name "GPL-2.0-or-later"]
                   [:url "https://opensource.org/license/gpl-2.0"]]]]})
  (b/copy-dir {:src-dirs ["src"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))
