package com.example.bigdata;

import com.example.bigdata.model.*;
import com.example.bigdata.serde.ConnectJsonSerializer;
import com.example.bigdata.serde.JsonSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
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
    private final static String DAY_STATE_AGG_A = "flights-etl-A";
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
        config.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);

        if (delayMode.equals("A")) {
            config.put(StreamsConfig.BUFFERED_RECORDS_PER_PARTITION_CONFIG, 1);
        }


        final StreamsBuilder builder = new StreamsBuilder();

        Serde<FlightRecord> flightSerde = new JsonSerde<>(FlightRecord.class);
        Serde<AirportRecord> airportSerde = new JsonSerde<>(AirportRecord.class);
        Serde<StateDayAggregation> aggSerde = new JsonSerde<>(StateDayAggregation.class);
        Serde<FlightEventForAggregation> eventSerde = new JsonSerde<>(FlightEventForAggregation.class);
        final ConnectJsonSerializer connectJsonSerializer = new ConnectJsonSerializer();


        // pobieranie danych o lotniskach, mapowanie na obiekt, dodawanie klucza (potrzebny do joina) i insert do KTable.
        KStream<String, String> airportsRawStream = builder.stream(AIRPORTS_INPUT);
        KTable<String, AirportRecord> airportKTable = airportsRawStream
                .filter((key, value) -> AirportRecord.lineIsCorrect(value))
                .mapValues(AirportRecord::parseFromLine)
                .selectKey((oldKey, airport) -> airport.getIata())
                .toTable(Materialized.with(Serdes.String(), airportSerde));

        // pobieranie danych o lotach, filtrowanie poprawnych rekordów i mapowanie na odpowiedni obiekt,
        KStream<String, String> flightsStream = builder.stream(FLIGHTS_INPUT);
        KStream<String, FlightRecord> filteredFlights = flightsStream
                .filter((key, value) -> FlightRecord.lineIsCorrect(value))
                .mapValues(FlightRecord::parseFromLogLine);


        // dodawanie klucza do każdego lotu w zależności od tego, czy wyleciał, czy przyleciał
        KStream<String, FlightRecord> keyedFlights = filteredFlights.selectKey((oldKey, flight) ->
                flight.getInfoType().equals("D") ? flight.getStartAirport() : flight.getDestAirport()
        );

        // join na tabeli o lotach i strumieniu z lotami,
        // z każdego lotu wydobywamy potrzebne informacje i łączymy je z informacjami o lotnisku,
        // z lotniska pobieramy informacje o stanie a z lotu o opóźnieniu, typie opóźnienia i typie zdarzenia (wylot/przylot)
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

        // dodajemy klucz do strumienia
        KStream<String, FlightEventForAggregation> keyedByStateDate = enrichedStream
                .selectKey((key, flight) -> flight.getState() + "_" + flight.getDate());

        // agregacja typu C
        if (delayMode.equals("C")) {
            Duration oneDay = Duration.ofDays(1);
            Duration grace = Duration.ofMinutes(5);

            TimeWindows dailyWindows = TimeWindows.ofSizeAndGrace(oneDay, grace);

            KTable<Windowed<String>, StateDayAggregation> windowedAgg = keyedByStateDate
                    .groupByKey(Grouped.with(Serdes.String(), eventSerde))
                    .windowedBy(dailyWindows)
                    .aggregate(
                            StateDayAggregation::new,
                            (key, value, aggregate) -> aggregate.add(value),
                            Materialized.with(Serdes.String(), aggSerde)
                    )
                    // Używam suppress untilWindowCloses, co powoduje, że dane są umieszczane w temacie kafki dopiero wtedy, gdy okno się zamknie
                    .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()));

            windowedAgg.toStream()
                    .map((windowedKey, value) -> new KeyValue<>(windowedKey.key(), value))
                    .peek((key, value) -> System.out.printf("Final Aggregated [%s] = %s\n", key, value))
                    // serializacja na json, który również dodaje scheme - element potrzebny dla kafka-connect, aby ujście było tabelą
                    .mapValues(value -> connectJsonSerializer.serialize(DAY_STATE_AGG, value))
                    .to(DAY_STATE_AGG, Produced.with(Serdes.String(), Serdes.ByteArray()));
        }

        // agregacja typu A
        // w sumie to to samo co C tylko bez suppressed. Aby wymusić umieszczanie zdarzeń od razu w wynikowym temacie należy zmienić rozmiar bufora na jak najmniejszy (na górze pliku)
        else {
            Duration oneDay = Duration.ofDays(1);
            Duration grace = Duration.ofMinutes(5);

            TimeWindows dailyWindows = TimeWindows.ofSizeAndGrace(oneDay, grace);

            KTable<Windowed<String>, StateDayAggregation> windowedAgg = keyedByStateDate
                    .groupByKey(Grouped.with(Serdes.String(), eventSerde))
                    .windowedBy(dailyWindows)
                    .aggregate(
                            StateDayAggregation::new,
                            (key, value, aggregate) -> aggregate.add(value),
                            Materialized.with(Serdes.String(), aggSerde)
                    );

            windowedAgg.toStream()
                    .map((windowedKey, value) -> new KeyValue<>(windowedKey.key(), value))
                    .peek((key, value) -> System.out.printf("Aggregated [%s] = %s\n", key, value))
                    .mapValues(value -> connectJsonSerializer.serialize(DAY_STATE_AGG_A, value))
                    .to(DAY_STATE_AGG_A, Produced.with(Serdes.String(), Serdes.ByteArray()));
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
