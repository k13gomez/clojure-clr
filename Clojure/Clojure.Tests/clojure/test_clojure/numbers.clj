;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

; Author: Stephen C. Gilardi
;;  scgilardi (gmail)
;;  Created 30 October 2008
;;

(ns clojure.test-clojure.numbers
  (:use clojure.test
        [clojure.test.generative :exclude (is)]
        clojure.template)
  (:require [clojure.data.generators :as gen]
            [clojure.test-helper :as helper]))


; TODO:
; ==
; and more...


;; *** Types ***


(deftest Coerced-BigDecimal
  (doseq [v [(bigdec 3) (bigdec (inc (bigint Int64/MaxValue)))]]              ;;; Long/MAX_VALUE
    (are [x] (true? x)
     (instance? BigDecimal v)
     (number? v)
     (decimal? v)
     (not (float? v)))))

(deftest BigInteger-conversions
  (doseq [coerce-fn [bigint biginteger]]
    (doseq [v (map coerce-fn [ Int32/MaxValue                                  ;;; Long/MAX_VALUE
                              13178456923875639284562345789M
                              13178456923875639284562345789N
                              Single/MaxValue                                  ;;; Float/MAX_VALUE
                              (- Single/MaxValue)                              ;;; (- Float/MAX_VALUE)
                              Double/MaxValue                                  ;;; Double/MAX_VALUE
                              (- Double/MaxValue)                              ;;; (- Double/MAX_VALUE)
                              (* 2 (bigdec Double/MaxValue)) ])]               ;;;  Double/MAX_VALUE
      (are [x] (true? x)
        (integer? v)
        (number? v)
        (not (decimal? v))
        (not (float? v))))))

(defn all-pairs-equal [equal-var vals]
  (doseq [val1 vals]
    (doseq [val2 vals]
      (is (equal-var val1 val2)
          (str "Test that " val1 " (" (class val1) ") "
               equal-var " " val2 " (" (class val2) ")")))))

(defn all-pairs-hash-consistent-with-= [vals]
  (doseq [val1 vals]
    (doseq [val2 vals]
      (when (= val1 val2)
        (is (= (hash val1) (hash val2))
            (str "Test that (hash " val1 ") (" (class val1) ") "
                 " = (hash " val2 ") (" (class val2) ")"))))))

