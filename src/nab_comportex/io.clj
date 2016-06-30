(ns nab-comportex.io
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.predicates :as pr]
            [clojure.data.csv :as csv]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            ))

(def time-formatter
  (f/formatter "yyyy-MM-dd HH:mm:ss"))

(defn read-nab-ts
  [file]
  (with-open [in-file (io/reader file)]
    (doall
     (->> (csv/read-csv in-file)
          (drop 1)
          (map-indexed
           (fn [i [timestamp-str val-str]]
             (let [timestamp (f/parse time-formatter timestamp-str)]
               {:timestamp timestamp
                :index i
                :day-of-week (t/day-of-week timestamp)
                :hour-of-day (t/hour timestamp)
                :is-weekend? (pr/weekend? timestamp)
                :value (Double/parseDouble val-str)})))))))

(defn read-nab-ts-by-id
  [nabdir file-id]
  (read-nab-ts (str nabdir "data/" file-id)))

(defn read-anomaly-windows
  [nabdir]
  (->> (str nabdir "labels/combined_windows.json")
       (slurp)
       (json/read-str)
       (reduce (fn [m [k wins]]
                 (->> wins
                      (mapv (fn [[t1 t2]]
                              (mapv
                               #(f/parse (first (str/split % #"\.")))
                               [t1 t2])))
                      (assoc m k)))
               {})))

(defn write-nab-results-one-file
  [nabdir detector-name file-id anomaly-ts]
  (let [nab-results-dir (str nabdir "results/")
        [file-dir file-name] (str/split file-id #"/")
        csvfile (str nab-results-dir detector-name "/"
                     file-dir "/" detector-name "_" file-name)]
    (with-open [out-file (io/writer csvfile)]
      (csv/write-csv
        out-file
        (cons
          ["timestamp" "value" "anomaly_score" "raw_score" "label"
           "S(t)_reward_low_FP_rate" "S(t)_reward_low_FN_rate" "S(t)_standard"]
          (for [rec anomaly-ts]
            [(f/unparse time-formatter (:timestamp rec))
             (:value rec)
             (:anomaly rec)
             (:anomaly rec)
             "0" ;; label - ?
             "0.0"
             "0.0"
             "0.0"
             ]))))))

(defn write-nab-results
  [nabdir detector-name all-anomalies]
  (doseq [file-id (sort (keys all-anomalies))]
    (write-nab-results-one-file nabdir detector-name file-id
                                (get all-anomalies file-id))))

(defn read-nab-results
  [file]
  (with-open [in-file (io/reader file)]
    (doall
     (->> (csv/read-csv in-file)
          (drop 1)
          (map-indexed
           (fn [i [timestamp-str val-str anomaly-str & more]]
             (let [timestamp (f/parse time-formatter timestamp-str)]
               {:timestamp timestamp
                :index i
                :day-of-week (t/day-of-week timestamp)
                :hour-of-day (t/hour timestamp)
                :is-weekend? (pr/weekend? timestamp)
                :value (Double/parseDouble val-str)
                :anomaly (Double/parseDouble anomaly-str)})))))))

(defn read-nab-results-by-id
  [nabdir detector-name file-id]
  (let [nab-results-dir (str nabdir "results/")
        [file-dir file-name] (str/split file-id #"/")
        csvfile (str nab-results-dir detector-name "/"
                     file-dir "/" detector-name "_" file-name)]
    (read-nab-results csvfile)))
