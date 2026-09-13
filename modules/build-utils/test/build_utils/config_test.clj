(ns build-utils.config-test
  (:require [build-utils.config :as sut
             :refer [create-config 
                     sandbox-violation?
                     excluded-violation?
                     check-sandbox-violation
                     check-strict-violation
                     check-excluded-violation
                     check-no-overwrite-violation
                     check-config]]
            [clojure.test :refer [deftest testing is are]]
            [babashka.fs :as fs]
            [build-utils.util :as u]
            [build-utils.test-util :as tu]))

(def user-dir 
  "The super-root of this project, .i.e. the parent of modules/build-utils
   and all other modules in the monorepo structure."
 (System/getProperty "user.dir"))

(def test-resources-dir
"The absolute path of test-resources dir in build-utils (this) module. 
 Intended to be used ONLY when running tests in this namespace 
 from omni super-root"
  (-> user-dir (fs/path "modules/build-utils/test-resources") str))

(def fixture-root-dir
"The absolute path of fixture-project dir in build-utils (this) module. 
 Intended to be used ONLY when running tests in this namespace 
 from omni super-root"
  (-> test-resources-dir (fs/path "fixture-project") str))


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
  (->> kws bool-constraints (into config)))

(defn new-config 
  ([root-dir target-dir excluded & bool-kws]
   (->> :dry-run? (conj bool-kws) bool-constraints flatten
        (into [:excluded excluded])
        (apply create-config 'mylib "version-0" 
               root-dir target-dir )))
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
      "a" ".."))
  (testing "that all constraints are as expected"
    (let [base-config (new-config [])]
      (are [kws]
          (let [c (apply add-constraints base-config kws)]
            (->> kws (every? #(get c %))))
        [:sandboxed?]
        [:strict? :no-overwrite?]
        [:a :b :c :d]))))

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


(deftest check-strict-violation-test
  (let [nc #(new-config % %2 [] :strict?)]
    (testing "strict violations"
      (are [root-dir tgt-dir]
          (= :strict (check-strict-violation (nc root-dir tgt-dir)))
        "a" ".."
        "a" "../a"
        "a" "../../.."))
    (testing "strict mode compliance"
      (are [root-dir tgt-dir]
          (nil? (check-strict-violation (nc root-dir tgt-dir)))
        "a" "a"
        "a" "b"
        "modules/build-utils" "target"))))

(deftest check-excluded-violation-test
  (let [nc #(new-config % %2 %3)]
    (testing "excluded violations"
      (are [root-dir tgt-dir excl]
          (let [c (nc root-dir tgt-dir excl)]
            (= :excluded (check-excluded-violation c)))
        "a" "src/b" ["src" "test"]
        "a" "test/b" ["src" "test"]
        "a" "src/b/test" ["src" "test"]
        "a" "src/b/test" ["test"]))
    (testing "excluded compliance"
      (are [root-dir tgt-dir excl]
          (let [c (nc root-dir tgt-dir excl)]
            (nil? (check-excluded-violation c)))
        "a" "b" []
        "a" "b" ["c" "d"]
        "a" "b/target" ["src" "test"]))))

(deftest check-overwrite-violation-test 
  (let [nc #(new-config test-resources-dir % [] :no-overwrite?)]
    (testing "no-overwrite violations detection"
      #_(are [relpath-to-create]
          (tu/within-test-resources-dir relpath-to-create 
            ;; new name for clarity
            (let [existing-dir relpath-to-create
                  cf (nc existing-dir)]
              (= :no-overwrite (check-no-overwrite-violation cf))))
        "tmp/a/b/c"
        #_"tmp"))
    (testing "no-overwrite violation compliance"
      (let [cf (nc "target")]
        (are [relpath-to-create]
           (nil? (check-no-overwrite-violation cf))
          "tmp/a/b/c"
          "tmp")))))


(defn new-config-under-test-resources-tmp [tmpdir-path tgt excl kws]
  (apply new-config test-resources-dir 
         (->> tgt (fs/path tmpdir-path) fs/normalize str) 
         excl kws))

(deftest check-config-test
  (let [tmpdir-path "tmp"
        nc (partial new-config-under-test-resources-tmp tmpdir-path)]
    (tu/within-test-resources-dir tmpdir-path
      (testing "that each violation if any, is collected"
        (are [tgt excl bool-kws expected]
            (tu/within-test-resources-tmpdir 
                tmpdir-path tgt 
              (let [cfg (nc tgt excl bool-kws)]
                (is  (= (set expected) 
                        (-> (is (thrown? clojure.lang.ExceptionInfo 
                                         (check-config cfg)))
                            ex-data
                            (get :violations)
                            set)))))

          "a" [] [:no-overwrite?]
          [:no-overwrite]

          "tmp" ["tmp"] [:strict? :no-overwrite? :sandboxed?]
          [:no-overwrite :excluded]))

      (testing "compliant configurations"
        (are [tgt excl bool-kws]
            (nil? (check-config (nc tgt excl bool-kws)))
          "target" [] [:no-overwrite?]))))
)
