1. Sklonuj repozytorium \
git clone [https://github.com/BigDataStreamProcessing/ApacheKafkaDocker.git ](https://github.com/dawidnowakowski/KafkaStreams.git)

2. Włącz Docker'a. Gdy będzie gotowy, przejdź do folderu: \
cd KafkaStreams/Kafka

3. Następnie uruchom: \
docker compose up -d

4. Poczekaj, aż wszystkie kontenery wstaną
 
5. Wykonaj \
docker exec --workdir /opt/kafka/bin/ -it broker-1 bash

6. Na początek upewnij się, że odpowiednie tematy są utworzone, wykonując polecenie: \
cd /home/appuser \
./prepare-kafka-topics.sh \
(usuwanie nieistniejących tematów spowoduje błędy, zignoruj je)

7. Wyświetl dostępną listę tematów (nie powinna być pusta) \
cd /opt/kafka/bin/
./kafka-topics.sh --bootstrap-server broker-1:19092,broker-2:19092 --list

8. Powróć do katalogu domowego \
cd /home/appuser

9. Pobierz dane przy użyciu skryptu: \
./prepare-data.sh


