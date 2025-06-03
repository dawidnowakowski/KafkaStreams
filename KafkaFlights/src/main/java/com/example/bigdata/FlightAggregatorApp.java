package com.example.bigdata;

import com.example.bigdata.model.AirportRecord;
import com.example.bigdata.model.FlightRecord;
import com.example.bigdata.serde.JsonSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/*
aggregate for (day, state) should contain:
number of departures
sum of departures delays
number of arrivals
sum of arrivals delays
 */


/*
anomalies: each 10 minutes report airports to which in span of next D minutes >= N airplanes will arrive
report should contain:
windows boundaries
airport's name
airport's IATA
airport's city
airport's state
number of airplanes arriving in next D minutes
number of all airplanes in the sky flying to this airport
 */

// "flights-input" "airports-input" "flights-etl" "airports-anomalies"
// first record might not be data but column headers


public class FlightAggregatorApp {
    private final static String AIRPORTS_INPUT = "airports-input";
    private final static String FLIGHTS_INPUT = "flights-input";
    private final static String DAY_STATE_AGG = "flights-etl";
    private final static String ANOMALIES = "airports-anomalies";

    public static void main(String[] args) {
        Properties config = new Properties();
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, args[0]);
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-flights");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, TimeExtractor.class);

        final StreamsBuilder builder = new StreamsBuilder();

        // logic goes here

        Serde<AirportRecord> airportSerde = new JsonSerde<>(AirportRecord.class);


        KStream<String, String> airportsRawStream = builder.stream(
                AIRPORTS_INPUT,
                Consumed.with(Serdes.String(), Serdes.String())
        );
//        airportsRawStream.peek((key, value) -> System.out.println("Airport: " + key));

        KTable<String, AirportRecord> airportKTable = airportsRawStream
                .filter((key, value) -> AirportRecord.lineIsCorrect(value))
                .mapValues(AirportRecord::parseFromLine)
                .selectKey((oldKey, airport) -> airport.getIata())
                .toTable(Materialized.with(Serdes.String(), airportSerde));
        airportKTable.toStream().peek((key, value) -> System.out.println("Key: " + key + " Value: " + value));


        KStream<String, String> flightsStream = builder.stream(FLIGHTS_INPUT);
        KStream<String, FlightRecord> filteredFlights = flightsStream
                .filter((key, value) -> FlightRecord.lineIsCorrect(value))
                .mapValues(FlightRecord::parseFromLogLine)
                .filter((key, flight) -> {
                    String type = flight.getInfoType();
                    return type.equals("A") || type.equals("D");
                });
//        filteredFlights.peek((key, value) -> {
//            System.out.println(key + " : " + value);
//        });


        // logic ends here


        final Topology topology = builder.build();
        System.out.println(topology.describe());

        KafkaStreams streams = new KafkaStreams(topology, config);


        final CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
}
