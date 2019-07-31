(ns cdl.test.lang
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]

            [cdl.lang :as l]))

(deftest constant-str-test
  (let [f (l/->ConstantVal "test-val")]
    (is (= "test-val" (l/from-json f {:a 1 :b 2})))
    (is (s/valid? (eval (l/get-spec-form f)) "test-val"))
    (is (not (s/valid? (eval (l/get-spec-form f)) "asd")))
    (is (= {:type "keyword"} (l/get-es-mapping f)))))

(deftest boolean-test
  (let [f (l/->Bool "$.val")]
    (is (nil? (l/from-json f {:not-val "true"})))
    (is (l/from-json f {:val ""}))
    (is (not (l/from-json f {:val 1})))
    (is (not (l/from-json f {:val "false"})))
    (is (s/valid? (eval (l/get-spec-form f)) true))
    (is (s/valid? (eval (l/get-spec-form f)) false))
    (is (not (s/valid? (eval (l/get-spec-form f)) "asd")))
    (is (not (s/valid? (eval (l/get-spec-form f)) "")))
    (is (not (s/valid? (eval (l/get-spec-form f)) nil)))
    (is (not (s/valid? (eval (l/get-spec-form f)) '())))
    (is (= {:type "boolean"} (l/get-es-mapping f)))))

(deftest str-test
  (let [f (l/->Str "$.val")]
    (is (= "test" (l/from-json f {:val "test" :not-val "not-test"})))
    (is (nil? (l/from-json f {:val 1})))
    (is (nil? (l/from-json f {:val nil})))
    (is (nil? (l/from-json f {:val ""})))
    (is (nil? (l/from-json f {:not-val "not-test"})))
    (is (s/valid? (eval (l/get-spec-form f)) "sasd"))
    (is (not (s/valid? (eval (l/get-spec-form f)) :asdasd)))
    (is (not (s/valid? (eval (l/get-spec-form f)) "")))
    (is (not (s/valid? (eval (l/get-spec-form f)) nil)))
    (is (not (s/valid? (eval (l/get-spec-form f)) [])))
    (is (not (s/valid? (eval (l/get-spec-form f)) true)))
    (is (= {:type "keyword"} (l/get-es-mapping f)))))

(deftest text-test
  (let [f (l/->Text "$.val")]
    (is (= "test" (l/from-json f {:val "test" :not-val "not-test"})))
    (is (nil? (l/from-json f {:val 1 :not-val "not-test"})))
    (is (nil? (l/from-json f {:val nil})))
    (is (nil? (l/from-json f {:val ""})))
    (is (nil? (l/from-json f {:not-val "not-test"})))
    (is (s/valid? (eval (l/get-spec-form f)) "sasd"))
    (is (not (s/valid? (eval (l/get-spec-form f)) :asdasd)))
    (is (not (s/valid? (eval (l/get-spec-form f)) "")))
    (is (not (s/valid? (eval (l/get-spec-form f)) nil)))
    (is (not (s/valid? (eval (l/get-spec-form f)) [])))
    (is (= {:type "text"} (l/get-es-mapping f)))))

(deftest enum-test
  (let [f (l/->Enumeration "$.val" {"1" "Y" "2" "N"})]
    (is (nil? (l/from-json f {:not-val "not-test"})))
    (is (nil? (l/from-json f {:val "test" :not-val "not-test"})))
    (is (nil? (l/from-json f {:val 10 :not-val "not-test"})))
    (is (nil? (l/from-json f {:val 1 :not-val "not-test"})))
    (is (= "Y" (l/from-json f {:val "1" :not-val "not-test"})))
    (is (= "N" (l/from-json f {:val "2" :not-val "not-test"})))
    (is (s/valid? (eval (l/get-spec-form f)) "Y"))
    (is (s/valid? (eval (l/get-spec-form f)) "N"))
    (is (not (s/valid? (eval (l/get-spec-form f)) :asdasd)))
    (is (not (s/valid? (eval (l/get-spec-form f)) "")))
    (is (not (s/valid? (eval (l/get-spec-form f)) nil)))
    (is (not (s/valid? (eval (l/get-spec-form f)) [])))
    (is (= {:type "text"} (l/get-es-mapping f)))))

(deftest date-test
  (let [f (l/->Date "$.val")]
    (is (= "2019-08-12" (l/from-json f {:val "2019-08-12" :not-val "not-test"})))
    (is (nil? (l/from-json f {:not-val "not-test"})))
    (is (nil? (l/from-json f {:val 1})))
    (is (nil? (l/from-json f {:val ""})))
    (is (s/valid? (eval (l/get-spec-form f)) "2019-08-12"))
    (is (s/valid? (eval (l/get-spec-form f)) "2019-08-12 16:43"))
    (is (s/valid? (eval (l/get-spec-form f)) "2043"))
    (is (not (s/valid? (eval (l/get-spec-form f)) "")))
    (is (not (s/valid? (eval (l/get-spec-form f)) :asdasd)))
    (is (not (s/valid? (eval (l/get-spec-form f)) nil)))
    (is (not (s/valid? (eval (l/get-spec-form f)) [])))
    (is (= {:type "date"
            :format "yyyy-MM-dd HH:mm|yyyy-MM-dd|yyyy-MM|yyyy"} (l/get-es-mapping f)))))

(deftest date-time-test
  (let [f (l/->DateTime "$.date" "$.time")]
    (is (= "2019-08-12" (l/from-json f {:date "2019-08-12" :time ""})))
    (is (= "2019-08-12 16:43" (l/from-json f {:date "2019-08-12" :time "16:43"})))

    (is (nil? (l/from-json f {:not-val "not-test"})))
    (is (nil? (l/from-json f {:date 1 :time 123})))
    (is (s/valid? (eval (l/get-spec-form f)) "2019-08-12"))
    (is (s/valid? (eval (l/get-spec-form f)) "2019-08-12 16:43"))
    (is (s/valid? (eval (l/get-spec-form f)) "2043"))
    (is (not (s/valid? (eval (l/get-spec-form f)) "")))
    (is (not (s/valid? (eval (l/get-spec-form f)) :asdasd)))
    (is (not (s/valid? (eval (l/get-spec-form f)) nil)))
    (is (not (s/valid? (eval (l/get-spec-form f)) [])))
    (is (= {:type "date"
            :format "yyyy-MM-dd HH:mm|yyyy-MM-dd|yyyy-MM|yyyy"} (l/get-es-mapping f)))))

(deftest array-test
  (let [f (l/->Array "$.arr" (l/->Enumeration "$.val" {"1" "Y" "2" "N"}) false)]
    (is (nil? (l/from-json f {:not-val "not-test"})))
    (is (nil? (l/from-json f {:arr "test" :not-val "not-test"})))
    (is (nil? (l/from-json f {:arr 123 :not-val "not-test"})))
    (is (= ["Y" "Y" "N" "Y"]
           (l/from-json f {:arr [{:val "1"} {:val "1"} {:val "2"} {:val "1"}]})))
    (is (= ["Y" "Y" "Y"]
           (l/from-json f {:arr [{:val "1"} {:val "1"} {:val "wrong"} {:val "1"}]})))
    (is (= nil
           (l/from-json f {:arr [{:val "no"} {:val "no"} {:val "wrong"}]})))

    (is (s/valid? (eval (l/get-spec-form f)) ["Y" "Y" "Y"]))
    (is (s/valid? (eval (l/get-spec-form f)) ["Y" "Y" "N" "Y"]))
    (is (not (s/valid? (eval (l/get-spec-form f)) ["Y" "Y" "N" nil])))
    (is (not (s/valid? (eval (l/get-spec-form f)) ["Y" "Y" "N" "wrong"])))
    (is (not (s/valid? (eval (l/get-spec-form f)) ["Y" "Y" "N" ""])))
    (is (not (s/valid? (eval (l/get-spec-form f)) [])))
    (is (not (s/valid? (eval (l/get-spec-form f)) nil)))
    (is (= {:type "text"} (l/get-es-mapping f)))))

(deftest composite-test
  (let [f (l/map->Composite
            {:test-arr (l/->Array "$.arr" (l/->Enumeration "$.val" {"1" "Y" "2" "N"}) false)
             :test-str (l/->Str "$.str-val")
             :test-bool (l/->Bool "$.bool-val")
             :const (l/->ConstantVal "exp-type")})
        f2 (l/map->Composite
             {:test-arr (l/->Array "$.arr" (l/->Enumeration "$.val" {"1" "Y" "2" "N"}) false)
              :test-str (l/->Str "$.str-val")
              :test-bool (l/->Bool "$.bool-val")})]
    (is (= {:test-arr ["Y" "N" "Y"]
            :test-str "test-str"
            :const "exp-type"}
           (l/from-json f {:arr [{:val "1"} {:val "2"} {:val "1"} {:val "wrong"}]
                           :str-val "test-str"})))
    (is (= {:test-arr ["Y" "N" "Y"]
            :test-bool false
            :const "exp-type"}
           (l/from-json f {:arr [{:val "1"} {:val "2"} {:val "1"} {:val "wrong"}]
                           :str-val ""
                           :bool-val "false"})))
    (is (= {:test-bool true
            :const "exp-type"}
           (l/from-json f {:arr 12 :str-val "" :bool-val ""})))
    (is (= nil (l/from-json f2 {:arr 12 :str-val ""})))

    (eval (l/to-spec-defs ::f-spec (l/get-spec-form f)))

    (is (not (s/valid? ::f-spec [])))
    (is (not (s/valid? ::f-spec nil)))
    (is (not (s/valid? ::f-spec "")))
    (is (s/valid? ::f-spec {}))
    (is (s/valid? ::f-spec {:unknown "asd"
                            :test-arr ["Y" "Y" "N"]
                            :test-str "asd"
                            :test-bool true}))
    (is (s/valid? ::f-spec {:unknown "asd"
                            :test-arr ["Y" "Y" "N"]
                            :test-str "asd"}))
    (is (not (s/valid? ::f-spec {:test-arr ["Y" "Y" "N" "weird"]
                                 :test-str "asd"})))
    (is (not (s/valid? ::f-spec {:unknown "asd"
                                 :const "wrong"
                                 :test-arr ["Y" "Y" "N"]
                                 :test-str "asd"
                                 :test-bool true})))

    (is (= {:properties {:test-arr {:type "text"}
                         :test-str {:type "keyword"}
                         :test-bool {:type "boolean"}
                         :const {:type "keyword"}}}
           (l/get-es-mapping f)))))

(deftest nested-composite-test
  (let [f (l/map->Composite
            {:test-arr (l/->Array "$.arr" (l/->Enumeration "$.val" {"1" "Y" "2" "N"}) false)
             :test-str (l/->Str "$.str-val")
             :test-bool (l/->Bool "$.bool-val")
             :const (l/->ConstantVal "exp-type")
             :composite (l/map->Composite {:nest-str (l/->Str "$.test-str")})})]
    (is (= {:test-arr ["Y" "N" "Y"]
            :test-str "test-str"
            :const "exp-type"
            :composite {:nest-str "something"}}
           (l/from-json f {:arr [{:val "1"} {:val "2"} {:val "1"} {:val "wrong"}]
                           :str-val "test-str"
                           :test-str "something"})))
    (is (= {:test-arr ["Y" "N" "Y"]
            :test-str "test-str"
            :const "exp-type"}
           (l/from-json f {:arr [{:val "1"} {:val "2"} {:val "1"} {:val "wrong"}]
                           :str-val "test-str"})))

    ;(clojure.pprint/pprint (l/to-spec-defs ::nest-f-spec (l/get-spec-form f)))
    (eval (l/to-spec-defs ::nest-f-spec (l/get-spec-form f)))

    (is (not (s/valid? ::nest-f-spec [])))
    (is (not (s/valid? ::nest-f-spec nil)))
    (is (not (s/valid? ::nest-f-spec "")))
    (is (s/valid? ::nest-f-spec {}))

    (is (s/valid? ::nest-f-spec {:unknown "asd"
                                 :test-arr ["Y" "Y" "N"]
                                 :test-str "asd"
                                 :test-bool true
                                 :composite {:nest-str "something"}}))
    (is (s/valid? ::nest-f-spec {:unknown "asd"
                                 :test-arr ["Y" "Y" "N"]
                                 :test-str "asd"
                                 :test-bool true
                                 :composite {:nest-str "something"
                                             :unknown true}}))
    (is (s/valid? ::nest-f-spec {:unknown "asd"
                                 :test-arr ["Y" "Y" "N"]
                                 :test-str "asd"
                                 :test-bool true
                                 :composite {:unknown true}}))
    (is (not (s/valid? ::nest-f-spec {:unknown "asd"
                                      :test-arr ["Y" "Y" "N"]
                                      :test-str "asd"
                                      :test-bool true
                                      :composite {:nest-str ""}})))
    (is (not (s/valid? ::nest-f-spec {:unknown "asd"
                                      :test-arr ["Y" "Y" "N"]
                                      :test-str "asd"
                                      :test-bool true
                                      :composite {:nest-str true}})))

    (is (= {:properties {:test-arr {:type "text"}
                         :test-str {:type "keyword"}
                         :test-bool {:type "boolean"}
                         :const {:type "keyword"}
                         :composite {:properties {:nest-str {:type "keyword"}}}}}
           (l/get-es-mapping f)))))
