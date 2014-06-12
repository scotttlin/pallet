(ns pallet.actions.direct.execute
  "Execute actions."
  (:require
   [clojure.set :refer [union]]
   [clojure.string :as string]
   [taoensso.timbre :as logging]
   [pallet.core.plan-state :refer [get-settings update-settings]]
   [pallet.session :refer [target]]
   [pallet.utils :refer [maybe-assoc]]))

(defn normalise-eol
  "Convert eol into platform specific value"
  [#^String s]
  (string/replace s #"[\r\n]+" (str \newline)))

(defn strip-sudo-password
  "Elides the user's password or sudo-password from the given script output."
  [#^String s user]
  (string/replace
   s (format "\"%s\"" (or (:password user) (:sudo-password user))) "XXXXXXX"))

(defn clean-logs
  "Clean passwords from logs"
  [user]
  (comp (fn [s] (strip-sudo-password s user)) normalise-eol))

(defn status-line? [^String line]
  (.startsWith line "#> "))

(defn status-lines
  "Return script status lines from the given sequence of lines."
  [lines]
  (seq (filter status-line? lines)))

(defn log-script-output
  "Return a function to log (multi-line) script output, removing passwords."
  [server user]
  (fn [s]
    (let [s (-> ((clean-logs user) s) string/split-lines)]
      (doseq [^String l s]
        (cond
         (not (status-line? l)) (logging/debugf "%s   <== %s" server l)
         (.endsWith l "FAIL") (logging/errorf "%s %s" server l)
         :else (logging/infof "%s %s" server l))))
    s))

(defn script-error-map
  "Create an error map for a script execution"
  [server msg result]
  (merge
   (select-keys result [:server :err :out :exit])
   {:message (format
              "%s %s%s%s"
              server
              msg
              (let [out (when-let [o (get result :out)]
                          (string/join
                           ", "
                           (status-lines (string/split-lines o))))]
                (if (string/blank? out) "" (str " :out " out)))
              (if-let [err (:err result)] (str " :err " err) ""))
    :type :pallet-script-excution-error
    :server server}))

(defn result-with-error-map
  "Verify the return code of a script execution, and add an error map if
   there is a non-zero result :exit"
  [server msg {:keys [exit] :as result}]
  (if (zero? exit)
    result
    (assoc result :error (script-error-map server msg result))))