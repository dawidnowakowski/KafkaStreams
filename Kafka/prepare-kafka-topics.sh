#!/bin/bash

TOPICS=("flights-etl" "airport-anomalies")

echo "Deleting existing topics"
for topic in "${TOPICS[@]}"; do
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server broker-1:19092 \
    --delete --topic "$topic" || echo "Topic $topic doesn't exist"
done

sleep 5

echo "Creating topics"
for topic in "${TOPICS[@]}"; do
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server broker-1:19092 \
    --create --topic "$topic" \
    --partitions 3 \
    --replication-factor 1 \
    --if-not-exists
done

echo "Kafka ready to go"
