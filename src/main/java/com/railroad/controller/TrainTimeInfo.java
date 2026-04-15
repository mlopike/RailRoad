package com.railroad.controller;

import java.time.LocalDateTime;

public class TrainTimeInfo {
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;

    public TrainTimeInfo(LocalDateTime departureTime, LocalDateTime arrivalTime) {
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }
}
