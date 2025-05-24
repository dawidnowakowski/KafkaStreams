#!/bin/bash

mkdir -p kafka-input
cd kafka-input

ZIP_URL="http://www.cs.put.poznan.pl/kjankiewicz/bigdata/stream_project/flights-2015.zip"

ZIP_FILE="flights-2015.zip"

echo "Downloading from $ZIP_URL"
wget -O "$ZIP_FILE" "$ZIP_URL"

echo "Unzipping $ZIP_FILE"
unzip -o "$ZIP_FILE"

 rm "$ZIP_FILE"

echo "Data ready to go"
