#!/bin/bash

# loty
# mkdir -p kafka-flights
# cd kafka-flights

# ZIP_URL="http://www.cs.put.poznan.pl/kjankiewicz/bigdata/stream_project/flights-2015.zip"
# ZIP_FILE="flights-2015.zip"

# echo "Downloading flight data from $ZIP_URL"
# wget -O "$ZIP_FILE" "$ZIP_URL"

# echo "Unzipping $ZIP_FILE"
# unzip -o "$ZIP_FILE"
# rm "$ZIP_FILE"

# cd ..

# lotniska
mkdir -p kafka-airports
cd kafka-airports

AIRPORTS_URL="http://www.cs.put.poznan.pl/kjankiewicz/bigdata/stream_project/airports.csv"
CSV_FILE="airports.csv"

echo "Downloading airport data from $AIRPORTS_URL"
wget -O "$CSV_FILE" "$AIRPORTS_URL"

echo "All data ready"
