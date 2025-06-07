1. Włącz Docker'a. Gdy będzie gotowy, przejdź do folderu: \
cd KafkaStreams/Kafka

2. Następnie uruchom: \
docker compose up -d

3. Poczekaj, aż wszystkie kontenery wstaną
 
4. Podłącz się do broker Kafki, najlepiej od razu otwórz trzy terminale, w których to zrobisz \
docker exec --workdir /home/appuser -it --user root broker-1 bash

5. Na początek upewnij się, że odpowiednie tematy są utworzone, wykonując polecenie: \
./prepare-kafka-topics.sh \
(usuwanie nieistniejących tematów spowoduje błędy, zignoruj je)

6. Pobierz dane przy użyciu skryptu: \
./download-data.sh

7. Kafka streams potrzebuje uruchomić RocksDB, do której brakuje biblioteki, zaisntaluj ją za pomocą:
./install-libs.sh

8. Broker jest teraz gotowy do pracy, ale trzeba przygotować jeszcze ujście, odpal nowy terminal i podłącz się do kontenera z MySQL: \
docker exec -it mysql mysql -u streamuser -pstream streamdb

9. Następnie utwórz tabelę, która będzie ujściem dla agregatów typu C: \
docker exec -it mysql mysql -u streamuser -pstream streamdb

CREATE DATABASE IF NOT EXISTS streamdb; \
USE streamdb;

CREATE TABLE data_sink ( \
    `key` VARCHAR(255) NOT NULL, \
    departures BIGINT NOT NULL, \
    departureDelays BIGINT NOT NULL, \
    arrivals BIGINT NOT NULL, \
    arrivalDelays BIGINT NOT NULL, \
    PRIMARY KEY (`key`) \
); 

10. Aby dane trafiały z tematu Kafki do bazy danych, należy skonfigurować jeszcze kafka-connect, w tym celu wykonaj uruchom nowy terminal i wyślij plik konfiguracyjny do kafka-connect: \
curl -X POST http://localhost:8083/connectors -H "Content-Type: application/json" --data @kafka-mysql-connector.json \
Pomimo tego, że kontener Kafka-connect jest uruchomiony, to na początku może odrzucać requesta (Empty reply from server). Należy poczekać jakiś czas (do minuty) i spróbować jeszcze raz \
Oczekiwany response z kafka-connect to zawartość pliku który wysyłamy. 

11. W teorii wszystko jest już skonfigurowane. Wróc do jednego z terminali brokera i uruchom przetwarzanie:
./C.sh - dla trybu Complete, w którym wynik agregacji będzie umieszczany w bazie\
./A.sh - dla trybu Append, dla którego wynik będzie umieszczany w temacie Kafki, ponieważ stosujemy tutaj wyzwalacz natychmiastowy i gdybyśmy umieszczali wynik w bazie to wymagałoby to wielu update'ów, czego chyba chcemy uniknąć\
(alternatywnie) java -cp /opt/kafka/libs/*:kafka-flights.jar com.example.bigdata.FlightAggregatorApp broker-1:19092 A \
(alternatywnie) java -cp /opt/kafka/libs/*:kafka-flights.jar com.example.bigdata.FlightAggregatorApp broker-1:19092 C

12. Przełącz się do innego terminala brokera i załaduj dane do tematów Kafki, najpierw informacje o lotniskach, które musimy odczytać przed rozpoczęciem rejestrowania zdarzeń \
./load-airports.sh 

13. Zanim załadujesz dane o lotach poczekaj, aż poprzedni krok się zakończy. Następnie uruchom generowanie zdarzeń o lotach: \
./load-flights.sh 

14. Aby podejrzeć wyniki agregacji typu C uruchom w kontenerze, w którym jesteś podłączony do bazy danych polecenie: \
SELECT * FROM data_sink;

15. 




