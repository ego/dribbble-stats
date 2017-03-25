(ns api-octopus.utils.url-match-test
  (:require [clojure.test :refer [is]]
            [api-octopus.utils.url-match :refer [pattern recognize]]))

(def twitter (pattern "host(twitter.com); path(?user/status/?id);"))
(recognize twitter "http://twitter.com/bradfitz/status/562360748727611392")
(is (= (recognize twitter "http://twitter.com/bradfitz/status/562360748727611392")
        [[:user "bradfitz"] [:id 562360748727611392]]))

(def dribbble (pattern "host(dribbble.com); path(shots/?id); queryparam(offset=?offset);"))
(is (= (recognize dribbble "https://dribbble.com/shots/1905065-Travel-Icons-pack?list=users&offset=1")
        [[:id "1905065-Travel-Icons-pack"] [:offset 1]]))

(is (= (recognize dribbble "https://twitter.com/shots/1905065-Travel-Icons-pack?list=users&offset=1") nil))
(is (= (recognize dribbble "https://dribbble.com/shots/1905065-Travel-Icons-pack?list=users") nil))

(def dribbble2 (pattern "host(dribbble.com); path(shots/?id); queryparam(offset=?offset); queryparam(type=?type);"))
(is (= (recognize dribbble2 "https://dribbble.com/shots/1905065-Travel-Icons-pack?offset=users") nil))
(is (= (recognize dribbble2 "https://dribbble.com/shots/1905065-Travel-Icons-pack?offset=users&type=list")
        [[:id "1905065-Travel-Icons-pack"] [:offset "users"] [:type "list"]]))

(def dribbble3 (pattern "host(dribbble.com); path(shots/?id); path(book/?art); queryparam(offset=?offset); queryparam(type=?type);"))
(is (= (recognize dribbble3 "https://dribbble.com/shots/1905065-Travel-Icons-pack/book/147894?offset=users") nil))
(is (= (recognize dribbble3 "https://dribbble.com/shots/1905065-Travel-Icons-pack/book/147894?offset=users&type=type")
        [[:id "1905065-Travel-Icons-pack"] [:art 147894] [:offset "users"] [:type "type"]]))

(def dribbble4 (pattern "host(?subsub.?subdomain.ali.com); path(product/?id/?color1/?color2); queryparam(campaign=?name);"))
(is (= (recognize dribbble4 "https://pp.product.ali.com/product/2076719/584/788/?campaign=s-23912-ikea")
        [[:subsub "pp"] [:subdomain "product"] [:id 2076719] [:color1 584] [:color2 788] [:campaign "s-23912-ikea"]]))
