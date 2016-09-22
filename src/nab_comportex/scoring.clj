(ns nab-comportex.scoring
  (:require [clj-time.core :as t]))


(def profiles
  {:standard {:TP 1.0
              :FN -1.0
              :FP -0.11}
   :low-fp-rate {:TP 1.0
                 :FN -1.0
                 :FP -0.22}
   :low-fn-rate {:TP 1.0
                 :FN -2.0
                 :FP -0.11}})

(defn sigm
  [t]
  (/ 1.0
     (+ 1.0 (Math/exp (- t)))))

(defn score
  [[start end] t TP FP]
  (let [win-length (t/in-seconds (t/interval start end))
        missed? (t/after? t end)
        offset (if missed?
                 (t/in-seconds (t/interval end t))
                 (- (t/in-seconds (t/interval t end))))
        y (/ offset win-length)]
    (-> (sigm (* 5 (- y)))
        ;(* (- TP FP)) ;; NB wrong
        (* 2)
        (- 1.0)
        (* (if missed?
             (- FP)
             (/ TP 0.98661)))))) ;; adjust by maximum scaled sigmoid


(defn warmup-period
  [ts]
  (int (* 0.15 (min 5000 (count ts)))))

(defn detections
  [anom-ts threshold wait]
  (loop [as anom-ts
         out []
         counter 0]
    (if-let [arec (first as)]
      (let [a (:anomaly arec)
            det? (and (>= a threshold)
                      (>= counter wait))]
        (recur (rest as)
               (conj out (assoc arec :detected? det?))
               (if det? 0 (inc counter))))
      ;; done
      out)))

(defn in-window?
  [[start end] t]
  (and (not (t/before? t start))
       (not (t/after? t end))))

(defn score-one-ts
  [detec-ts windows {:keys [TP FN FP] :as profile}]
  (let [warmup (warmup-period detec-ts)]
    (loop [ts detec-ts
           last-win nil
           this-win nil
           wins windows
           win-detected? false
           scores []]
      (if (seq ts)
        (let [rec (first ts)
              t (:timestamp rec)
              i (:index rec)
              end-of-win? (when-let [[t0 t1] this-win]
                            (not (t/before? t t1)))
              start-of-win? (when (not this-win)
                              (when-let [[t0 t1] (first wins)]
                                (not (t/before? t t0))))
              in-win? (or start-of-win? (and this-win (in-window? this-win t)))
              detected? (and (>= i warmup)
                             (:detected? rec))]
          (recur (rest ts)
                 ;; last-win
                 (cond
                   start-of-win? nil
                   end-of-win? this-win
                   :else last-win)
                 ;; this-win
                 (cond
                   start-of-win? (first wins)
                   end-of-win? nil
                   :else this-win)
                 ;; wins
                 (if start-of-win? (rest wins) wins)
                 ;; win-detected?
                 (cond
                   (and in-win? detected?) true
                   end-of-win? nil
                   :else win-detected?)
                 ;; scores
                 (cond
                   ;; false negative
                   (and end-of-win? (not win-detected?) (not detected?))
                   (conj scores (assoc rec
                                       :type :FN
                                       :nab-score FN))
                   ;; false positive
                   (and (not in-win?) detected?)
                   (conj scores (assoc rec
                                       :type :FP
                                       :nab-score (if last-win
                                                    (score last-win t TP FP)
                                                    FP)))
                   ;; true positive
                   (and in-win? (not win-detected?) detected?)
                   (conj scores (assoc rec
                                       :type :TP
                                       :nab-score (score this-win t TP FP)))
                   ;; else
                   :else scores)))
        ;; done
        scores))))


(defn ts-results
  [anomaly-ts windows profile threshold wait]
  (let [detec-ts (detections anomaly-ts threshold wait)
        scores (score-one-ts detec-ts windows profile)
        nab-sum (reduce + (map :nab-score scores))]
    {:nab-sum nab-sum
     :confusion (frequencies (map :type scores))
     :scores scores
     :n-anomalies (count windows)
     :profile profile}))


(defn combine-results
  [many-ts-results]
  (let [vs many-ts-results
        total-n (->> vs (map :n-anomalies) (reduce +))
        total-nab-sum (->> vs (map :nab-sum) (reduce +))
        ;cfn (->> vs (mapcat :scores) (map :type) (frequencies))
        cfn (->> vs (map :confusion) (apply merge-with +))]
    {:nab-sum total-nab-sum
     :confusion cfn
     :n-anomalies total-n
     :profile (:profile (first vs))}))


(defn normalised-nab-score
  ([results]
   (normalised-nab-score (:nab-sum results)
                         (:confusion results)
                         (:n-anomalies results)
                         (:profile results)))
  ([nab-score confusion n-anomalies profile]
   (let [TPs (:TP confusion)
         FNs (:FN confusion)
         null-score (* n-anomalies (:FN profile))
         perfect-score (* n-anomalies (:TP profile))]
     (if (zero? n-anomalies)
       nab-score
       (* 100 (/ (- nab-score null-score)
                 (- perfect-score null-score)))))))
