(ns com.lambda-startup.transducers)

;;
;; motivation: reusability of code
;;

;; a classical reduce example:
(reduce (fn [r x]
          (conj r (inc x)))
        [] [1 2 3])
;; => [2 3 4]
;; the fn knows about multiple things:
;; 1. about what to do with x, i.e. inc
;;    let's call this "business logic"
;; 2. about how to reduce/aggregate values, i.e. conj
;;    let's call this "aggregation"
;; 3. about if and when to do something with x, in this case: always = mapping
;;    let's call this "process";
;; wouldn't it be nice if we could separate these things from each other?
;; we would be able to reuse each part in different contexts instead of
;; having one single piece of code that knows about everything.

;; a) separate the business logic, i.e. inc:
(defn make-reducing-fn [f]
  (fn rf-fn [r x]
    (conj r (f x))))

(reduce (make-reducing-fn inc)
        [] [1 2 3])
;; => [2 3 4]
;; nice. just using higher order functions.

;; b) separate the aggregation, i.e. conj:
(defn make-mapper [f]
  (fn xf-fn [rf]
    (fn rf-fn [r x]
      (rf r (f x)))))

(reduce ((make-mapper inc) conj)
        [] [1 2 3])
;; => [2 3 4]
;; taking the higher order functions stuff one step further.
;; that's basically it. we've (roughly) created the concept of a transducer.
;; we've separated the three domains of knowledge from each other:
;; 1. the knowledge about our business logic is now represented by f
;; 2. the knowledge about aggregation is now represented by rf
;; 3. the knowledge about the process is encapsulated within the body of rf-fn.
;; xf-fn is what is being called a transducer - it takes a reducing function
;; and returns one.


;;
;; playing around some more
;;

(defn mapping                   ;; this fn is a transducer-creating function
  [f]
  (println "mapping ctor:" f)
  (fn mapping-xf [rf]           ;; this fn is a transducer
    (println "mapping xf:" rf)
    (fn mapping-rf              ;; this fn is a reducing function
                                ;; (that wraps another reducing function "rf")
      ([] (println "mapping rf 0:") (rf))                     ;; init variant
      ([r] (println "mapping rf 1:" r) (rf r))                ;; completion variant
      ([r x] (println "mapping rf 2:" r x) (rf r (f x))))))   ;; reducing variant

(defn filtering
  [pred]
  (println "filtering ctor:" pred)
  (fn filtering-xf [rf]
    (println "filtering xf:" rf)
    (fn filtering-rf
      ([] (println "filtering rf 0:") (rf))
      ([r] (println "filtering rf 1:" r) (rf r))
      ([r x] (println "filtering rf 2:" r x) (if (pred x) (rf r x) r)))))

(def inceven-xf                 ;; this is a transducer that first maps inc
                                ;; and then filters evens
  (comp (mapping inc)           ;; this returns a transducer
        (filtering even?)))     ;; this returns a transducer

((inceven-xf conj) [2] 3)       ;; applying a reducing function that knows how to build sequences
((inceven-xf +) 10 3)           ;; applying a reducing function that knows how to add up numbers

(def inceven-seq (inceven-xf conj))  ;; a reducing function that knows how to build sequances
(def inceven-plus (inceven-xf +))    ;; a reducing function that knows how to add up numbers
(inceven-seq [2] 3)
(inceven-plus 10 3)

(reduce (inceven-xf conj) ;; NOTE: behaves differently from transduce regarding handling of 0- & 1-arity variants, so it is NOT what you want!
        [] [1 2 3 4 5])

(transduce inceven-xf conj
           [] [1 2 3 4 5])



(((map inc)
  +)
 1 2)


(((fn [rf]
    (fn [s e]
      (rf s (inc e))))
  +)
 1 2)


(reduce ((map inc)
         +)
        0 [1 2 3])


(reduce ((fn [rf]
           (fn [s e]
             (rf s (inc e))))
         +)
        0 [1 2 3])


(defn myinc
  [& args]
  (println "myinc:" args)
  (apply inc args))


(transduce (map myinc)
           + 0 [1 2 3])


(transduce (fn [rf] ;; this is the transducer
             (fn
               ([] (println "arity 0") (rf))
               ([s] (println "arity 1" s) (rf s))
               ([s e] (println "arity 2" s e) (rf s (myinc e)))
               ([s e & es] (println "arity 3" s e es) (rf s (apply myinc e es)))))
           + 0 [1 2 3])
