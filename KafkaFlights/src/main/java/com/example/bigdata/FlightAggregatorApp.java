package com.example.bigdata;

import com.example.bigdata.model.AirportRecord;
import com.example.bigdata.model.FlightEventForAggregation;
import com.example.bigdata.model.FlightRecord;
import com.example.bigdata.model.StateDayAggregation;
import com.example.bigdata.serde.JsonSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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
        String bootstrapServers = args[0];
        String delayMode = args.length > 1 ? args[1] : "A";
        System.out.println("Processing in mode " + delayMode);
        Properties config = new Properties();
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-flights-" + delayMode);
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, TimeExtractor.class);

        if (delayMode.equals("A")) {
            config.put(StreamsConfig.BUFFERED_RECORDS_PER_PARTITION_CONFIG, 1);
            config.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        }

        final StreamsBuilder builder = new StreamsBuilder();

        Serde<FlightRecord> flightSerde = new JsonSerde<>(FlightRecord.class);
        Serde<AirportRecord> airportSerde = new JsonSerde<>(AirportRecord.class);
        Serde<StateDayAggregation> aggSerde = new JsonSerde<>(StateDayAggregation.class);
        Serde<FlightEventForAggregation> eventSerde = new JsonSerde<>(FlightEventForAggregation.class);

        KStream<String, String> airportsRawStream = builder.stream(AIRPORTS_INPUT);
        KTable<String, AirportRecord> airportKTable = airportsRawStream
                .filter((key, value) -> AirportRecord.lineIsCorrect(value))
                .mapValues(AirportRecord::parseFromLine)
                .selectKey((oldKey, airport) -> airport.getIata())
                .toTable(Materialized.with(Serdes.String(), airportSerde));

        KStream<String, String> flightsStream = builder.stream(FLIGHTS_INPUT);
        KStream<String, FlightRecord> filteredFlights = flightsStream
                .filter((key, value) -> FlightRecord.lineIsCorrect(value))
                .mapValues(FlightRecord::parseFromLogLine);

        KStream<String, FlightRecord> keyedFlights = filteredFlights.selectKey((oldKey, flight) ->
                flight.getInfoType().equals("D") ? flight.getStartAirport() : flight.getDestAirport()
        );

        KStream<String, FlightEventForAggregation> enrichedStream = keyedFlights.join(
                airportKTable,
                (flight, airport) -> {
                    String state = airport.getState();
                    String dateStr;
                    long delay;

                    if (flight.getInfoType().equals("D")) {
                        dateStr = flight.getDepartureTime().split(" ")[0];
                        delay = calculateDelayMinutes(flight.getScheduledDepartureTime(), flight.getDepartureTime());
                    } else {
                        dateStr = flight.getArrivalTime().split(" ")[0];
                        delay = calculateDelayMinutes(flight.getScheduledArrivalTime(), flight.getArrivalTime());
                    }

                    return new FlightEventForAggregation(state, dateStr, flight.getInfoType(), delay);
                },
                Joined.with(Serdes.String(), flightSerde, airportSerde)
        );

        KStream<String, FlightEventForAggregation> keyedByStateDate = enrichedStream
                .selectKey((key, flight) -> flight.getState() + "_" + flight.getDate());

        if (delayMode.equals("C")) {
            Duration windowSize = Duration.ofMinutes(1);
            Duration grace = Duration.ofMinutes(5);

            KTable<Windowed<String>, StateDayAggregation> windowedAgg = keyedByStateDate
                    .groupByKey(Grouped.with(Serdes.String(), eventSerde))
                    .windowedBy(TimeWindows.ofSizeAndGrace(windowSize, grace))
                    .aggregate(
                            StateDayAggregation::new,
                            (key, value, agg) -> agg.add(value),
                            Materialized.with(Serdes.String(), aggSerde)

                    );

            windowedAgg.toStream().peek((key, value) ->
                    System.out.println("Final Aggregated [" + key.key() + " @ " + key.window().startTime() + "] = " + value)
            );

        } else {
            KTable<String, StateDayAggregation> stateDayAggTable = keyedByStateDate
                    .groupByKey(Grouped.with(Serdes.String(), new JsonSerde<>(FlightEventForAggregation.class)))
                    .aggregate(
                            StateDayAggregation::new,
                            (key, value, aggregate) -> aggregate.add(value),
                            Materialized
                                    .<String, StateDayAggregation, KeyValueStore<Bytes, byte[]>>as("agg-store")
                                    .withKeySerde(Serdes.String())
                                    .withValueSerde(aggSerde)
                                    .withCachingDisabled()
                    );


            stateDayAggTable.toStream().peek((key, value) ->
                    System.out.println("Aggregated [" + key + "] = " + value)
            );
        }

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

    private static long calculateDelayMinutes(String scheduled, String actual) {
        try {
            LocalDateTime schedTime = parseFlexibleDateTime(scheduled);
            LocalDateTime actualTime = parseFlexibleDateTime(actual);
            if (schedTime == null || actualTime == null) return 0;
            long diff = Duration.between(schedTime, actualTime).toMinutes();
            return Math.max(0, diff);
        } catch (Exception e) {
            return 0;
        }
    }

    public static LocalDateTime parseFlexibleDateTime(String input) {
        if (input == null || input.isBlank() || input.equals("\"\"")) {
            return null;
        }

        try {
            if (input.contains("T")) {
                return OffsetDateTime.parse(input).toLocalDateTime();
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return LocalDateTime.parse(input, formatter);
            }
        } catch (Exception e) {
            return null;
        }
    }


}
