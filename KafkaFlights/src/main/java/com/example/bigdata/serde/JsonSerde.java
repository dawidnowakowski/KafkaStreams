package com.example.bigdata.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class JsonSerde<T> implements Serde<T> {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Class<T> clazz;

    public JsonSerde(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Serializer<T> serializer() {
        return new Serializer<T>() {
            @Override
            public byte[] serialize(String topic, T data) {
                try {
                    return mapper.writeValueAsBytes(data);
                } catch (Exception e) {
                    throw new RuntimeException("Serialization error", e);
                }
            }

            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {}
            @Override
            public void close() {}
        };
    }

    @Override
    public Deserializer<T> deserializer() {
        return new Deserializer<T>() {
            @Override
            public T deserialize(String topic, byte[] data) {
                try {
                    return mapper.readValue(data, clazz);
                } catch (Exception e) {
                    throw new RuntimeException("Deserialization error", e);
                }
            }

            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {}
            @Override
            public void close() {}
        };
    }
}
