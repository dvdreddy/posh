(ns scratch
  (:require [datascript.core :as d]
            [posh.q-datoms :as qd]
            [posh.pull-datoms :as pd]
            ))

(def schema {:todo/name             {:db/unique :db.unique/identity}
             :todo/owner            {:db/valueType :db.type/ref
                                     :db/cardinality :db.cardinality/one}
             :task/category         {:db/valueType :db.type/ref}
             :category/todo         {:db/valueType :db.type/ref}
             :task/name             {:db/unique :db.unique/identity}
             :todo/display-category {:db/valueType :db.type/ref}
             :todo/numbers          {:db/cardinality :db.cardinality/many}
             :action/editing        {:db/cardinality :db.cardinality/many}})

(def conn (d/create-conn schema))

(def tempid (let [n (atom 0)] (fn [] (swap! n dec))))

(defn new-entity! [conn varmap]
  ((:tempids (d/transact! conn [(merge varmap {:db/id -1})])) -1))

(defn populate! [conn]
  (let [matt       (new-entity! conn {:person/name "Matt" :person/age 14})
        todo-id    (new-entity! conn {:todo/name "Matt's List" :todo/listing :all
                                      :todo/owner matt})
        at-home    (new-entity! conn {:category/name "At Home" :category/todo todo-id})
        work-stuff (new-entity! conn {:category/name "Work Stuff" :category/todo todo-id})
        hobby      (new-entity! conn {:category/name "Hobby" :category/todo todo-id})]
    (d/transact!
     conn
     [[:db/add todo-id :todo/numbers 12]
      [:db/add todo-id :todo/numbers 20]
      [:db/add todo-id :todo/numbers 443]
      {:db/id (tempid)
       :task/name "Clean Dishes"
       :task/done true
       :task/category at-home}
      {:db/id (tempid)
       :task/name "Mop Floors"
       :task/done true
       :task/pinned true
       :task/category at-home}
      {:db/id (tempid)
       :task/name "Draw a picture of a cat"
       :task/done false
       :task/category hobby}
      {:db/id (tempid)
       :task/name "Compose opera"
       :task/done true
       :task/category hobby}
      {:db/id (tempid)
       :task/name "stock market library"
       :task/done false
       :task/pinned true
       :task/category work-stuff}])))


(populate! conn)

(comment

  (pd/pull-datoms (d/db conn)
                  '[:todo/name :todo/numbers {:category/_todo [:category/name]
                                              :todo/owner [*]}]
                  2)

  (def pat
    (pd/pull-tx-pattern (d/db conn)
                        '[:todo/name :todo/numbers
                          {:category/_todo
                           [:category/name
                            {:task/_category [:task/name :task/done]}]
                           :todo/owner [*]}]
                        2))

  (pd/count-avs pat)
  (pd/count-eas pat)
  
  (pd/pull-tx-pattern (d/db conn)
                      '[:todo/name :todo/numbers
                        (limit :category/_todo 2)
                        {
                         :todo/owner [*]}]
                      2)

  (d/q '[:find ?a ?b
         :in ])

  (d/q '[ :find  ?k ?x
         :in    [[?k [?min ?max]] ...] ?range
         :where [(?range ?min ?max) [?x ...]]
         [(even? ?x)] ]
       { :a [1 7], :b [2 4] }
       range)



  (def dt (pd/pull-datoms
           (d/db conn)
           '[:todo/name :todo/numbers
             {:category/_todo
              [:category/name
               {:task/_category [:task/name :task/done]}]
              :todo/owner [*]}]
           2))

  )


(comment
  (qd/q-pattern '[:find ?task ?task-name ?list-name
                  :in $ ?true ?owner-name
                  :where
                  [?p :person/name ?owner-name]
                  [?todo :todo/owner ?p]
                  [?todo :todo/name ?list-name]
                  [?cat  :category/todo ?todo]
                  [?task :task/category ?cat]
                  [?task :task/done ?true]
                  [?task :task/name ?task-name]]
                @conn true "Matt")

  (qd/patterns-from-eavs
   {'?todo #{2}
    '?cat #{3 5}
    '?list-name #{"Matt's List"}
    '?task #{7 6 9}
    '?task-name #{"Mop Floors" "Compose opera" "Clean Dishes"}
    '?p #{1}}
   '[[?p :person/name "Matt"]
     [?todo :todo/owner ?p]
     [?todo :todo/name _]
     [?cat :category/todo ?todo]
     [?task :task/category ?cat]
     [?task :task/done true]
     [?task :task/name _]])

  (qd/patterns-from-eavs
   {'?a :a
    '?b :b
    '?c :c}
   '[[?a :jim "hogan"]])

  
  
  (qd/patterns-from-eavs
   {'?a :a
    '?b :b
    '?c :c}
   '[[?a :hogan ?c]])

  )

(comment
  (def qd
    (qd/q-datoms '[:find ?task ?task-name ?list-name
                   :in $ ?true ?owner-name
                   :where
                   [?p :person/name ?owner-name]
                   [?todo :todo/owner ?p]
                   [?todo :todo/name ?list-name]
                   [?cat  :category/todo ?todo]
                   [?task :task/category ?cat]
                   [?task :task/done ?true]
                   [?task :task/name ?task-name]]
                 @conn true "Matt"))


  (def conn2 (d/create-conn schema))
  (d/transact! conn2 qd)
  )

