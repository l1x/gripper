#!/usr/bin/env bash

/opt/kafka_2.9.2-0.8.1.1/bin/zookeeper-server-start.sh /opt/kafka_2.9.2-0.8.1.1/config/zookeeper.properties &
/opt/kafka_2.9.2-0.8.1.1/bin/kafka-server-start.sh /opt/kafka_2.9.2-0.8.1.1/config/server.properties
