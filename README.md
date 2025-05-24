1. Sklonuj repozytorium \
git clone [https://github.com/BigDataStreamProcessing/ApacheKafkaDocker.git ](https://github.com/dawidnowakowski/KafkaStreams.git)

2. Włącz Docker'a. Gdy będzie gotowy, przejdź do folderu: \
cd KafkaStreams/Kafka

3. następnie uruchom: \
docker compose up -d

4. poczekaj, aż Kafka się uruchomi, a następnie przejdź do katalogu nadrzędnego \
cd ..
 
5. Uruchom terminal w folderze głównym projektu. Jeżeli jesteś na Windowsie to uruchom git bash, na Linuxie terminal, a następnie wykonaj: \
./prepare_kafka.sh \
jeżeli tematów nie było, to próba usunięcia będzie rzucać błędy, ale można to zignorować

6. Następnie w innym terminalu podłącz się do węzła master Kafki: \
docker exec --workdir /opt/kafka/bin/ -it broker-1 bash \

7. Wyświetl dostępną listę tematów (nie powinna być pusta) \
./kafka-topics.sh --bootstrap-server broker-1:19092,broker-2:19092 --list




