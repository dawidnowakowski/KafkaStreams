package com.example.bigdata.model;

public class FlightEventForAggregation {
    private String state;
    private String date;
    private String infoType;
    private long delayMinutes;

    public FlightEventForAggregation(String state, String date, String infoType, long delayMinutes) {
        this.state = state;
        this.date = date;
        this.infoType = infoType;
        this.delayMinutes = delayMinutes;
    }

    public FlightEventForAggregation() {
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getInfoType() {
        return infoType;
    }

    public void setInfoType(String infoType) {
        this.infoType = infoType;
    }

    public long getDelayMinutes() {
        return delayMinutes;
    }

    public void setDelayMinutes(long delayMinutes) {
        this.delayMinutes = delayMinutes;
    }

    @Override
    public String toString() {
        return "FlightEventForAggregation{" +
                "state='" + state + '\'' +
                ", date='" + date + '\'' +
                ", infoType='" + infoType + '\'' +
                ", delayMinutes=" + delayMinutes +
                '}';
    }
}

