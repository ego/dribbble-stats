(ns api-octopus.web.layout
  (:require [ring.util.anti-forgery :refer [anti-forgery-field]]
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup
             [core :as hi]
             [form :as fm]
             [element :as el]]))

(defn application [title & content]
  (html5 {:ng-app "app" :lang "en"}
    [:head
     [:title title]
     (include-css "http://netdna.bootstrapcdn.com/twitter-bootstrap/2.3.1/css/bootstrap-combined.min.css")
     (include-js "/js/main.js")
     [:body
      [:div {:class "container"} content ]]]))

(defn index []
  [:div {:id "content"}
   [:h1 "api-octopus - dribbble.com stats project"]
   [:div
    (el/ordered-list
      ["For a given Dribbble user find all followers"
       "For each follower find all shots"
       "For each shot find all 'likers'"
       "Calculate Top 10 'likers'"])]
   [:div.form-group
    (fm/form-to [:post "/username"]
      (anti-forgery-field)
      [:div.form-inline
       (fm/text-field :username)
       (fm/submit-button "OK")])]])

(defn html-table [colls rows]
  [:table
   [:thead
    [:tr
     (for [coll colls]
       [:td (str coll)])]]
   [:tbody
    (for [{:keys [message_id username likername liker_count] :as row} rows]
      [:tr
       [:td (str message_id)]
       [:td (str username)]
       [:td (str likername)]
       [:td (str liker_count)]])]])

(defn index-post [data]
  [:div {:id "content"}
   [:h1 {:class "text-success"} "Username sent to Kafka"]
   [:div
    [:p (str (:username data) " " (:message_id data))]]
   [:div
    (el/link-to (str "/top10?username=" (:username data)
                  "&message_id=" (:message_id data))
      "Get top 10 likers")]
   (when-let [rows (:rows data)]
     [:div.mid
      [:br] [:br]
      (html-table ["message id" "username" "liker name" "liker count"] rows)])])
