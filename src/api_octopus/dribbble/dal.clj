(ns api-octopus.dribbble.dal)

(def get-valid-count-call-cql
  "SELECT count(code) as count
   FROM \"api_octopus\".\"token\"
   WHERE count_call < 1440
   ALLOW FILTERING;")

(def get-valid-token-cql
  "SELECT * FROM \"api_octopus\".\"token\"
   WHERE available_from <= toTimestamp(now())
     AND count_call < 1440
   ALLOW FILTERING;")

(def reset-valid-token-cql
  "UPDATE \"api_octopus\".\"token\"
   SET count_call = 0
   WHERE code IN :codes;")

(def select-for-reset-token-cql
  "SELECT * FROM  \"api_octopus\".\"token\"
   WHERE available_from > :day-beforex
   ALLOW FILTERING;")

(defn lock-token! [token available-from]
  (format "UPDATE \"api_octopus\".\"token\"
           SET available_from = '%s',
               count_call = %d
           WHERE code = '%s';"
    available-from (-> token :count_call inc) (get token :code)))

(def top-user-likers-mv-cql
  "SELECT * FROM api_octopus.user_likers_count
   WHERE message_id= :message_id
     AND username= :username
   ORDER BY liker_count DESC LIMIT 10
   ALLOW FILTERING;")

(def insert-liker-cql
  "INSERT INTO api_octopus.user_likers
    (liker_count, message_id, username, likername, created_at)
   VALUES (:liker_count, :message_id, :username, :likername, toTimestamp(now()));")
