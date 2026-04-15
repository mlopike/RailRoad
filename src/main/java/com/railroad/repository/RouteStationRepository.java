package com.railroad.repository;

import com.railroad.entity.RouteStation;
import com.railroad.entity.Station;
import com.railroad.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RouteStationRepository extends JpaRepository<RouteStation, Long> {
    
    List<RouteStation> findByTrainOrderByStopOrderAsc(Train train);
    
    List<RouteStation> findByStation(Station station);
    
    @Query("SELECT DISTINCT rs FROM RouteStation rs " +
           "JOIN rs.train t " +
           "JOIN t.routeStations fromRs " +
           "JOIN t.routeStations toRs " +
           "WHERE fromRs.station = :fromStation " +
           "AND toRs.station = :toStation " +
           "AND fromRs.stopOrder < toRs.stopOrder " +
           "AND rs.station = :toStation " +
           "AND rs.arrivalTime >= :fromDateTime " +
           "ORDER BY rs.arrivalTime")
    List<RouteStation> findAvailableTrains(
        @Param("fromStation") Station fromStation,
        @Param("toStation") Station toStation,
        @Param("fromDateTime") LocalDateTime fromDateTime
    );
    
    @Query("SELECT DISTINCT t FROM Train t " +
           "JOIN t.routeStations fromRs " +
           "JOIN t.routeStations toRs " +
           "WHERE fromRs.station = :fromStation " +
           "AND toRs.station = :toStation " +
           "AND fromRs.stopOrder < toRs.stopOrder " +
           "ORDER BY t.trainNumber")
    List<Train> findTrainsByRoute(
        @Param("fromStation") Station fromStation,
        @Param("toStation") Station toStation
    );
}
