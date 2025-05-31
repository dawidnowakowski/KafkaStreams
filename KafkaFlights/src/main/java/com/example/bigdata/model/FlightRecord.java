package com.example.bigdata.model;

import java.io.Serializable;

public class FlightRecord implements Serializable {
    String startAirport;            // IATA
    String destAirport;             // IATA
    String scheduledDepartureTime;  // non-UTC
    String scheduledArrivalTime;    // non-UTC
    String departureTime;           // real departure time
    String arrivalTime;             // real arrival time
    String orderColumn;             // event timestamp, assuming maximum 5 minutes of delay (watermark), assuming it's UTC!
    String infoType;                // D - departure, A - arrival, C - cancellation

}
