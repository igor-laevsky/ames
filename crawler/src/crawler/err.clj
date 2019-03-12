(ns crawler.err
  (:import (clojure.lang IDeref)))

(deftype Err [payload]
  IDeref
  (deref [this] payload))

(defn rr [payload]
  (->Err payload))

(defn rr? [val]
  (instance? Err val))

(defmacro rr-> [expr & forms]
  (let [g (gensym)
        steps (map (fn [step] `(if (rr? ~g) ~g (-> ~g ~step)))
                   forms)]
    `(let [~g ~expr
           ~@(interleave (repeat g) (butlast steps))]
       ~(if (empty? steps)
          g
          (last steps)))))

(defmacro rr->> [expr & forms]
  (let [g (gensym)
        steps (map (fn [step] `(if (rr? ~g) ~g (->> ~g ~step)))
                   forms)]
    `(let [~g ~expr
           ~@(interleave (repeat g) (butlast steps))]
       ~(if (empty? steps)
          g
          (last steps)))))
