# Dribbble stats project

    For a given Dribbble user find all followers
    For each follower find all shots
    For each shot find all "likers"
    Calculate Top 10 "likers"

## Requirements

### I Cassandra 3
cqlsh 5.0.1 | Cassandra 3.2.1 | CQL spec 3.4.0 | Native protocol v4

Install Cassandra on Ubuntu 14.04
http://docs.datastax.com/en/cassandra/3.0/cassandra/install/installDeb.html

Edit cassandra.yaml
/etc/cassandra/cassandra.yaml
start_rpc: true


Check cassandra:
nodetool -h localhost status
sudo service cassandra status

Run cqlsh
make cqlsh3.4.0


### II Apache Kafka 2.11-0.10.0.0
https://kafka.apache.org/documentation

Install Kafka on Ubuntu 14.04
https://www.digitalocean.com/community/tutorials/how-to-install-apache-kafka-on-ubuntu-14-04
http://apache.ip-connect.vn.ua/kafka/0.10.0.0/kafka_2.11-0.10.0.0.tgz

Run Kafka
sudo su kafka
nohup ~/kafka/bin/kafka-server-start.sh ~/kafka/config/server.properties > ~/kafka/kafka.log 2>&1 &

Create topik and send message
echo "Hello, World" | ~/kafka/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic TutorialTopic > /dev/null

Read message
~/kafka/bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic TutorialTopic --from-beginning
~/kafka/bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic onyx-api-octopus --from-beginning

Other commands
~/kafka/bin/kafka-topics.sh --list --zookeeper localhost:2181
~/kafka/bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic TutorialTopic --from-beginning
~/kafka/bin/kafka-console-producer.sh --broker-list=localhost:9093 --topic TutorialTopic

Alter topik data
~/kafka/bin/kafka-configs.sh --zookeeper localhost:2181 --alter --topic onyx-api-octopus --config retention.ms=1000

### III ZooKeeper
https://www.digitalocean.com/community/tutorials/how-to-install-apache-kafka-on-ubuntu-14-04

ZooKeeper tool Running-Exhibitor
https://github.com/soabase/exhibitor/wiki/Running-Exhibitor

### Makefile
    make cql-insert-token   - Insertn into cassandra initial token for meking requests to dribbble.com
                              more info http://developer.dribbble.com/v1/#authentication
    make cql-truncate-data   - Delete all app tables from Cassandra
    make cqlsh3.4.0          - run cqlsh
    make test-all            - run tests
    make uber                - create app .jar file
    make start-pears         - onyx start 7 pears with ENV prod
    make submit-dribbble-job - onyx submit dribbble-job with ENV prod
    make web-server          - run web server with ENV prod

### Run application
Sart before:
    + Cassandra
    + Kafka
    + ZooKeeper

    make uber
    make cql-insert-token
    make start-pears
    make submit-dribbble-job
    make web-server


### TODO
    + Build system
    + Interactive dev development
    + Frontend with reactive clojurescript (single page)
    + WebSocket
    + UI =)
    + Onyx plugin for recursive posts
    + Docker
