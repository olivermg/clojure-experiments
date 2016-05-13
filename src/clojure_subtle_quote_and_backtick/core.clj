(ns clojure-subtle-quote-and-backtick.core)

(in-ns 'user)

;; Both quote and backtick prevent evaluation of the
;; subsequent expression:
'(1 2 3) ;; => (1 2 3)
`(1 2 3) ;; => (1 2 3)


;; Backtick allows you to use the tilde character (~)
;; to re-enable evaluation of parts in the expression.
;; Contrarily, quote does not interpret the tilde sign,
;; but takes it literally:
(def x 3)
`(1 2 ~x) ;; (1 2 3)
'(1 2 ~x) ;; (1 2 (clojure.core/unquote x))


;; Besides the tilde sign, you can even use '~@' to
;; unquote and splice a sequence.
;; Quote does not interpret this, again:
(def y (list 3 4))
`(1 2 ~@y) ;; => (1 2 3 4)
'(1 2 ~@y) ;; => (1 2 (clojure.core/unquote-splicing y))


;; Finally (and this may be the difference that is
;; not as obvious as the preceding onces), backtick
;; qualifies the symbols within its expression with a
;; namespace.
;; Quote does not qualify anything:
(def z 111)
`(1 2 z) ;; => (1 2 user/z)
'(1 2 z) ;; => (1 2 z)

;; So why is this relevant?
;; Consider the following macros in a namespace "macro-ns":

(in-ns 'macro-ns)
(clojure.core/use 'clojure.core)

(def z 222)

(defmacro m-quote []
  'z)
(defmacro m-backtick []
  `z)

;; Now, we'll want to use these macros in the "user" namespace:

(in-ns 'user)

(def z 111)

(macroexpand-1 '(macro-ns/m-quote))    ;; => z
(macroexpand-1 '(macro-ns/m-backtick)) ;; => macro-ns/z

(macro-ns/m-quote)    ;; => 111
(macro-ns/m-backtick) ;; => 222

;; So, using quote refers to the symbol found in the macro-user's
;; namespace, while using backtick refers to the symbol found in
;; the namespace where the macro is defined.
