VERSION = $(shell cat VERSION)
LEIN ?= lein

test-all:
	$(LEIN) test

api-octopus-$(VERSION).jar:
	$(LEIN) clean
	$(LEIN) uberjar
	mkdir target/uberjar
	mv target/api-octopus-standalone.jar target/uberjar/api-octopus-standalone.jar
	mv target/api-octopus-$(VERSION).jar target/uberjar/api-octopus-$(VERSION).jar

uber: api-octopus-$(VERSION).jar

lein-run-peers:
	$(LEIN) run -p prod start-peers 7

lein-submit-dribbble-job:
	$(LEIN) run -p prod submit-job dribbble-job

lein-web-server:
	$(LEIN) run -p prod web-server

lein-figwheel:
	$(LEIN) figwheel

start-pears:
	java -jar target/uberjar/api-octopus-standalone.jar -p prod start-peers 10

submit-dribbble-job:
	java -jar target/uberjar/api-octopus-standalone.jar -p prod submit-job dribbble-job

web-server:
	java -jar target/uberjar/api-octopus-standalone.jar -p prod web-server

kill-all-kobs:
	java -jar target/uberjar/api-octopus-standalone.jar -p prod kill-all-jobs 0

cql-insert-token:
	cqlsh localhost --cqlversion="3.4.0" -f resources/migrations/cassandra/init.cql

cql-truncate-data:
	cqlsh localhost --cqlversion="3.4.0" -f resources/migrations/cassandra/drop.cql

cqlsh3.4.0:
	cqlsh localhost --no-color --cqlversion="3.4.0"

cljfmt:
	lein cljfmt check

cljfmt-fix-all:
	lein cljfmt fix
