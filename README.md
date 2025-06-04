1. Sklonuj repozytorium \
git clone [https://github.com/BigDataStreamProcessing/ApacheKafkaDocker.git ](https://github.com/dawidnowakowski/KafkaStreams.git)

2. Włącz Docker'a. Gdy będzie gotowy, przejdź do folderu: \
cd KafkaStreams/Kafka

3. Następnie uruchom: \
docker compose up -d

4. Poczekaj, aż wszystkie kontenery wstaną
 
5. Wykonaj \
docker exec --workdir /home/appuser -it --user root broker-1 bash

6. Na początek upewnij się, że odpowiednie tematy są utworzone, wykonując polecenie: \
./prepare-kafka-topics.sh \
(usuwanie nieistniejących tematów spowoduje błędy, zignoruj je)

7. Pobierz dane przy użyciu skryptu: \
./download-data.sh

8. Kafka streams potrzebuje uruchomić RocksDB, do której brakuje biblioteki, zaisntaluj ją za pomocą:
./install-libs.sh

8. Następnie uruchom przetwarzanie:
./C.sh \
./A.sh \
(alternatywnie) java -cp /opt/kafka/libs/*:kafka-flights.jar com.example.bigdata.FlightAggregatorApp broker-1:19092 A \
(alternatywnie) java -cp /opt/kafka/libs/*:kafka-flights.jar com.example.bigdata.FlightAggregatorApp broker-1:19092 C

9. Załaduj dane w tematach Kafki, najpierw informacje o lotniskach, które musimy odczytać przed rozpoczęciem rejestrowania zdarzeń \
./load-airports.sh 

10. Teraz czas na uruchomienie generowania zdarzeń o lotach:
./load-flights.sh \


CREATE DATABASE IF NOT EXISTS streamdb;
USE streamdb;

CREATE TABLE data_sink (
    `key` VARCHAR(255) NOT NULL,
    departures BIGINT NOT NULL,
    departureDelays BIGINT NOT NULL,
    arrivals BIGINT NOT NULL,
    arrivalDelays BIGINT NOT NULL,
    PRIMARY KEY (`key`)
);


curl -X DELETE http://localhost:8083/connectors/kafka-to-mysql-task
curl -X POST http://localhost:8083/connectors -H "Content-Type: application/json" --data @kafka-mysql-connector.json

docker exec --workdir /opt/kafka/bin -it --user root broker-1 bash

./kafka-console-consumer.sh   --bootstrap-server broker-1:19092   --topic flights-etl   --from-beginning   --property print.key=true   --property print.value=true   --property key.separator=" : "