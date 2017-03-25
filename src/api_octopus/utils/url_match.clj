(ns api-octopus.utils.url-match
  (:require [clojure.string :as str]))

(def dot-re                #"\.")
(def last-slash-re         #"\/$")
(def slash-re              #"\/")
(def queryparam-re         #"\?")
(def queryparam-split-re   #"&")
(def queryparam-kv-re      #"=")
(def matcher-split-re      #";")

(defn filter-empty
  [seq]
  (filter (complement empty?) seq))

(defn clean-url
  [url]
  "Deacode and clean URL"
  (-> url str/trim
    java.net.URLDecoder/decode
    (str/replace last-slash-re "")))

(defn drop-schema
  [url]
  "Drop schema from URL"
  (let [s (str/split url #"\/\/")]
    (if (-> s count (= 2))
      (second s)
      (first s))))

(defn split-qp [ps]
  "Split URL query params"
  (into {} (map (fn [[k v]] (vector (keyword k) v))
             (map #(str/split % queryparam-kv-re)
               (str/split ps queryparam-split-re)))))

(defn parse-url
  [clean-url]
  "Parse URL and return map with vector params"
  (let [[host-path queryparam] (-> clean-url drop-schema
                                 (str/split queryparam-re)
                                 filter-empty)
        [host & path]          (-> host-path (str/split slash-re))]
    {:host       (into []  (filter-empty (str/split host dot-re)))
     :path       (into []  (filter-empty path))
     :queryparam (split-qp (or queryparam ""))}))

(defn subs-end [s take-from drop-from-end]
  (subs s take-from (- (count s) drop-from-end)))

(defn merge-args [coll skip-key]
  "queryparam - only unique, else first entry
  skip-key for queryparam"
  (reduce (fn [new-map [key val]]
            (let [k (get new-map key)]
              (if (nil? k)
                (assoc new-map key val)
                (assoc new-map key
                  (if (not= key skip-key) (vec (concat k val))
                      (into k val))))))
    {} coll))

(defn pattern
  [pattern]
  "Parse URL return vector of params and queryparam as map"
  (merge-args
    (map (fn [s]
           (condp (comp seq re-seq) s
             #"^host\(\S+\)$"                 :>> #(vector
                                                     (keyword "host")
                                                     (-> % first (subs-end 5 1)
                                                       (str/split dot-re)
                                                       filter-empty vec))
             #"^path\([\S]*\?*[\S]*\)$"       :>> #(vector
                                                     (keyword "path")
                                                     (-> % first (subs-end 5 1)
                                                       (str/split slash-re)
                                                       filter-empty vec))
             #"^queryparam\([\S]*\?*[\S]*\)$" :>> #(vector
                                                     (keyword "queryparam")
                                                     (into {} (map (fn [r] (-> r (subs-end 11 1)
                                                                             split-qp)) %)))
             nil))
      (map str/trim (str/split (str/replace pattern #"[\s]*" "") matcher-split-re)))
    :queryparam))

(defn match-bind
  [item-match]
  "Check for all parts are match"
  (if (every? identity (map :match item-match))
    (filter-empty
      (reduce conj [] (map (comp first vec #(dissoc % :match)) item-match)))))

(defn parse-int
  [s]
  "Parse int params"
  (if (re-matches (re-pattern "\\d+") s)
    (read-string s)
    s))

(defn parse-mather-bind
  [[p u]]
  "Parse bind-? form host and path"
  (if (.startsWith p "?")
    {:match true (keyword (subs p 1)) (parse-int u)}
    {:match (= p u)}))

(defn make-pairs [key pu]
  (let [[p u] (map key pu)] (map vector p u)))

(defn parse-bind-queryparam
  [p u]
  "Parse bind for queryparam"
  (for [pk   (keys p)
        :let [uv (get u pk)
              pv (get p pk)]]
    (if (nil? uv)
      {:match false}
      {:match (or (= pv uv) (.startsWith pv "?")) pk (parse-int uv)})))

(defn recognize
  [pattern url]
  "Recognize pattern and URL"
  (let [p  pattern u (parse-url url)
        pu [p u]]
    (when (and p u)
      (let [hs (make-pairs :host pu)
            ph (make-pairs :path pu)
            qp (parse-bind-queryparam (:queryparam p) (:queryparam u))]
        (not-empty (vec (concat (match-bind (concat (map parse-mather-bind
                                                      (concat hs ph)) qp)))))))))
