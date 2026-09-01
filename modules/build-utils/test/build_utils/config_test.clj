(ns build-utils.config-test
  (:require [build-utils.config :refer [create-config 
                                        sandbox-violation?
                                        check-sandbox-violation
                                        check-strict-violation
                                        check-excluded-violation
                                        check-no-overwrite-violation]]
            [clojure.test :refer [deftest testing is are]]
            [build-utils.util :as u]))

(def user-dir (System/getProperty "user.dir"))

(def fixture-root-dir
"The absolute path of test-resources dir in this module. 
 MUST be used ONLY when running tests from omni super-root"
  (-> user-dir 
      (u/join-path "modules/build-utils/test-resources/fixture-project")))

(defn from-userdir [relative-path]
  (u/join-path user-dir relative-path))

(defn normalized-path? [path]
  (= path (u/normalize path)))

(defn normalized-paths? [{:keys [user-dir root-dir target-dir]}]
  (->> [user-dir root-dir target-dir]
       (every? normalized-path?)))

(defn bool-constraints [kws]
  (->> kws 
       (map #(vector % :yes))))

(defn add-constraints [config & kws]
  (->> kws bool-constraints (into config))
)
(defn new-config 
  ([root-dir target-dir excluded & bool-kws]
   (->> :dry-run? (conj bool-kws) bool-constraints flatten
        (apply create-config 'mylib "version-0" root-dir target-dir)))
  ([bool-kws]
   (apply new-config fixture-root-dir "target" [] bool-kws)))

(defn no-dry-run [config] (dissoc config :dry-run?))

(deftest create-config-test
  (testing "that directory entries are all absolute and normalized"
    (are [root tgt]
        (let [{:keys [user-dir root-dir target-dir] :as conf}
              (new-config root tgt [])]
          (normalized-paths? conf))
      "a" "b"
      "/a/b/../b/c" "a/b/c"
      "a" "..")))

(deftest check-sandbox-violation-test
  (let [nc #(new-config % %2 [] :sandboxed?)]
    (testing "sandbox-violations"
      (are [root-dir tgt-dir] 
          (= :sandbox (check-sandbox-violation (nc root-dir tgt-dir)))
        "a" ".."
        ".." "b"
        ".." "../.."
        "a/b" "../../../"
        "a/b" "../../.."
        "a/b" "../.."))

    (testing "sandbox compliance"
      (are [root-dir tgt-dir]
          (= nil (check-sandbox-violation (nc root-dir tgt-dir)))
        "a/b/c" "../.."
        "a" "b"
        "a" "a/b"))))

