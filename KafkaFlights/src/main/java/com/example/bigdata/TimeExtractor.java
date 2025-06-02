package com.example.bigdata;

import com.example.bigdata.model.FlightRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

public class TimeExtractor implements TimestampExtractor {
    @Override
    public long extract(ConsumerRecord<Object, Object> record, long previousTimestamp) {
        long timestamp = -1;
        if (record.topic().equals("flights-input") && record.value() instanceof String) {
            if (FlightRecord.lineIsCorrect(record.value().toString())) {
                try {
                    FlightRecord flight = FlightRecord.parseFromLogLine((String) record.value());
                    return flight.getTimestampInMillis();
                } catch (Exception e) {
                    return timestamp;
                }
            }
        }
        return timestamp;
    }
}
