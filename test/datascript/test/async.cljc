(ns datascript.test.async
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go go-loop]]))
  (:require
   [datascript :as d]
   [datascript.core :as dc]
   [datascript.test.core :as tdc]
   #?(:cljs [cljs.core.async :refer [<! put! chan]]
      :clj  [clojure.core.async :refer [<! put! chan go go-loop]])))

(defn btset-by [cmp & keys]
  (apply sorted-set-by cmp keys))

(defn- distinct-consecutive [sequence]
  (map first (partition-by identity sequence)))

(defn slice
  "When called with single key, returns iterator over set that contains all elements equal to the key.
   When called with two keys (range), returns iterator for all X where key-from <= X <= key-to"
  ([ss a] (slice ss a a))
  ([ss a b]
  (go
   ;; do subseq from !a -> a, and a -> b -> !b; concat and dedup
   ;; do overlaps (always <= or >=) since the glob matching needs it.
   (-> (rsubseq ss >= a >= a)
       (reverse)
       (concat (subseq ss >= a <= b))
       (distinct-consecutive)))))

(def eavt (btset-by dc/cmp-datoms-eavt))
(def aevt (btset-by dc/cmp-datoms-aevt))
(def avet (btset-by dc/cmp-datoms-avet))

(defn empty-db
  ([]
    (set! eavt (btset-by dc/cmp-datoms-eavt))
    (set! aevt (btset-by dc/cmp-datoms-aevt))
    (set! avet (btset-by dc/cmp-datoms-avet))
    (dc/map->DB {
      :schema  nil
      :eavt    #(slice eavt %1 %2)
      :aevt    #(slice aevt %1 %2)
      :avet    #(slice avet %1 %2)
      :max-eid 0
      :max-tx  0
      :rschema {}})))

(defn with-datom [db & datom]
  (set! eavt (conj eavt (apply dc/datom datom)))
  (set! aevt (conj aevt (apply dc/datom datom)))
  (set! avet (conj avet (apply dc/datom datom)))
  db)

(defn output-test [under-test correct-answer]
  (if (= under-test correct-answer)
    (println "PASSED!")
    (println "FAILED!" under-test correct-answer)))

(defn test-joins []
  (go
    (let [db (-> (empty-db)
                 (with-datom 1 :name "Ivan")
                 (with-datom 1 :age 15)
                 (with-datom 2 :name "Petr")
                 (with-datom 2 :age 37)
                 (with-datom 3 :name "Ivan")
                 (with-datom 3 :age 37)
                 (with-datom 4 :age 15))]
      (output-test (<! (d/q '[:find ?e
                    :where [?e :name]] db))
             #{[1] [2] [3]})
      (output-test (<! (d/q '[:find  ?e ?v
                    :where [?e :name "Ivan"]
                           [?e :age ?v]] db))
             #{[1 15] [3 37]})
      (output-test (<! (d/q '[:find  ?e1 ?e2
                    :where [?e1 :name ?n]
                           [?e2 :name ?n]] db))
             #{[1 1] [2 2] [3 3] [1 3] [3 1]})
      (output-test (<! (d/q '[:find  ?e ?e2 ?n
                    :where [?e :name "Ivan"]
                           [?e :age ?a]
                           [?e2 :age ?a]
                           [?e2 :name ?n]] db))
             #{[1 1 "Ivan"]
               [3 3 "Ivan"]
               [3 2 "Petr"]}))))

(defn test-q-many []
  (go
  (let [db (-> (empty-db)
               (with-datom 1 :name "Ivan")
               (with-datom 1 :aka  "ivolga")
               (with-datom 1 :aka  "pi")
               (with-datom 2 :name "Petr")
               (with-datom 2 :aka  "porosenok")
               (with-datom 2 :aka  "pi"))]
    (output-test (<! (d/q '[:find  ?n1 ?n2
                  :where [?e1 :aka ?x]
                         [?e2 :aka ?x]
                         [?e1 :name ?n1]
                         [?e2 :name ?n2]] db))
           #{["Ivan" "Ivan"]
             ["Petr" "Petr"]
             ["Ivan" "Petr"]
             ["Petr" "Ivan"]}))))

