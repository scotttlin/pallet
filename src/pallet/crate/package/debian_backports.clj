(ns pallet.crate.package.debian-backports
  "Actions for working with the debian backports repository"
  (:require
   [pallet.actions :refer [package-source repository]]
   [pallet.plan :refer [defplan]]
   [pallet.script.lib :as lib]
   [pallet.stevedore :as stevedore]
   [pallet.utils :refer [apply-map]]))

(defplan add-debian-backports
  "Add debian backport package repository"
  [session]
  (package-source
   session
   "debian-backports"
   {:url "http://backports.debian.org/debian-backports"
    :release (str
              (stevedore/script (~lib/os-version-name)) "-backports")
    :scopes ["main"]}))

(defmethod repository :debian-backports
  [args]
  (apply-map add-debian-backports args))
