(ns datascript.test.async
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [datascript :as d]
   [datascript.core :as dc]
   [datascript.test.core :as tdc]
   [cljs.core.async :refer [<! put! chan]]))

(defn test-joins []
  (go
    (let [db (-> (d/empty-db)
                 (d/db-with [ { :db/id 1, :name  "Ivan", :age   15 }
                              { :db/id 2, :name  "Petr", :age   37 }
                              { :db/id 3, :name  "Ivan", :age   37 }
                              { :db/id 4, :age 15 }]))]
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

(test-joins)