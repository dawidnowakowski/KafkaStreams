package com.example.bigdata.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FlightRecord implements Serializable {

    private static final Logger logger = Logger.getLogger("Flight");

    private String startAirport;
    private String destAirport;
    private String scheduledDepartureTime;
    private String scheduledArrivalTime;
    private String departureTime;
    private String arrivalTime;
    private String orderColumn;  // UTC timestamp
    private String infoType;

    private FlightRecord(String startAirport, String destAirport, String scheduledDepartureTime,
                         String scheduledArrivalTime, String departureTime, String arrivalTime,
                         String orderColumn, String infoType) {
        this.startAirport = startAirport;
        this.destAirport = destAirport;
        this.scheduledDepartureTime = scheduledDepartureTime;
        this.scheduledArrivalTime = scheduledArrivalTime;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.orderColumn = orderColumn;
        this.infoType = infoType;
    }

    public static FlightRecord parseFromLogLine(String line) {
        String[] parts = line.split(",");
        if (parts.length < 24) {
            logger.log(Level.WARNING, "Invalid flight line: " + line);
            throw new RuntimeException("Invalid flight record: " + line);
        }

        return new FlightRecord(
                parts[3],   // start airport IATA
                parts[4],   // destination IATA
                parts[5],   // scheduled departure
                parts[8],   // scheduled arrival
                parts[9],   // real departure
                parts[13],  // real arrival
                parts[23],  // event time (UTC)
                parts[24]   // info type: A/D/C
        );
    }

    public static boolean lineIsCorrect(String line) {
        if (line == null || line.trim().isEmpty()) return false;

        String lower = line.toLowerCase();
        if (lower.startsWith("airline")) return false;

        String[] parts = line.split(",");
        return parts.length >= 24;
    }


    public long getTimestampInMillis() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(orderColumn, formatter);
            return dateTime.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Invalid timestamp: " + orderColumn);
            return -1;
        }
    }

    @Override
    public String toString() {
        return String.format("FlightRecord{from='%s', to='%s', schedDep='%s', schedArr='%s', dep='%s', arr='%s', time='%s', type='%s'}",
                startAirport, destAirport, scheduledDepartureTime, scheduledArrivalTime,
                departureTime, arrivalTime, orderColumn, infoType);
    }


    public String getStartAirport() {
        return startAirport;
    }

    public void setStartAirport(String startAirport) {
        this.startAirport = startAirport;
    }

    public String getDestAirport() {
        return destAirport;
    }

    public void setDestAirport(String destAirport) {
        this.destAirport = destAirport;
    }

    public String getScheduledDepartureTime() {
        return scheduledDepartureTime;
    }

    public void setScheduledDepartureTime(String scheduledDepartureTime) {
        this.scheduledDepartureTime = scheduledDepartureTime;
    }

    public String getScheduledArrivalTime() {
        return scheduledArrivalTime;
    }

    public void setScheduledArrivalTime(String scheduledArrivalTime) {
        this.scheduledArrivalTime = scheduledArrivalTime;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getOrderColumn() {
        return orderColumn;
    }

    public void setOrderColumn(String orderColumn) {
        this.orderColumn = orderColumn;
    }

    public String getInfoType() {
        return infoType;
    }

    public void setInfoType(String infoType) {
        this.infoType = infoType;
    }
}
