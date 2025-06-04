package com.example.bigdata.serde;

import com.example.bigdata.model.StateDayAggregation;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.json.JsonConverter;

import java.util.HashMap;
import java.util.Map;

public class ConnectJsonSerializer {

    private final JsonConverter converter;

    public ConnectJsonSerializer() {
        converter = new JsonConverter();
        Map<String, String> config = new HashMap<>();
        config.put("schemas.enable", "true");
        converter.configure(config, false);
    }

    public byte[] serialize(String topic, StateDayAggregation value) {
        Schema schema = SchemaBuilder.struct().name("com.example.bigdata.model.StateDayAggregation")
                .field("departures", Schema.INT64_SCHEMA)
                .field("departureDelays", Schema.INT64_SCHEMA)
                .field("arrivals", Schema.INT64_SCHEMA)
                .field("arrivalDelays", Schema.INT64_SCHEMA)
                .build();

        Struct struct = new Struct(schema)
                .put("departures", value.departures)
                .put("departureDelays", value.departureDelays)
                .put("arrivals", value.arrivals)
                .put("arrivalDelays", value.arrivalDelays);

        return converter.fromConnectData(topic, schema, struct);
    }
}

