# Kafka Streams - Instrukcja uruchomienia

SYTUACJ A MAŁO PRAWDOPODOBNA, ALE - JEŻELI JAKIKOLWIEK SKRYPT .sh NIE DA SIĘ URUCHOMIĆ (INFORMACJA, ŻE PLIK NIE ZNALEZIONY), TO PRAWDOPODOBNIE CHODZI O ZNAKI KOŃCA LINII. WSZYSTKIE POWINNY MIEĆ LF, ALE WINDOWS PŁATA FIGLE I ZAMIENIA NA CRLF. W TAKIEJ SYTUACJI PROSZĘ O PRZEKONWERTOWANIE SOBIE ZNAKU KOŃCA LINI W NOTEPAD++ LUB JAKIMŚ ONLINE KONWERTERZE. PRZEPRASZAM ZA UTRUDNIENIA. W OSTATECZNOŚCI TRZEBA WYKONAĆ POLECENIA ZE SKRYPTÓW RĘCZNIE.

## 1. Uruchom Dockera i otwórz terminal w folderze projektu

## 2. Uruchom kontenery

```bash
docker compose up -d
```

Poczekaj, aż wszystkie kontenery zostaną uruchomione.

## 3. Podłącz się do brokera Kafka

Otwórz **dwa terminale** i w każdym z nich wykonaj:

```bash
docker exec --workdir /home/appuser -it --user root broker-1 bash
```

## 4. Przygotuj tematy Kafka

W terminalu brokera uruchom:

```bash
./prepare-kafka-topics.sh
```

>  Błędy związane z usuwaniem nieistniejących tematów można zignorować.

## 5. Pobierz dane

```bash
./download-data.sh
```

## 6. Zainstaluj bibliotekę RocksDB

Kafka Streams wymaga biblioteki RocksDB. Zainstaluj ją:

```bash
./install-libs.sh
```

## 7. Przygotuj bazę danych (MySQL)

Otwórz nowy terminal i podłącz się do kontenera MySQL:

```bash
docker exec -it mysql mysql -u streamuser -pstream streamdb
```

Następnie wykonaj poniższe zapytania SQL:

```sql
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
```

## 8. Skonfiguruj Kafka Connect

W nowym terminalu wyślij konfigurację connectora:

```bash
curl -X POST http://localhost:8083/connectors -H "Content-Type: application/json" --data @kafka-mysql-connector.json
```

> Kontener Kafka Connect może początkowo odrzucać żądanie (np. "Empty reply from server"). Poczekaj do minuty i spróbuj ponownie.  
> Oczekiwana odpowiedź to zawartość pliku `kafka-mysql-connector.json`.

## 9. Uruchom przetwarzanie Kafka Streams

W jednym z terminali brokera uruchom aplikację przetwarzającą dane:

### Tryb **Complete** – wynik trafia do bazy danych

```bash
./C.sh
```

### Tryb **Append** – wynik trafia do tematu Kafka

```bash
./A.sh
```

#### Alternatywnie można użyć bezpośrednio komendy Java:

```bash
java -cp /opt/kafka/libs/*:kafka-flights.jar com.example.bigdata.FlightAggregatorApp broker-1:19092 A
```

lub

```bash
java -cp /opt/kafka/libs/*:kafka-flights.jar com.example.bigdata.FlightAggregatorApp broker-1:19092 C
```

## 10. Załaduj dane do tematów Kafka

### a) Lotniska:

```bash
./load-airports.sh
```

### b) Loty (załaduj dopiero po zakończeniu poprzedniego kroku):

```bash
./load-flights.sh
```

## 11C. Sprawdź wyniki agregacji (typ C)

W kontenerze MySQL wykonaj zapytanie:

```sql
SELECT * FROM data_sink;
```

## 11A. Sprawdź wyniki agregacji (typ A)
Otwórz nowy terminal i podłącz się do kontenera brokera, a następnie uruchom nasłuch tematu:
```bash
   docker exec --workdir /opt/kafka/bin -it --user root broker-1 bash
   ./kafka-console-consumer.sh --bootstrap-server broker-1:19092 --topic flights-etl-A --from-beginning
```
Żeby dane ładnie się wyświetlały zamiast raw JSON użyj:
```bash
./kafka-console-consumer.sh \
  --bootstrap-server broker-1:19092 \
  --topic flights-etl-A \
  --from-beginning \
  --property print.key=true \
  --property key.separator=" | " \
  --property print.value=true \
  | awk -F ' \\| ' '{ key=$1; val=$2; for(i=3;i<=NF;i++){val=val" | "$i} print key "|" val }' \
  | while IFS='|' read -r key json; do
      payload=$(echo "$json" | jq -c '.payload' 2>/dev/null)
      if [ -n "$payload" ]; then
        echo "$key | $payload"
      else
        echo "$key | [invalid json]"
      fi
    done

```

## Dodatkowe informacje:
Jeżeli chcesz uruchomić przetwarzanie ponownie (zmienić tryb przetwarzania na A lub C), musisz najpierw zrestartować środowisko.

Najpierw przerwij nadawanie zdarzeń o lotach poprzez CTRL+C, przerwij również przetwarzanie używając CTRL+C (Dla trybu A terminal może nie reagować, jeżeli spamowanie CTRL-C nic nie da, to trzeba będzie przejść instrukcje od nowa) 

Jeżeli udało ci się przerwać przetwarzanie, to uruchom skrypt:
```bash
./prepare-kafka-topics.sh
```

Następnie (o ile jest potrzeba) wyczyść tabelę wynikową:
```sql
TRUNCATE TABLE data_sink;
```

W teorii możesz wrócić do punktu 9. i uruchomić przetwarzanie od nowa. Jeżeli wystąpi jakiś problem to zalecam przejście instrukcji od nowa.

Raczej nie będzie potrzeby, ale do usunięcia konfiguracji Kafka-connect użyj: 
```bash
curl -X DELETE http://localhost:8083/connectors/kafka-to-mysql-task
```

