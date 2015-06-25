(ns datascript.test.async
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go go-loop]]))
  (:require
   [datascript :as d]
   [datascript.core :as dc]
   [datascript.test.core :as tdc]
   #?(:cljs [cljs.core.async :refer [<! put! chan]]
      :clj  [clojure.core.async :refer [<! put! chan go go-loop]])))

(defn test-joins []
  (go
    (let [db [[1 :name  "Ivan"]
              [1 :age   15]
              [2 :name  "Petr"]
              [2 :age   37]
              [3 :name  "Ivan"]
              [3 :age   37]
              [4 :age   15]]]
      (println (<! (d/q '[:find ?e
                    :where [?e :name]] db))
             #{[1] [2] [3]})
      (println (<! (d/q '[:find  ?e ?v
                    :where [?e :name "Ivan"]
                           [?e :age ?v]] db))
             #{[1 15] [3 37]})
      (println (<! (d/q '[:find  ?e1 ?e2
                    :where [?e1 :name ?n]
                           [?e2 :name ?n]] db))
             #{[1 1] [2 2] [3 3] [1 3] [3 1]})
      (println (<! (d/q '[:find  ?e ?e2 ?n
                    :where [?e :name "Ivan"]
                           [?e :age ?a]
                           [?e2 :age ?a]
                           [?e2 :name ?n]] db))
             #{[1 1 "Ivan"]
               [3 3 "Ivan"]
               [3 2 "Petr"]}))))

(defn test-q-many []
  (go
  (let [db [[1 :name "Ivan"]
            [1 :aka  "ivolga"]
            [1 :aka  "pi"]
            [2 :name "Petr"]
            [2 :aka  "porosenok"]
            [2 :aka  "pi"]]]
    (println (<! (d/q '[:find  ?n1 ?n2
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
    (println (<! (d/q '[ :find  ?n ?a
                   :where [?e :aka "dragon_killer_94"]
                          [?e :name ?n]
                          [?e :age  ?a]] db))
           #{["Ivan" 19]}))

  ; (testing "Query over long tuples"
    (let [db [ [1 :name "Ivan" 945 :db/add]
               [1 :age  39     999 :db/retract]] ]
      (println (<! (d/q '[ :find  ?e ?v
                     :where [?e :name ?v]] db))
             #{[1 "Ivan"]})
      (println (<! (d/q '[ :find  ?e ?a ?v ?t
                     :where [?e ?a ?v ?t :db/retract]] db))
             #{[1 :age 39 999]}))))

(defn test-q-in []
  (go
  (let [db [[1 :name  "Ivan"]
            [1 :age   15]
            [2 :name  "Petr"]
            [2 :age   37]
            [3 :name  "Ivan"]
            [3 :age   37]]
        query '{:find  [?e]
                :in    [$ ?attr ?value]
                :where [[?e ?attr ?value]]}]
    (println (<! (d/q query db :name "Ivan"))
           #{[1] [3]})
    (println (<! (d/q query db :age 37))
           #{[2] [3]})

    ; (testing "Named DB"
      (println (<! (d/q '[:find  ?a ?v
                    :in    $db ?e
                    :where [$db ?e ?a ?v]] db 1))
             #{[:name "Ivan"]
               [:age 15]})

    ; (testing "DB join with collection"
      (println (<! (d/q '[:find  ?e ?email
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
      (println (<! (d/q '[:find ?a ?b
                    :in   ?a ?b]
                  10 20))
             #{[10 20]}))))

(defn test-bindings []
  (go
  (let [db [[1 :name  "Ivan"]
            [1 :age   15]
            [2 :name  "Petr"]
            [2 :age   37]
            [3 :name  "Ivan"]
            [3 :age   37]]]
    ; (testing "Relation binding"
      (println (<! (d/q '[:find  ?e ?email
                    :in    $ [[?n ?email]]
                    :where [?e :name ?n]]
                  db
                  [["Ivan" "ivan@mail.ru"]
                   ["Petr" "petr@gmail.com"]]))
             #{[1 "ivan@mail.ru"]
               [2 "petr@gmail.com"]
               [3 "ivan@mail.ru"]})

    ; (testing "Tuple binding"
      (println (<! (d/q '[:find  ?e
                    :in    $ [?name ?age]
                    :where [?e :name ?name]
                           [?e :age ?age]]
                  db ["Ivan" 37]))
             #{[3]})

    ; (testing "Collection binding"
      (println (<! (d/q '[:find  ?attr ?value
                    :in    $ ?e [?attr ...]
                    :where [?e ?attr ?value]]
                  db 1 [:name :age]))
             #{[:name "Ivan"] [:age 15]})

    ; (testing "Empty coll handling"
      (println (<! (d/q '[:find ?id
                    :in $ [?id ...]
                    :where [?id :age _]]
               [[1 :name "Ivan"]
                [2 :name "Petr"]]))
             #{})
      (println (<! (d/q '[:find ?id
                    :in $ [[?id]]
                    :where [?id :age _]]
               [[1 :name "Ivan"]
                [2 :name "Petr"]]))
             #{})

    ; (testing "Placeholders"
      (println (<! (d/q '[:find ?x ?z
                    :in [?x _ ?z]]
                  [:x :y :z]))
             #{[:x :z]})
      (println (<! (d/q '[:find ?x ?z
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
  (println (<! (d/q '[:find  ?k ?v
                :in    [[?k ?v] ...]
                :where [(> ?v 1)]]
              {:a 1, :b 2, :c 3}))
         #{[:b 2] [:c 3]})

  (println (<! (d/q '[:find  ?k ?min ?max
              :in    [[?k ?v] ...] ?minmax
              :where [(?minmax ?v) [?min ?max]]
                     [(> ?max ?min)]]
            {:a [1 2 3 4]
             :b [5 6 7]
             :c [3]}
            #(vector (reduce min %) (reduce max %))))
       #{[:a 1 4] [:b 5 7]})

  (println (<! (d/q '[:find  ?k ?x
                :in    [[?k [?min ?max]] ...] ?range
                :where [(?range ?min ?max) [?x ...]]
                       [(even? ?x)]]
              {:a [1 7]
               :b [2 4]}
              range))
         #{[:a 2] [:a 4] [:a 6]
           [:b 2]})))

;TODO: make function to run all the tests
(test-joins)
(test-q-many)
(test-q-coll)
(test-q-in)
(test-bindings)
(test-nested-bindings)
