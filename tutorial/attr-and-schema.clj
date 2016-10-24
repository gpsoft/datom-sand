(require '[datomic.api :as d]
         '[datom-sand.util :as util])

(def uri-in-memory "datomic:mem://hello")

(d/delete-database uri-in-memory)
(d/create-database uri-in-memory)
(def conn (d/connect uri-in-memory))

(->> (util/read-edn "datom-sand/schema.edn")
     (util/tx-all conn))
(->> (util/read-edn "datom-sand/customers.edn")
     (util/tx-all conn))

