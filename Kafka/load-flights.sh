#!/bin/bash

echo "Loading flight events into Kafka"

java -cp /opt/kafka/libs/*:TestProducer.jar \
  com.example.bigdata.TestProducer \
  kafka-flights 15 flights-input 0 \
  broker-1:19092,broker-2:19092

echo "Flight events loading started with interval of 15s"
