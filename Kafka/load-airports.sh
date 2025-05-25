#!/bin/bash

echo "Loading airport data into Kafka"

java -cp /opt/kafka/libs/*:TestProducer.jar \
  com.example.bigdata.TestProducer \
  kafka-airports 0 airports-input 0 \
  broker-1:19092,broker-2:19092

echo "Airport data fully loaded"
