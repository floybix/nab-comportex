(ns nab-comportex.anomaly
  (:require [org.nfrac.comportex.core :as core]
            [org.nfrac.comportex.protocols :as p]
            [org.nfrac.comportex.encoders :as e]
            [clojure.set :as set]))


(defn htm-burst-stream
  [ts make-model]
  (let [lower (dec (apply min (map :value ts)))
        upper (inc (apply max (map :value ts)))]
    (loop [ts ts
           htm (make-model lower upper)
           prev-a-cols #{}
           prev-b-cols #{}
           out []]
      (if-let [rec (first ts)]
        (let [nhtm (p/htm-step htm rec)
              rgn (-> nhtm (core/region-seq) (first))
              lyr (-> rgn (get (first (core/layers rgn))))
              effective? (== (:timestep (:prior-state lyr))
                             (:timestep (:state lyr)))
              t (p/timestep htm)]

          (when (zero? (mod t 2000))
            (println "t =" t))
          (if effective?
            (let [a-cols (p/active-columns lyr)
                  b-cols (p/bursting-columns lyr)
                  new-a-cols (set/difference a-cols prev-a-cols)
                  new-b-cols (set/difference b-cols prev-a-cols)
                  newly-b-cols (set/difference b-cols prev-b-cols)]
              (recur (rest ts)
                     nhtm
                     a-cols
                     b-cols
                     (conj out (assoc rec
                                      :n-a-cols (count a-cols)
                                      :n-b-cols (count b-cols)
                                      :n-new-a-cols (count new-a-cols)
                                      :n-new-b-cols (count new-b-cols)
                                      :n-newly-b-cols (count newly-b-cols)))))

            ;; not an effective step
            (recur (rest ts)
                   nhtm
                   prev-a-cols
                   prev-b-cols
                   (conj out rec))))

        ;; done
        out))))

(defn anomaly-scores
  [burst-stream]
  (for [rec burst-stream]
    (let [{:keys [n-a-cols n-b-cols]} rec
          score (if n-a-cols
                  (/ n-b-cols
                     (max 5 n-a-cols))
                  0)]
      (assoc rec :anomaly (double score)))))

(defn delta-anomaly-scores
  [burst-stream]
  (let [col-difference 0.2]
    (for [rec burst-stream]
      (let [{:keys [n-a-cols n-new-a-cols n-new-b-cols]} rec
            score (if n-a-cols
                    (/ n-new-b-cols
                       (max n-new-a-cols
                            (* n-a-cols col-difference)))
                    0)]
        (assoc rec :anomaly (double score))))))

(defn new-anomaly-scores
  [burst-stream]
  (let [col-difference 0.2]
    (for [rec burst-stream]
      (let [{:keys [n-a-cols n-new-a-cols n-newly-b-cols]} rec
            score (if n-a-cols
                    (/ n-newly-b-cols
                       (max n-new-a-cols
                            (* n-a-cols col-difference)))
                    0)]
        (assoc rec :anomaly (double score))))))
