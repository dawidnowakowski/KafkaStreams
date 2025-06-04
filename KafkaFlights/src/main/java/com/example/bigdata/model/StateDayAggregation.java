package com.example.bigdata.model;

public class StateDayAggregation {
    public long departures = 0;
    public long departureDelays = 0;
    public long arrivals = 0;
    public long arrivalDelays = 0;

    public StateDayAggregation() {}

    public StateDayAggregation add(FlightEventForAggregation flight) {
        if (flight.getInfoType().equals("D")) {
            departures++;
            departureDelays += Math.max(0, flight.getDelayMinutes());
        } else if (flight.getInfoType().equals("A")) {
            arrivals++;
            arrivalDelays += Math.max(0, flight.getDelayMinutes());
        }
        return this;
    }

    @Override
    public String toString() {
        return "StateDayAggregation{" +
                "departures=" + departures +
                ", departureDelays=" + departureDelays +
                ", arrivals=" + arrivals +
                ", arrivalDelays=" + arrivalDelays +
                '}';
    }
}

