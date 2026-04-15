package com.railroad.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stations")
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название станции обязательно")
    @Size(max = 100, message = "Название не должно превышать 100 символов")
    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RouteStation> routeStations = new ArrayList<>();

    public Station() {}

    public Station(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RouteStation> getRouteStations() {
        return routeStations;
    }

    public void setRouteStations(List<RouteStation> routeStations) {
        this.routeStations = routeStations;
    }
}
