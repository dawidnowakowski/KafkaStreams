#!/bin/bash

KAFKA_LIBS="/opt/kafka/libs/*"
JAR_FILE="kafka-flights.jar"
MAIN_CLASS="com.example.bigdata.FlightAggregatorApp"
BROKER="broker-1:19092"

java -cp "${KAFKA_LIBS}:${JAR_FILE}" ${MAIN_CLASS} ${BROKER}
