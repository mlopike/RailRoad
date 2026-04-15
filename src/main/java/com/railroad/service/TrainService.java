package com.railroad.service;

import com.railroad.entity.RouteStation;
import com.railroad.entity.Station;
import com.railroad.entity.Train;
import com.railroad.repository.RouteStationRepository;
import com.railroad.repository.StationRepository;
import com.railroad.repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TrainService {

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private RouteStationRepository routeStationRepository;

    public List<Train> getAllTrains() {
        return trainRepository.findAllByOrderByTrainNumberAsc();
    }

    public Optional<Train> getTrainById(Long id) {
        return trainRepository.findById(id);
    }

    public Optional<Train> getTrainByNumber(String trainNumber) {
        return trainRepository.findByTrainNumber(trainNumber);
    }

    public Train createTrain(String trainNumber) {
        Train train = new Train(trainNumber);
        return trainRepository.save(train);
    }

    public void deleteTrain(Long id) {
        trainRepository.deleteById(id);
    }

    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    public Optional<Station> getStationById(Long id) {
        return stationRepository.findById(id);
    }

    public Station createStation(String name) {
        Station station = new Station(name);
        return stationRepository.save(station);
    }

    public void deleteStation(Long id) {
        stationRepository.deleteById(id);
    }

    public RouteStation addRouteStation(Long trainId, Long stationId, Integer stopOrder,
                                        LocalDateTime arrivalTime, LocalDateTime departureTime,
                                        BigDecimal price, boolean isFinal) {
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new RuntimeException("Поезд не найден"));
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Станция не найдена"));

        RouteStation routeStation = new RouteStation();
        routeStation.setTrain(train);
        routeStation.setStation(station);
        routeStation.setStopOrder(stopOrder);
        routeStation.setArrivalTime(arrivalTime);
        routeStation.setDepartureTime(departureTime);
        routeStation.setPrice(price);
        routeStation.setFinal(isFinal);

        return routeStationRepository.save(routeStation);
    }

    public List<RouteStation> getTrainRoute(Long trainId) {
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new RuntimeException("Поезд не найден"));
        return routeStationRepository.findByTrainOrderByStopOrderAsc(train);
    }

    public void deleteRouteStation(Long id) {
        routeStationRepository.deleteById(id);
    }
}
