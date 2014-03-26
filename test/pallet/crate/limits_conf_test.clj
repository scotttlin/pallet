(ns pallet.crate.limits-conf-test
  (:require
   [clojure.test :refer :all]
   [pallet.actions :refer [remote-file]]
   [pallet.build-actions :refer [build-plan]]
   [com.palletops.log-config.timbre :refer [logging-threshold-fixture]]
   [pallet.crate.limits-conf :as limits-conf]))

(use-fixtures :once (logging-threshold-fixture))

(deftest service-test
  (is
   (=
    (build-plan [session {}]
      (remote-file
       session
       "/etc/security/limits.conf"
       {:owner "root"
        :group "root"
        :mode "644"
        :content
        "* - fsize 1024
* - no-file 1024
* - core 1024
* - data 1024"}))
    (build-plan [session {}]
      (limits-conf/settings
       session
       {:entries [["*" "-" "fsize" "1024"]
                  {:domain "*" :type "-" :item "no-file" :value "1024"}]})
      (limits-conf/ulimit
       session
       ["*" "-" "core" "1024"])
      (limits-conf/ulimit
       session
       {:domain "*" :type "-" :item "data" :value "1024"})
      (limits-conf/configure session {})))))
