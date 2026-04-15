package com.railroad.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность "Поезд"
 * Хранит информацию о поезде: номер, маршрут (станции), билеты
 */
@Entity
@Table(name = "trains")
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Уникальный номер поезда (например, "001А") */
    @NotBlank(message = "Номер поезда обязателен")
    @Column(unique = true, nullable = false)
    private String trainNumber;

    /** Маршрут поезда - список станций с временем и ценами */
    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RouteStation> routeStations = new ArrayList<>();

    /** Список билетов на этот поезд */
    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ticket> tickets = new ArrayList<>();

    public Train() {}

    public Train(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public List<RouteStation> getRouteStations() {
        return routeStations;
    }

    public void setRouteStations(List<RouteStation> routeStations) {
        this.routeStations = routeStations;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }
}