(defn test-q-coll []
  (go
  (let [db [ [1 :name "Ivan"]
             [1 :age  19]
             [1 :aka  "dragon_killer_94"]
             [1 :aka  "-=autobot=-"] ] ]
    (output-test (<! (d/q '[ :find  ?n ?a
                   :where [?e :aka "dragon_killer_94"]
                          [?e :name ?n]
                          [?e :age  ?a]] db))
           #{["Ivan" 19]}))

  ; (testing "Query over long tuples"
    (let [db [ [1 :name "Ivan" 945 :db/add]
               [1 :age  39     999 :db/retract]] ]
      (output-test (<! (d/q '[ :find  ?e ?v
                     :where [?e :name ?v]] db))
             #{[1 "Ivan"]})
      (output-test (<! (d/q '[ :find  ?e ?a ?v ?t
                     :where [?e ?a ?v ?t :db/retract]] db))
             #{[1 :age 39 999]}))))

(defn test-q-in []
  (go
  (let [db (-> (empty-db)
               (with-datom 1 :name  "Ivan")
               (with-datom 1 :age   15 )
               (with-datom 2 :name  "Petr")
               (with-datom 2 :age   37 )
               (with-datom 3 :name  "Ivan")
               (with-datom 3 :age   37 ))
        query '{:find  [?e]
                :in    [$ ?attr ?value]
                :where [[?e ?attr ?value]]}]
    (output-test (<! (d/q query db :name "Ivan"))
           #{[1] [3]})
    (output-test (<! (d/q query db :age 37))
           #{[2] [3]})

    ; (testing "Named DB"
      (output-test (<! (d/q '[:find  ?a ?v
                    :in    $db ?e
                    :where [$db ?e ?a ?v]] db 1))
             #{[:name "Ivan"]
               [:age 15]})

    ; (testing "DB join with collection"
      (output-test (<! (d/q '[:find  ?e ?email
                    :in    $ $b
                    :where [?e :name ?n]
                           [$b ?n ?email]]
                  db
                  [["Ivan" "ivan@mail.ru"]
                   ["Petr" "petr@gmail.com"]]))
             #{[1 "ivan@mail.ru"]
               [2 "petr@gmail.com"]
               [3 "ivan@mail.ru"]})

    ; (testing "Query without DB"
      (output-test (<! (d/q '[:find ?a ?b
                    :in   ?a ?b]
                  10 20))
             #{[10 20]}))))

(defn test-bindings []
  (go
  (let [db (-> (empty-db)
               (with-datom 1 :name  "Ivan")
               (with-datom 1 :age   15 )
               (with-datom 2 :name  "Petr")
               (with-datom 2 :age   37 )
               (with-datom 3 :name  "Ivan")
               (with-datom 3 :age   37 ))]
    ; (testing "Relation binding"
      (output-test (<! (d/q '[:find  ?e ?email
                    :in    $ [[?n ?email]]
                    :where [?e :name ?n]]
                  db
                  [["Ivan" "ivan@mail.ru"]
                   ["Petr" "petr@gmail.com"]]))
             #{[1 "ivan@mail.ru"]
               [2 "petr@gmail.com"]
               [3 "ivan@mail.ru"]})

    ; (testing "Tuple binding"
      (output-test (<! (d/q '[:find  ?e
                    :in    $ [?name ?age]
                    :where [?e :name ?name]
                           [?e :age ?age]]
                  db ["Ivan" 37]))
             #{[3]})

    ; (testing "Collection binding"
      (output-test (<! (d/q '[:find  ?attr ?value
                    :in    $ ?e [?attr ...]
                    :where [?e ?attr ?value]]
                  db 1 [:name :age]))
             #{[:name "Ivan"] [:age 15]})

    ; (testing "Empty coll handling"
      (output-test (<! (d/q '[:find ?id
                    :in $ [?id ...]
                    :where [?id :age _]]
               [[1 :name "Ivan"]
                [2 :name "Petr"]]))
             #{})
      (output-test (<! (d/q '[:find ?id
                    :in $ [[?id]]
                    :where [?id :age _]]
               [[1 :name "Ivan"]
                [2 :name "Petr"]]))
             #{})

    ; (testing "Placeholders"
      (output-test (<! (d/q '[:find ?x ?z
                    :in [?x _ ?z]]
                  [:x :y :z]))
             #{[:x :z]})
      (output-test (<! (d/q '[:find ?x ?z
                    :in [[?x _ ?z]]]
                  [[:x :y :z] [:a :b :c]]))
             #{[:x :z] [:a :c]})

    ; (testing "Error reporting"
    ;   (is (thrown-with-msg? ExceptionInfo #"Cannot bind value :a to tuple \[\?a \?b\]"
    ;         (d/q '[:find ?a ?b :in [?a ?b]] :a)))
    ;   (is (thrown-with-msg? ExceptionInfo #"Cannot bind value :a to collection \[\?a \.\.\.\]"
    ;         (d/q '[:find ?a :in [?a ...]] :a)))
    ;   (is (thrown-with-msg? ExceptionInfo #"Not enough elements in a collection \[:a\] to bind tuple \[\?a \?b\]"
    ;         (d/q '[:find ?a ?b :in [?a ?b]] [:a]))))

)))

(defn test-nested-bindings []
  (go
  (output-test (<! (d/q '[:find  ?k ?v
                :in    [[?k ?v] ...]
                :where [(> ?v 1)]]
              {:a 1, :b 2, :c 3}))
         #{[:b 2] [:c 3]})

  (output-test (<! (d/q '[:find  ?k ?min ?max
              :in    [[?k ?v] ...] ?minmax
              :where [(?minmax ?v) [?min ?max]]
                     [(> ?max ?min)]]
            {:a [1 2 3 4]
             :b [5 6 7]
             :c [3]}
            #(vector (reduce min %) (reduce max %))))
       #{[:a 1 4] [:b 5 7]})

  (output-test (<! (d/q '[:find  ?k ?x
                :in    [[?k [?min ?max]] ...] ?range
                :where [(?range ?min ?max) [?x ...]]
                       [(even? ?x)]]
              {:a [1 7]
               :b [2 4]}
              range))
         #{[:a 2] [:a 4] [:a 6]
           [:b 2]})))

;TODO: make function to run all the tests
(go
  (<! (test-joins))
  (<! (test-q-many))
  (<! (test-q-coll))
  (<! (test-q-in))
  (<! (test-bindings))
  (<! (test-nested-bindings)))