(deftest equality-tests
  ;; = only returns true for numbers that are in the same category,
  ;; where category is one of INTEGER, FLOATING, DECIMAL, RATIO.
  (all-pairs-equal #'= [(byte 2) (short 2) (int 2) (long 2)
                        (bigint 2) (biginteger 2)])
  (all-pairs-equal #'= [(float 2.0) (double 2.0)])
  (all-pairs-equal #'= [(float 0.0) (double 0.0) (float -0.0) (double -0.0)])
  (all-pairs-equal #'= [2.0M 2.00M])
  (all-pairs-equal #'= [(float 1.5) (double 1.5)])
  (all-pairs-equal #'= [1.50M 1.500M])
  (all-pairs-equal #'= [0.0M 0.00M])
  (all-pairs-equal #'= [(/ 1 2) (/ 2 4)])

  ;; No BigIntegers or floats in following tests, because hash
  ;; consistency with = for them is out of scope for Clojure
  ;; (CLJ-1036).
  (all-pairs-hash-consistent-with-= [(byte 2) (short 2) (int 2) (long 2)
                                     (bigint 2)
                                     (double 2.0) 2.0M 2.00M])
  (all-pairs-hash-consistent-with-= [(/ 3 2) (double 1.5) 1.50M 1.500M])
  (all-pairs-hash-consistent-with-= [(double -0.0) (double 0.0) -0.0M -0.00M 0.0M 0.00M (float -0.0) (float 0.0)])

  ;; == tests for numerical equality, returning true even for numbers
  ;; in different categories.
  (all-pairs-equal #'== [(byte 0) (short 0) (int 0) (long 0)
                         (bigint 0) (biginteger 0)
						 (float -0.0) (double -0.0) -0.0M -0.00M
                         (float 0.0) (double 0.0) 0.0M 0.00M])
  (all-pairs-equal #'== [(byte 2) (short 2) (int 2) (long 2)
                         (bigint 2) (biginteger 2)
                         (float 2.0) (double 2.0) 2.0M 2.00M])
  (all-pairs-equal #'== [(/ 3 2) (float 1.5) (double 1.5) 1.50M 1.500M]))

(deftest unchecked-cast-num-obj
  (do-template [prim-array cast]
    (are [n]
      (let [a (prim-array 1)]
        (aset a 0 (cast n)))
      Byte/MaxValue           ;;; (Byte. Byte/MAX_VALUE)
      Int16/MaxValue
      Int32/MaxValue
      Int64/MaxValue
      Single/MaxValue
      Double/MaxValue)
    byte-array
    unchecked-byte
    short-array
    unchecked-short
    char-array
    unchecked-char
    int-array
    unchecked-int
    long-array
    unchecked-long
    float-array
    unchecked-float
    double-array
    unchecked-double))

(deftest unchecked-cast-num-prim
  (do-template [prim-array cast]
    (are [n]
      (let [a (prim-array 1)]
        (aset a 0 (cast n)))
      Byte/MaxValue           ;;; MAX_VALUE
      Int16/MaxValue
      Int32/MaxValue
      Int64/MaxValue
      Single/MaxValue
      Double/MaxValue)
    byte-array
    unchecked-byte
    short-array
    unchecked-short
    char-array
    unchecked-char
    int-array
    unchecked-int
    long-array
    unchecked-long
    float-array
    unchecked-float
    double-array
    unchecked-double))

(deftest unchecked-cast-char
  ; in keeping with the checked cast functions, char and Character can only be cast to int
  (is (unchecked-int (char 0xFFFF)))
  (is (let [c (char 0xFFFF)] (unchecked-int c)))) ; force primitive char

(compile-when (>= (:major dotnet-version) 9)

(def expected-casts   ;; for byte 127 => 255  (not signed), and other differences
  [
   [:input [-1 0 1 Byte/MaxValue Int16/MaxValue Int32/MaxValue Int64/MaxValue Single/MaxValue Double/MaxValue]]
   [char [:error (char 0) (char 1) (char 255) (char 32767) :error :error :error :error]]
   [unchecked-char [(char 65535) (char 0) (char 1) (char 255) (char 32767) (char 65535) (char 65535) (char 65535) (char 65535)]]
   [byte [:error 0 1 Byte/MaxValue :error :error :error :error :error]]
   [unchecked-byte [255 0 1 Byte/MaxValue 255 255 255 255 255]]
   [short [-1 0 1 Byte/MaxValue Int16/MaxValue :error :error :error :error]]
   [unchecked-short [-1 0 1 Byte/MaxValue Int16/MaxValue -1 -1 -1 -1]]
   [int [-1 0 1 Byte/MaxValue Int16/MaxValue Int32/MaxValue :error :error :error]]
   [unchecked-int [-1 0 1 Byte/MaxValue Int16/MaxValue Int32/MaxValue -1 Int32/MaxValue Int32/MaxValue]]
   [long [-1 0 1 Byte/MaxValue Int16/MaxValue Int32/MaxValue Int64/MaxValue :error :error]]
   [unchecked-long [-1 0 1 Byte/MaxValue Int16/MaxValue Int32/MaxValue Int64/MaxValue Int64/MaxValue Int64/MaxValue]]
                                                                                             ;; 2.14748365E9 if when float/double conversion is avoided...
   [float [-1.0 0.0 1.0 255.0 32767.0 2.147483648E9 9.223372036854776E18 Single/MaxValue :error]]
   [unchecked-float [-1.0 0.0 1.0 255.0 32767.0 2.147483648E9 9.223372036854776E18 Single/MaxValue Single/PositiveInfinity]]
   [double [-1.0 0.0 1.0 255.0 32767.0 2.147483647E9 9.223372036854776E18 Single/MaxValue Double/MaxValue]]
   [unchecked-double [-1.0 0.0 1.0 255.0 32767.0 2.147483647E9 9.223372036854776E18 Single/MaxValue Double/MaxValue]]])
)

(compile-when (< (:major dotnet-version) 9)

(def expected-casts   ;; for byte 127 => 255  (not signed), and other differences
  [
   [:input [-1 0 1 Byte/MaxValue Int16/MaxValue Int32/MaxValue Int64/MaxValue Single/MaxValue Double/MaxValue]]
   [char [:error (char 0) (char 1) (char 255) (char 32767) :error :error :error :error]]
   [unchecked-char [(char 65535) (char 0) (char 1) (char 255) (char 32767) (char 65535) (char 65535) (char 0) (char 0)]]
   [byte [:error 0 1 Byte/MaxValue :error :error :error :error :error]]
   [unchecked-byte [255 0 1 Byte/MaxValue 255 255 255 0 0]]
   [short [-1 0 1 Byte/MaxValue Int16/MaxValue :error :error :error :error]]
   [unchecked-short [-1 0 1 Byte/MaxValue Int16/MaxValue -1 -1 0 0]]
   [int [-1 0 1 Byte/MaxValue Int16/MaxValue Int32/MaxValue :error :error :error]]
   [unchecked-int [-1 0 1 Byte/MaxValue Int16/MaxValue Int32/MaxValue -1 Int32/MinValue Int32/MinValue]]
   [long [-1 0 1 Byte/MaxValue Int16/MaxValue Int32/MaxValue Int64/MaxValue :error :error]]
   [unchecked-long [-1 0 1 Byte/MaxValue Int16/MaxValue Int32/MaxValue Int64/MaxValue Int64/MinValue Int64/MinValue]]
                                                                                             ;; 2.14748365E9 if when float/double conversion is avoided...
   [float [-1.0 0.0 1.0 255.0 32767.0 2.147483648E9 9.223372036854776E18 Single/MaxValue :error]]
   [unchecked-float [-1.0 0.0 1.0 255.0 32767.0 2.147483648E9 9.223372036854776E18 Single/MaxValue Single/PositiveInfinity]]
   [double [-1.0 0.0 1.0 255.0 32767.0 2.147483647E9 9.223372036854776E18 Single/MaxValue Double/MaxValue]]
   [unchecked-double [-1.0 0.0 1.0 255.0 32767.0 2.147483647E9 9.223372036854776E18 Single/MaxValue Double/MaxValue]]])
)

(deftest test-expected-casts
  (let [[[_ inputs] & expectations] expected-casts]
    (doseq [[f vals] expectations]
      (let [wrapped (fn [x]
                      (try
                       (f x)
                       (catch ArgumentException e :error)))]           ;;; IllegalArgumentException
        (is (= vals (map wrapped inputs)))))))

(deftest test-prim-with-matching-hint
  (is (= 1.0 (let [x 1.2] (Math/Round ^double x)))))                     ;;; 1 round

;; *** Functions ***

(defonce DELTA 1e-12)

(deftest test-add
  (are [x y] (= x y)
      (+) 0
      (+ 1) 1
      (+ 1 2) 3
      (+ 1 2 3) 6

      (+ -1) -1
      (+ -1 -2) -3
      (+ -1 +2 -3) -2

      (+ 1 -1) 0
      (+ -1 1) 0

      (+ 2/3) 2/3
      (+ 2/3 1) 5/3
      (+ 2/3 1/3) 1 )

  (are [x y] (< (- x y) DELTA)
      (+ 1.2) 1.2
      (+ 1.1 2.4) 3.5
      (+ 1.1 2.2 3.3) 6.6 )

  (is (> (+ Int32/MaxValue 10) Int32/MaxValue))  ; no overflow               ;;; Integer/MAX_VALUE
  (is (thrown? FormatException (+ "ab" "cd"))) )    ; no string concatenation           ;;; ClassCastException


(deftest test-subtract
  (is (thrown? ArgumentException (-)))         ;;; IllegalArgumentException
  (are [x y] (= x y)
      (- 1) -1
      (- 1 2) -1
      (- 1 2 3) -4

      (- -2) 2
      (- 1 -2) 3
      (- 1 -2 -3) 6

      (- 1 1) 0
      (- -1 -1) 0

      (- 2/3) -2/3
      (- 2/3 1) -1/3
      (- 2/3 1/3) 1/3 )

  (are [x y] (< (- x y) DELTA)
      (- 1.2) -1.2
      (- 2.2 1.1) 1.1
      (- 6.6 2.2 1.1) 3.3 )

  (is (< (- Int32/MinValue 10) Int32/MinValue)) )  ; no underflow        ;;; Integer/MIN_VALUE


(deftest test-multiply
  (are [x y] (= x y)
      (*) 1
      (* 2) 2
      (* 2 3) 6
      (* 2 3 4) 24

      (* -2) -2
      (* 2 -3) -6
      (* 2 -3 -1) 6

      (* 1/2) 1/2
      (* 1/2 1/3) 1/6
      (* 1/2 1/3 -1/4) -1/24 )

  (are [x y] (< (- x y) DELTA)
      (* 1.2) 1.2
      (* 2.0 1.2) 2.4
      (* 3.5 2.0 1.2) 8.4 )

  (is (> (* 3 (int (/ Int32/MaxValue 2.0))) Int32/MaxValue)) )  ; no overflow  ;;; Integer/MAX_VALUE

(deftest test-multiply-longs-at-edge
  (are [x] (= x 9223372036854775808N)
       (*' -1 Int64/MinValue)                                     ;;; Long/MIN_VALUE
       (*' Int64/MinValue -1)                                     ;;; Long/MIN_VALUE
       (* -1N Int64/MinValue)                                     ;;; Long/MIN_VALUE
       (* Int64/MinValue -1N)                                     ;;; Long/MIN_VALUE
       (* -1 (bigint Int64/MinValue))                             ;;; Long/MIN_VALUE
       (* (bigint Int64/MinValue) -1))                            ;;; Long/MIN_VALUE
  (is (thrown? ArithmeticException (* Int64/MinValue -1)))        ;;; Long/MIN_VALUE
  (is (thrown? ArithmeticException (* -1 Int64/MinValue))))       ;;; Long/MIN_VALUE

(deftest test-ratios-simplify-to-ints-where-appropriate
  (testing "negative denominator (assembla #275)"
    (is (integer? (/ 1 -1/2)))
    (is (integer? (/ 0 -1/2)))))

(deftest test-divide
  (are [x y] (= x y)
      (/ 1) 1
      (/ 2) 1/2
      (/ 3 2) 3/2
      (/ 4 2) 2
      (/ 24 3 2) 4
      (/ 24 3 2 -1) -4

      (/ -1) -1
      (/ -2) -1/2
      (/ -3 -2) 3/2
      (/ -4 -2) 2
      (/ -4 2) -2 )

  (are [x y] (< (- x y) DELTA)
      (/ 4.5 3) 1.5
      (/ 4.5 3.0 3.0) 0.5 )

  (is (thrown? ArithmeticException (/ 0)))
  (is (thrown? ArithmeticException (/ 2 0)))
  (is (thrown? ArgumentException (/))) )              ;;; IllegalArgumentException

(deftest test-divide-bigint-at-edge
  (are [x] (= x (-' Int64/MinValue))                  ;;; Long/MIN_VALUE
       (/ Int64/MinValue -1N)                         ;;; Long/MIN_VALUE
       (/ (bigint Int64/MinValue) -1)                 ;;; Long/MIN_VALUE
       (/ (bigint Int64/MinValue) -1N)                ;;; Long/MIN_VALUE
       (quot Int64/MinValue -1N)                      ;;; Long/MIN_VALUE
       (quot (bigint Int64/MinValue) -1)              ;;; Long/MIN_VALUE
       (quot (bigint Int64/MinValue) -1N)))           ;;; Long/MIN_VALUE

;; mod
;; http://en.wikipedia.org/wiki/Modulo_operation
;; http://mathforum.org/library/drmath/view/52343.html
;;
;; is mod correct?
;; http://groups.google.com/group/clojure/browse_frm/thread/2a0ee4d248f3d131#
;;
;; Issue 23: mod (modulo) operator
;; http://code.google.com/p/clojure/issues/detail?id=23

(deftest test-mod
  ; wrong number of args
;  (is (thrown? ArgumentException (mod)))           ;;; IllegalArgumentException
;  (is (thrown? ArgumentException (mod 1)))         ;;; IllegalArgumentException
;  (is (thrown? ArgumentException (mod 3 2 1)))     ;;; IllegalArgumentException

  ; divide by zero
  (is (thrown? ArithmeticException (mod 9 0)))
  (is (thrown? ArithmeticException (mod 0 0)))

  (are [x y] (= x y)
    (mod 4 2) 0
    (mod 3 2) 1
    (mod 6 4) 2
    (mod 0 5) 0

    (mod 2 1/2) 0
    (mod 2/3 1/2) 1/6
    (mod 1 2/3) 1/3

    (mod 4.0 2.0) 0.0
    (mod 4.5 2.0) 0.5

    ; |num| > |div|, num != k * div
    (mod 42 5) 2      ; (42 / 5) * 5 + (42 mod 5)        = 8 * 5 + 2        = 42
    (mod 42 -5) -3    ; (42 / -5) * (-5) + (42 mod -5)   = -9 * (-5) + (-3) = 42
    (mod -42 5) 3     ; (-42 / 5) * 5 + (-42 mod 5)      = -9 * 5 + 3       = -42
    (mod -42 -5) -2   ; (-42 / -5) * (-5) + (-42 mod -5) = 8 * (-5) + (-2)  = -42

    ; |num| > |div|, num = k * div
    (mod 9 3) 0       ; (9 / 3) * 3 + (9 mod 3) = 3 * 3 + 0 = 9
    (mod 9 -3) 0
    (mod -9 3) 0
    (mod -9 -3) 0

    ; |num| < |div|
    (mod 2 5) 2       ; (2 / 5) * 5 + (2 mod 5)        = 0 * 5 + 2          = 2
    (mod 2 -5) -3     ; (2 / -5) * (-5) + (2 mod -5)   = (-1) * (-5) + (-3) = 2
    (mod -2 5) 3      ; (-2 / 5) * 5 + (-2 mod 5)      = (-1) * 5 + 3       = -2
    (mod -2 -5) -2    ; (-2 / -5) * (-5) + (-2 mod -5) = 0 * (-5) + (-2)    = -2

    ; num = 0, div != 0
    (mod 0 3) 0       ; (0 / 3) * 3 + (0 mod 3) = 0 * 3 + 0 = 0
    (mod 0 -3) 0

    ; large args
    (mod 3216478362187432 432143214) 120355456
  )
)

;; rem & quot
;; http://en.wikipedia.org/wiki/Remainder

(deftest test-rem
  ; wrong number of args
;  (is (thrown? ArgumentException (rem)))           ;;; IllegalArgumentException
;  (is (thrown? ArgumentException (rem 1)))         ;;; IllegalArgumentException
;  (is (thrown? ArgumentException (rem 3 2 1)))     ;;; IllegalArgumentException

  ; divide by zero
  (is (thrown? ArithmeticException (rem 9 0)))
  (is (thrown? ArithmeticException (rem 0 0)))
  
  (are [x y] (= x y)
    (rem 4 2) 0
    (rem 3 2) 1
    (rem 6 4) 2
    (rem 0 5) 0

    (rem 2 1/2) 0
    (rem 2/3 1/2) 1/6
    (rem 1 2/3) 1/3

    (rem 4.0 2.0) 0.0
    (rem 4.5 2.0) 0.5

    ; |num| > |div|, num != k * div
    (rem 42 5) 2      ; (8 * 5) + 2 == 42
    (rem 42 -5) 2     ; (-8 * -5) + 2 == 42
    (rem -42 5) -2    ; (-8 * 5) + -2 == -42
    (rem -42 -5) -2   ; (8 * -5) + -2 == -42

    ; |num| > |div|, num = k * div
    (rem 9 3) 0
    (rem 9 -3) 0
    (rem -9 3) 0
    (rem -9 -3) 0

    ; |num| < |div|
    (rem 2 5) 2
    (rem 2 -5) 2
    (rem -2 5) -2
    (rem -2 -5) -2
    
    ; num = 0, div != 0
    (rem 0 3) 0
    (rem 0 -3) 0
  )
)

(deftest test-quot
  ; wrong number of args
;  (is (thrown? ArgumentException (quot)))           ;;; IllegalArgumentException
;  (is (thrown? ArgumentException (quot 1)))         ;;; IllegalArgumentException
;  (is (thrown? ArgumentException (quot 3 2 1)))     ;;; IllegalArgumentException

  ; divide by zero
  (is (thrown? ArithmeticException (quot 9 0)))
  (is (thrown? ArithmeticException (quot 0 0)))
  
  (are [x y] (= x y)
    (quot 4 2) 2
    (quot 3 2) 1
    (quot 6 4) 1
    (quot 0 5) 0

    (quot 2 1/2) 4
    (quot 2/3 1/2) 1
    (quot 1 2/3) 1

    (quot 4.0 2.0) 2.0
    (quot 4.5 2.0) 2.0

    ; |num| > |div|, num != k * div
    (quot 42 5) 8     ; (8 * 5) + 2 == 42
    (quot 42 -5) -8   ; (-8 * -5) + 2 == 42
    (quot -42 5) -8   ; (-8 * 5) + -2 == -42
    (quot -42 -5) 8   ; (8 * -5) + -2 == -42

    ; |num| > |div|, num = k * div
    (quot 9 3) 3
    (quot 9 -3) -3
    (quot -9 3) -3
    (quot -9 -3) 3

    ; |num| < |div|
    (quot 2 5) 0
    (quot 2 -5) 0
    (quot -2 5) 0
    (quot -2 -5) 0

    ; num = 0, div != 0
    (quot 0 3) 0
    (quot 0 -3) 0
  )
)


;; *** Predicates ***

;; pos? zero? neg?

(deftest test-pos?-zero?-neg?
  (let [nums [[(sbyte 2) (sbyte 0) (sbyte -1)]   ;;;[(byte 2) (byte 0) (byte -1)]      ;;; right now we map byte to byte, which is unsigned versus signed in java.  (byte -2)
              [(short 3) (short 0) (short -3)]
              [(int 4) (int 0) (int -4)]
              [(long 5) (long 0) (long -5)]
              [(bigint 6) (bigint 0) (bigint -6)]
              [(float 7) (float 0) (float -7)]
              [(double 8) (double 0) (double -8)]
              [(bigdec 9) (bigdec 0) (bigdec -9)]
              [2/3 0 -2/3]]
        pred-result [[pos?  [true false false]]
                     [zero? [false true false]]
                     [neg?  [false false true]]] ]
    (doseq [pr pred-result]
      (doseq [n nums]
        (is (= (map (first pr) n) (second pr))
          (pr-str (first pr) n))))))


;; even? odd?

(deftest test-even?
  (are [x] (true? x)
    (even? -4)
    (not (even? -3))
    (even? 0)
    (not (even? 5))
    (even? 8))
  (is (thrown? ArgumentException (even? 1/2)))                     ;;; IllegalArgumentException
  (is (thrown? ArgumentException (even? (double 10)))))            ;;; IllegalArgumentException

(deftest test-odd?
  (are [x] (true? x)
    (not (odd? -4))
    (odd? -3)
    (not (odd? 0))
    (odd? 5)
    (not (odd? 8)))
  (is (thrown? ArgumentException (odd? 1/2)))                     ;;; IllegalArgumentException
  (is (thrown? ArgumentException (odd? (double 10)))))            ;;; IllegalArgumentException

(defn- expt
  "clojure.contrib.math/expt is a better and much faster impl, but this works.
Math/pow overflows to Infinity."
  [x n] (apply *' (replicate n x)))

(deftest test-bit-shift-left
  (are [x y] (= x y)
       2r10 (bit-shift-left 2r1 1)
       2r100 (bit-shift-left 2r1 2)
       2r1000 (bit-shift-left 2r1 3)
       2r00101110 (bit-shift-left 2r00010111 1)
       2r00101110 (apply bit-shift-left [2r00010111 1])
       0 (bit-shift-left 2r10 -1) ; truncated to least 6-bits, 63
       (expt 2 32) (bit-shift-left 1 32)
       (expt 2 16) (bit-shift-left 1 10000) ; truncated to least 6-bits, 16
       )
  (is (thrown? ArgumentException (bit-shift-left 1N 1))))                        ;;; IllegalArgumentException

(deftest test-bit-shift-right
  (are [x y] (= x y)
       2r0 (bit-shift-right 2r1 1)
       2r010 (bit-shift-right 2r100 1)
       2r001 (bit-shift-right 2r100 2)
       2r000 (bit-shift-right 2r100 3)
       2r0001011 (bit-shift-right 2r00010111 1)
       2r0001011 (apply bit-shift-right [2r00010111 1])
       0 (bit-shift-right 2r10 -1) ; truncated to least 6-bits, 63
       1 (bit-shift-right (expt 2 32) 32)
       1 (bit-shift-right (expt 2 16) 10000) ; truncated to least 6-bits, 16
       -1 (bit-shift-right -2r10 1)
       )
  (is (thrown? ArgumentException (bit-shift-right 1N 1))))                        ;;; IllegalArgumentException
       
(deftest test-unsigned-bit-shift-right
  (are [x y] (= x y)
       2r0 (unsigned-bit-shift-right 2r1 1)
       2r010 (unsigned-bit-shift-right 2r100 1)
       2r001 (unsigned-bit-shift-right 2r100 2)
       2r000 (unsigned-bit-shift-right 2r100 3)
       2r0001011 (unsigned-bit-shift-right 2r00010111 1)
       2r0001011 (apply unsigned-bit-shift-right [2r00010111 1])
       0 (unsigned-bit-shift-right 2r10 -1) ; truncated to least 6-bits, 63
       1 (unsigned-bit-shift-right (expt 2 32) 32)
       1 (unsigned-bit-shift-right (expt 2 16) 10000) ; truncated to least 6-bits, 16
       9223372036854775807 (unsigned-bit-shift-right -2r10 1)
       )
  (is (thrown? ArgumentException (unsigned-bit-shift-right 1N 1))))               ;;; IllegalArgumentException

 (deftest test-bit-clear
  (is (= 2r1101 (bit-clear 2r1111 1)))
  (is (= 2r1101 (bit-clear 2r1101 1))))

(deftest test-bit-set
  (is (= 2r1111 (bit-set 2r1111 1)))
  (is (= 2r1111 (bit-set 2r1101 1))))

(deftest test-bit-flip
  (is (= 2r1101 (bit-flip 2r1111 1)))
  (is (= 2r1111 (bit-flip 2r1101 1))))

(deftest test-bit-test
  (is (true? (bit-test 2r1111 1)))
  (is (false? (bit-test 2r1101 1))))

;; arrays
(deftest test-array-types
  (are [x y z] (= (clojure.lang.RT/classForName x) (class y) (class z))       ;;; Class/forName
       "System.Boolean[]" (boolean-array 1) (booleans (boolean-array 1 true))        ;;; "[Z"
       "System.Byte[]"    (byte-array 1) (bytes (byte-array 1 (byte 1)))             ;;; "[B" 
       "System.Char[]"    (char-array 1) (chars (char-array 1 \a))                   ;;; "[C"
       "System.Int16[]"   (short-array 1) (shorts (short-array 1 (short 1)))         ;;; "[S"
       "System.Single[]"  (float-array 1) (floats (float-array 1 1))                 ;;; "[F"
       "System.Double[]"  (double-array 1) (doubles (double-array 1 1))              ;;; "[D"
       "System.Int32[]"   (int-array 1) (ints (int-array 1 1))                       ;;; "[I"
       "System.Int64[]"   (long-array 1) (longs (long-array 1 1))))                  ;;; "[J"


(deftest test-ratios
  (is (== (denominator 1/2) 2))
  (is (== (numerator 1/2) 1))
  (is (= (bigint (/ 100000000000000000000 3)) 33333333333333333333))
  (is (= (long 10000000000000000000/3) 3333333333333333333))

  ;; special cases around Long/MIN_VALUE
  (is (= (/ 1 Int64/MinValue) -1/9223372036854775808))                            ;;; Long/MIN_VALUE
  (is (true? (< (/ 1 Int64/MinValue) 0)))                                         ;;; Long/MIN_VALUE
  (is (true? (< (* 1 (/ 1 Int64/MinValue)) 0)))                                   ;;; Long/MIN_VALUE
  (is (= (abs (/ 1 Int64/MinValue)) 1/9223372036854775808))                       ;;; Long/MIN_VALUE
  (is (false? (< (abs (/ 1 Int64/MinValue)) 0)))                                  ;;; Long/MIN_VALUE
  (is (false? (< (* 1 (abs (/ 1 Int64/MinValue))) 0)))                            ;;; Long/MIN_VALUE
  (is (= (/ Int64/MinValue -3) 9223372036854775808/3))                            ;;; Long/MIN_VALUE
  (is (false? (< (/ Int64/MinValue -3) 0))))                                      ;;; Long/MIN_VALUE

(deftest test-arbitrary-precision-subtract
  (are [x y] (= x y)
       9223372036854775808N (-' 0 -9223372036854775808)
       clojure.lang.BigInt  (class (-' 0 -9223372036854775808))
       Int64      (class (-' 0 -9223372036854775807))))              ;;; java.lang.Long 

(deftest test-min-max
  (testing "min/max on different numbers of floats and doubles"
    (are [xmin xmax a]
         (and (= (float xmin) (min (float a)))                          ;;; Float.
              (= (float xmax) (max (float a)))                          ;;; Float.
              (= xmin (min a))
              (= xmax (max a)))
         0.0 0.0 0.0)
    (are [xmin xmax a b]
         (and (= (float xmin) (min (float a) (float b)))            ;;; Float.
              (= (float xmax) (max (float a) (float b)))            ;;; Float.
              (= xmin (min a b))
              (= xmax (max a b)))
         -1.0  0.0  0.0 -1.0
         -1.0  0.0 -1.0  0.0
         0.0  1.0  0.0  1.0
         0.0  1.0  1.0  0.0)
    (are [xmin xmax a b c]
         (and (= (float xmin) (min (float a) (float b) (float c)))            ;;; Float.
              (= (float xmax) (max (float a) (float b) (float c)))            ;;; Float.
              (= xmin (min a b c))
              (= xmax (max a b c)))
         -1.0  1.0  0.0  1.0 -1.0
         -1.0  1.0  0.0 -1.0  1.0
         -1.0  1.0 -1.0  1.0  0.0))
  (testing "min/max preserves type of winner"
    (is (= Int64 (class (max 10))))                                 ;;; java.lang.Long
    (is (= Int64 (class (max 1.0 10))))                             ;;; java.lang.Long
    (is (= Int64 (class (max 10 1.0))))                             ;;; java.lang.Long
    (is (= Int64 (class (max 10 1.0 2.0))))                         ;;; java.lang.Long
    (is (= Int64 (class (max 1.0 10 2.0))))                         ;;; java.lang.Long
    (is (= Int64 (class (max 1.0 2.0 10))))                         ;;; java.lang.Long
    (is (= Double (class (max 1 2 10.0 3 4 5))))                    ;;; java.lang.Double
    (is (= Int64 (class (min 10))))                                 ;;; java.lang.Long
    (is (= Int64 (class (min 1.0 -10))))                            ;;; java.lang.Long
    (is (= Int64 (class (min -10 1.0))))                            ;;; java.lang.Long
    (is (= Int64 (class (min -10 1.0 2.0))))                        ;;; java.lang.Long
    (is (= Int64 (class (min 1.0 -10 2.0))))                        ;;; java.lang.Long
    (is (= Int64 (class (min 1.0 2.0 -10))))                        ;;; java.lang.Long
    (is (= Double (class (min 1 2 -10.0 3 4 5))))))                 ;;; java.lang.Double

(deftest test-abs
  (are [in ex] (= ex (abs in))
    -1 1
    1 1
    ;;;  Int64/MinValue Int64/MinValue   ;; special case!    Long/MIN_VALUE Long/MIN_VALUE  -- in CLR, taking abs of Int64/MaxValue throws
    -1.0 1.0
    -0.0 0.0
    ##-Inf ##Inf
    ##Inf ##Inf
    -123.456M 123.456M
    -123N 123N
    -1/5 1/5)
  (is (NaN? (abs ##NaN))))

(deftest clj-868
  (testing "min/max: NaN is contagious"
    (letfn [(fnan? [x] (Single/IsNaN x))                           ;;; ^Float  Float/isNaN                   
            (dnan? [^double x] (Double/IsNaN x))]                          ;;; Double/isNaN
      (are [minmax]
           (are [nan? nan zero]
                (every? nan? (map minmax
                                  [ nan zero zero]
                                  [zero  nan zero]
                                  [zero zero  nan]))
                fnan?  Single/NaN  (float 0.0)                              ;;;  Float/NaN (Float.   
                dnan? Double/NaN          0.0)
           min
         max))))

(defn integer
  "Distribution of integers biased towards the small, but
   including all longs."
  []
  (gen/one-of #(gen/uniform -1 32) gen/byte gen/short gen/int gen/long))

(defn longable?
  [n]
  (try
   (long n)
   true
   (catch Exception _)))

(defspec integer-commutative-laws
  (partial map identity)
  [^{:tag `integer} a ^{:tag `integer} b]
  (if (longable? (+' a b))
    (assert (= (+ a b) (+ b a)
               (+' a b) (+' b a)
               (unchecked-add a b) (unchecked-add b a)))
    (assert (= (+' a b) (+' b a))))
  (if (longable? (*' a b))
    (assert (= (* a b) (* b a)
               (*' a b) (*' b a)
               (unchecked-multiply a b) (unchecked-multiply b a)))
    (assert (= (*' a b) (*' b a)))))

(defspec integer-associative-laws
  (partial map identity)
  [^{:tag `integer} a ^{:tag `integer} b ^{:tag `integer} c]
  (if (every? longable? [(+' a b) (+' b c) (+' a b c)])
    (assert (= (+ (+ a b) c) (+ a (+ b c))
               (+' (+' a b) c) (+' a (+' b c))
               (unchecked-add (unchecked-add a b) c) (unchecked-add a (unchecked-add b c))))
    (assert (= (+' (+' a b) c) (+' a (+' b c))
               (+ (+ (bigint a) b) c) (+ a (+ (bigint b) c)))))
  (if (every? longable? [(*' a b) (*' b c) (*' a b c)])
    (assert (= (* (* a b) c) (* a (* b c))
               (*' (*' a b) c) (*' a (*' b c))
               (unchecked-multiply (unchecked-multiply a b) c) (unchecked-multiply a (unchecked-multiply b c))))
    (assert (= (*' (*' a b) c) (*' a (*' b c))
               (* (* (bigint a) b) c) (* a (* (bigint b) c))))))

(defspec integer-distributive-laws
  (partial map identity)
  [^{:tag `integer} a ^{:tag `integer} b ^{:tag `integer} c]
  (if (every? longable? [(*' a (+' b c)) (+' (*' a b) (*' a c))
                         (*' a b) (*' a c) (+' b c)])
    (assert (= (* a (+ b c)) (+ (* a b) (* a c))
               (*' a (+' b c)) (+' (*' a b) (*' a c))
               (unchecked-multiply a (+' b c)) (+' (unchecked-multiply a b) (unchecked-multiply a c))))
    (assert (= (*' a (+' b c)) (+' (*' a b) (*' a c))
               (* a (+ (bigint b) c)) (+ (* (bigint a) b) (* (bigint a) c))))))

(defspec addition-undoes-subtraction
  (partial map identity)
  [^{:tag `integer} a ^{:tag `integer} b]
  (if (longable? (-' a b))
    (assert (= a
               (-> a (- b) (+ b))
               (-> a (unchecked-subtract b) (unchecked-add b)))))
  (assert (= a
             (-> a (-' b) (+' b)))))

(defspec quotient-and-remainder
  (fn [a b] (sort [a b]))
  [^{:tag `integer} a ^{:tag `integer} b]
  (when-not (zero? (second %))
    (let [[a d] %
          q (quot a d)
          r (rem a d)]
      (assert (= a
                 (+ (* q d) r)
                 (unchecked-add (unchecked-multiply q d) r))))))

(deftest unchecked-inc-overflow
  (testing "max value overflows to min value"
    (is (= Int64/MinValue (unchecked-inc Int64/MaxValue)))                                      ;;; Long/MIN_VALUE  Long/MAX_VALUE
    (is (= Int64/MinValue (unchecked-inc ^Object Int64/MaxValue)))))                            ;;; Long/MIN_VALUE  (Long/valueOf Long/MAX_VALUE)

(deftest unchecked-dec-overflow
  (testing "min value overflows to max value"
    (is (= Int64/MaxValue (unchecked-dec Int64/MinValue)))                                      ;;;  Long/MAX_VALUE Long/MIN_VALUE
    (is (= Int64/MaxValue (unchecked-dec ^Object Int64/MinValue)))))                            ;;;  Long/MAX_VALUE (Long/valueOf Long/MIN_VALUE)

(deftest unchecked-negate-overflow
  (testing "negating min value overflows to min value itself"
    (is (= Int64/MinValue (unchecked-negate Int64/MinValue)))                                   ;;; Long/MIN_VALUE Long/MIN_VALUE
    (is (= Int64/MinValue (unchecked-negate ^Object Int64/MinValue)))))                         ;;; Long/MIN_VALUE (Long/valueOf Long/MIN_VALUE)

(deftest unchecked-add-overflow
  (testing "max value overflows to min value"
    (is (= Int64/MinValue (unchecked-add Int64/MaxValue 1)))				                    ;;; Long/MIN_VALUE  Long/MAX_VALUE
    (is (= Int64/MinValue (unchecked-add Int64/MaxValue ((fn [] 1)))))                            ;;; Long/MIN_VALUE  (Long/valueOf Long/MAX_VALUE)
    (is (= Int64/MinValue (unchecked-add ^Object Int64/MaxValue 1)))                            ;;; Long/MIN_VALUE  (Long/valueOf 1)
    (is (= Int64/MinValue (unchecked-add ^Object Int64/MaxValue ((fn [] 1))))))                   ;;; Long/MIN_VALUE  (Long/valueOf Long/MAX_VALUE)   (Long/valueOf 1)
  (testing "adding min value to min value results in zero"
    (is (= 0 (unchecked-add Int64/MinValue Int64/MinValue)))                                    ;;; Long/MIN_VALUE  Long/MIN_VALUE
    (is (= 0 (unchecked-add Int64/MinValue ^Object Int64/MinValue)))                            ;;; Long/MIN_VALUE (Long/valueOf Long/MIN_VALUE)
    (is (= 0 (unchecked-add ^Object Int64/MinValue Int64/MinValue)))                            ;;; (Long/valueOf Long/MIN_VALUE) Long/MIN_VALUE
    (is (= 0 (unchecked-add ^Object Int64/MinValue ^Object Int64/MinValue)))))            ;;; (Long/valueOf Long/MIN_VALUE) (Long/valueOf Long/MIN_VALUE)

(deftest unchecked-subtract-overflow
  (testing "min value overflows to max-value"                                                   ;;; etc
    (is (= Int64/MaxValue (unchecked-subtract Int64/MinValue 1)))
    (is (= Int64/MaxValue (unchecked-subtract Int64/MinValue ((fn [] 1)))))
    (is (= Int64/MaxValue (unchecked-subtract ^Object Int64/MinValue 1)))
    (is (= Int64/MaxValue (unchecked-subtract ^Object Int64/MinValue ((fn [] 1))))))
  (testing "negating min value overflows to min value itself"
    (is (= Int64/MinValue (unchecked-subtract 0 Int64/MinValue)))
    (is (= Int64/MinValue (unchecked-subtract 0 ^Object Int64/MinValue)))
    (is (= Int64/MinValue (unchecked-subtract ((fn [] 0)) Int64/MinValue)))
    (is (= Int64/MinValue (unchecked-subtract ((fn [] 0)) ^Object Int64/MinValue)))))

(deftest unchecked-multiply-overflow
  (testing "two times max value results in -2"
    (is (= -2 (unchecked-multiply Int64/MaxValue 2)))
    (is (= -2 (unchecked-multiply Int64/MaxValue ((fn [] 2)))))
    (is (= -2 (unchecked-multiply ^Object Int64/MaxValue 2)))
    (is (= -2 (unchecked-multiply ^Object Int64/MaxValue ((fn [] 2))))))
  (testing "two times min value results in 0"
    (is (= 0 (unchecked-multiply Int64/MinValue 2)))
    (is (= 0 (unchecked-multiply Int64/MinValue ((fn [] 2)))))
    (is (= 0 (unchecked-multiply ^Object Int64/MinValue 2)))
    (is (= 0 (unchecked-multiply ^Object Int64/MinValue ((fn [] 2)))))))

(defmacro check-warn-on-box [warn? form]
  `(do (binding [*unchecked-math* :warn-on-boxed]
                (is (= ~warn?
                       (boolean
                         (re-find #"^Boxed math warning"
                                  (helper/with-err-string-writer
                                    (helper/eval-in-temp-ns ~form)))))))
       (binding [*unchecked-math* true]
                (is (false?
                      (boolean
                        (re-find #"^Boxed math warning"
                                 (helper/with-err-string-writer
                                   (helper/eval-in-temp-ns ~form)))))))
       (binding [*unchecked-math* false]
                (is (false?
                      (boolean
                        (re-find #"^Boxed math warning"
                                 (helper/with-err-string-writer
                                   (helper/eval-in-temp-ns ~form)))))))))

(deftest warn-on-boxed
  (check-warn-on-box true (#(inc %) 2))
  (check-warn-on-box false (#(inc ^long %) 2))
  (check-warn-on-box false (long-array 5))
  (check-warn-on-box true (> (first (range 3)) 0))
  (check-warn-on-box false (> ^long (first (range 3)) 0)))


(deftest comparisons
  (let [small-numbers [1 1.0 (int 1) (float 1) 9/10 1N 1M]              ;;; (Integer. 1) (Float. 1.0)
        big-numbers [10 10.0 (int 10) (float 10.0) 99/10 10N 10N]]      ;;; (Integer. 10) (Float. 10.0)
    (doseq [small small-numbers big big-numbers]
      (is (< small big))
      (is (not (< big small)))
      (is (not (< small small)))
      (is (< (int small) (int big)))
      (is (not (< (int big) (int small))))
      (is (not (< (int small) (int small))))
      (is (< (double small) (double big)))
      (is (not (< (double big) (double small))))
      (is (not (< (double small) (double small))))
      (is (<= small big))
      (is (<= small small))
      (is (not (<= big small)))
      (is (<= (int small) (int big)))
      (is (<= (int small) (int small)))
      (is (not (<= (int big) (int small))))
      (is (<= (double small) (double big)))
      (is (<= (double small) (double small)))
      (is (not (<= (double big) (double small))))
      (is (> big small))
      (is (not (> small big)))
      (is (not (> small small)))
      (is (> (int big) (int small)))
      (is (not (> (int small) (int big))))
      (is (not (> (int small) (int small))))
      (is (> (double big) (double small)))
      (is (not (> (double small) (double big))))
      (is (not (> (double small) (double small))))
      (is (>= big small))
      (is (>= small small))
      (is (not (>= small big)))
      (is (>= (int big) (int small)))
      (is (>= (int small) (int small)))
      (is (not (>= (int small) (int big))))
      (is (>= (double big) (double small)))
      (is (>= (double small) (double small)))
      (is (not (>= (double small) (double big)))))))

(deftest test-nan-comparison
  (are [x y] (= x y)
       (< 1000 Double/NaN) (< 1000 (double Double/NaN))            ;;; (Double. Double/NaN)  -- no boxed, so kinda pointless
       (<= 1000 Double/NaN) (<= 1000 (double Double/NaN))          ;;; (Double. Double/NaN)
       (> 1000 Double/NaN) (> 1000 (double Double/NaN))            ;;; (Double. Double/NaN)
       (>= 1000 Double/NaN) (>= 1000 (double Double/NaN))))        ;;; (Double. Double/NaN)

(deftest test-nan-as-operand
  (testing "All numeric operations with NaN as an operand produce NaN as a result"
    (let [nan Double/NaN
          onan (cast Object Double/NaN)]
      (are [x] (Double/IsNaN x)                                    ;;; Double/isNaN
          (+ nan 1)
          (+ nan 0)
          (+ nan 0.0)
          (+ 1 nan)
          (+ 0 nan)
          (+ 0.0 nan)
          (+ nan nan)
          (- nan 1)
          (- nan 0)
          (- nan 0.0)
          (- 1 nan)
          (- 0 nan)
          (- 0.0 nan)
          (- nan nan)
          (* nan 1)
          (* nan 0)
          (* nan 0.0)
          (* 1 nan)
          (* 0 nan)
          (* 0.0 nan)
          (* nan nan)
          (/ nan 1)
          (/ nan 0)
          (/ nan 0.0)
          (/ 1 nan)
          (/ 0 nan)
          (/ 0.0 nan)
          (/ nan nan)
          (+ onan 1)
          (+ onan 0)
          (+ onan 0.0)
          (+ 1 onan)
          (+ 0 onan)
          (+ 0.0 onan)
          (+ onan onan)
          (- onan 1)
          (- onan 0)
          (- onan 0.0)
          (- 1 onan)
          (- 0 onan)
          (- 0.0 onan)
          (- onan onan)
          (* onan 1)
          (* onan 0)
          (* onan 0.0)
          (* 1 onan)
          (* 0 onan)
          (* 0.0 onan)
          (* onan onan)
          (/ onan 1)
          (/ onan 0)
          (/ onan 0.0)
          (/ 1 onan)
          (/ 0 onan)
          (/ 0.0 onan)
          (/ onan onan)
          (+ nan onan)
          (+ onan nan)
          (- nan onan)
          (- onan nan)
          (* nan onan)
          (* onan nan)
          (/ nan onan)
          (/ onan nan) ))))