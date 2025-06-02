package com.example.bigdata.model;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AirportRecord implements Serializable {

    private static final Logger logger = Logger.getLogger("Airport");

    private String iata;
    private String state;
    private String name;
    private String city;
    private String timezone;

    private AirportRecord(String iata, String state, String name, String city, String timezone) {
        this.iata = iata;
        this.state = state;
        this.name = name;
        this.city = city;
        this.timezone = timezone;
    }

    public static AirportRecord parseFromLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 14) {
            logger.log(Level.WARNING, "Invalid airport line: " + line);
            throw new RuntimeException("Invalid airport record: " + line);
        }

        return new AirportRecord(
                parts[4],    // IATA
                parts[13],   // state
                parts[1],    // name
                parts[2],    // city
                parts[9]     // timezone
        );
    }

    public static boolean lineIsCorrect(String line) {
        if (line == null || line.trim().isEmpty()) return false;

        String lower = line.toLowerCase();
        if (lower.startsWith("airport")) return false;

        String[] parts = line.split(",");
        return parts.length >= 14;
    }


    @Override
    public String toString() {
        return String.format("AirportRecord{iata='%s', state='%s', name='%s', city='%s', timezone='%s'}",
                iata, state, name, city, timezone);
    }


    public String getIata() {
        return iata;
    }

    public void setIata(String iata) {
        this.iata = iata;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
