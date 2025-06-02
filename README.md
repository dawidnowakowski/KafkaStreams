1. Sklonuj repozytorium \
git clone [https://github.com/BigDataStreamProcessing/ApacheKafkaDocker.git ](https://github.com/dawidnowakowski/KafkaStreams.git)

2. Włącz Docker'a. Gdy będzie gotowy, przejdź do folderu: \
cd KafkaStreams/Kafka

3. Następnie uruchom: \
docker compose up -d

4. Poczekaj, aż wszystkie kontenery wstaną
 
5. Wykonaj \
docker exec --workdir /home/appuser -it broker-1 bash

6. Na początek upewnij się, że odpowiednie tematy są utworzone, wykonując polecenie: \
./prepare-kafka-topics.sh \
(usuwanie nieistniejących tematów spowoduje błędy, zignoruj je)

7. (opcjonalne) Wyświetl dostępną listę tematów (nie powinna być pusta) \
cd /opt/kafka/bin/ \
./kafka-topics.sh --bootstrap-server broker-1:19092,broker-2:19092 --list \
cd /home/appuser


8. Pobierz dane przy użyciu skryptu: \
./download-data.sh

9. Następnie uruchom przetwarzanie:
./run-processing.sh \
(alternatywnie) java -cp /opt/kafka/libs/*:kafka-flights.jar com.example.bigdata.FlightAggregatorApp broker-1:19092 

10. Załaduj dane w tematach Kafki, najpierw informacje o lotniskach, które musimy odczytać przed rozpoczęciem rejestrowania zdarzeń \
./load-airports.sh 


11. Teraz czas na uruchomienie generowania zdarzeń o lotach:
./load-flights.sh \



additional commands

docker exec --workdir /opt/kafka/bin -it broker-1 bash

./kafka-console-consumer.sh --bootstrap-server broker-1:19092 --topic airports-anomalies --from-beginning --property print.key=true --property print.value=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer


./kafka-console-consumer.sh --bootstrap-server broker-1:19092 --topic airports-input --from-beginning --property print.key=true --property print.value=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer