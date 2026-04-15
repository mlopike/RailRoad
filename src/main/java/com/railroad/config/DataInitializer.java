package com.railroad.config;

import com.railroad.entity.RouteStation;
import com.railroad.entity.Station;
import com.railroad.entity.Train;
import com.railroad.entity.User;
import com.railroad.entity.User.Role;
import com.railroad.repository.RouteStationRepository;
import com.railroad.repository.StationRepository;
import com.railroad.repository.TrainRepository;
import com.railroad.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private RouteStationRepository routeStationRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) {
        // Создаем пользователей по умолчанию
        createUserIfNotExists("admin", "admin", "admin@railroad.ru", Role.ADMIN);
        createUserIfNotExists("user", "user", "user@railroad.ru", Role.USER);

        // Создаем станции
        Station moscow = createStation("Москва");
        Station tver = createStation("Тверь");
        Station spb = createStation("Санкт-Петербург");
        Station nizhny = createStation("Нижний Новгород");
        Station kazan = createStation("Казань");
        Station ekb = createStation("Екатеринбург");
        
        // Города Беларуси
        Station minsk = createStation("Минск");
        Station brest = createStation("Брест");
        Station gomel = createStation("Гомель");
        Station vitebsk = createStation("Витебск");
        Station grodno = createStation("Гродно");
        Station mogilev = createStation("Могилев");

        // Базовая дата - завтра
        LocalDateTime baseDate = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);

        // Создаем поезд Москва - Санкт-Петербург (завтра в 8:00)
        Train train1 = createTrain("001А");
        addRouteStation(train1, moscow, 1,
                baseDate,
                baseDate.plusMinutes(30),
                new BigDecimal("0"), false);
        addRouteStation(train1, tver, 2,
                baseDate.plusHours(2).plusMinutes(30),
                baseDate.plusHours(2).plusMinutes(45),
                new BigDecimal("15.50"), false);
        addRouteStation(train1, spb, 3,
                baseDate.plusHours(6),
                baseDate.plusHours(6),
                new BigDecimal("32.00"), true);

        // Создаем поезд Москва - Казань (завтра в 9:00)
        Train train2 = createTrain("002Б");
        addRouteStation(train2, moscow, 1,
                baseDate.plusHours(1),
                baseDate.plusHours(1).plusMinutes(30),
                new BigDecimal("0"), false);
        addRouteStation(train2, nizhny, 2,
                baseDate.plusHours(8),
                baseDate.plusHours(8).plusMinutes(30),
                new BigDecimal("38.50"), false);
        addRouteStation(train2, kazan, 3,
                baseDate.plusHours(12),
                baseDate.plusHours(12),
                new BigDecimal("25.00"), true);

        // Создаем поезд Москва - Екатеринбург (послезавтра в 10:00)
        Train train3 = createTrain("003В");
        LocalDateTime train3Date = baseDate.plusDays(1);
        addRouteStation(train3, moscow, 1,
                train3Date,
                train3Date.plusMinutes(30),
                new BigDecimal("0"), false);
        addRouteStation(train3, ekb, 2,
                train3Date.plusDays(1).plusHours(2),
                train3Date.plusDays(1).plusHours(2),
                new BigDecimal("110.00"), true);
        
        // Создаем поезд Москва - Минск - Брест (завтра в 7:00)
        Train train4 = createTrain("004Г");
        addRouteStation(train4, moscow, 1,
                baseDate.minusHours(1),
                baseDate.minusHours(1).plusMinutes(30),
                new BigDecimal("0"), false);
        addRouteStation(train4, minsk, 2,
                baseDate.plusHours(5),
                baseDate.plusHours(5).plusMinutes(30),
                new BigDecimal("45.00"), false);
        addRouteStation(train4, brest, 3,
                baseDate.plusHours(9),
                baseDate.plusHours(9),
                new BigDecimal("28.50"), true);
        
        // Создаем поезд Москва - Гомель (завтра в 11:00)
        Train train5 = createTrain("005Д");
        addRouteStation(train5, moscow, 1,
                baseDate.plusHours(3),
                baseDate.plusHours(3).plusMinutes(30),
                new BigDecimal("0"), false);
        addRouteStation(train5, gomel, 2,
                baseDate.plusHours(9),
                baseDate.plusHours(9),
                new BigDecimal("35.00"), true);
        
        // Создаем поезд Санкт-Петербург - Витебск - Минск (завтра в 6:00)
        Train train6 = createTrain("006Е");
        addRouteStation(train6, spb, 1,
                baseDate.minusHours(2),
                baseDate.minusHours(2).plusMinutes(30),
                new BigDecimal("0"), false);
        addRouteStation(train6, vitebsk, 2,
                baseDate.plusHours(4),
                baseDate.plusHours(4).plusMinutes(30),
                new BigDecimal("32.00"), false);
        addRouteStation(train6, minsk, 3,
                baseDate.plusHours(8),
                baseDate.plusHours(8),
                new BigDecimal("26.50"), true);
        
        // Создаем поезд Москва - Гродно (послезавтра в 9:00)
        Train train7 = createTrain("007Ж");
        LocalDateTime train7Date = baseDate.plusDays(1);
        addRouteStation(train7, moscow, 1,
                train7Date.plusHours(1),
                train7Date.plusHours(1).plusMinutes(30),
                new BigDecimal("0"), false);
        addRouteStation(train7, grodno, 2,
                train7Date.plusHours(10),
                train7Date.plusHours(10),
                new BigDecimal("52.00"), true);
        
        // Создаем поезд Минск - Могилев - Витебск (завтра в 8:00)
        Train train8 = createTrain("008З");
        addRouteStation(train8, minsk, 1,
                baseDate,
                baseDate.plusMinutes(30),
                new BigDecimal("0"), false);
        addRouteStation(train8, mogilev, 2,
                baseDate.plusHours(3),
                baseDate.plusHours(3).plusMinutes(30),
                new BigDecimal("18.50"), false);
        addRouteStation(train8, vitebsk, 3,
                baseDate.plusHours(6),
                baseDate.plusHours(6),
                new BigDecimal("15.00"), true);
    }

    private Station createStation(String name) {
        return stationRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .findFirst()
                .orElseGet(() -> stationRepository.save(new Station(name)));
    }

    private Train createTrain(String trainNumber) {
        return trainRepository.findByTrainNumber(trainNumber)
                .orElseGet(() -> trainRepository.save(new Train(trainNumber)));
    }

    private void addRouteStation(Train train, Station station, int stopOrder,
                                 LocalDateTime arrival, LocalDateTime departure,
                                 BigDecimal price, boolean isFinal) {
        RouteStation routeStation = new RouteStation();
        routeStation.setTrain(train);
        routeStation.setStation(station);
        routeStation.setStopOrder(stopOrder);
        routeStation.setArrivalTime(arrival);
        routeStation.setDepartureTime(departure);
        routeStation.setPrice(price);
        routeStation.setFinal(isFinal);
        routeStationRepository.save(routeStation);
    }

    private void createUserIfNotExists(String username, String password, String email, Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(new BCryptPasswordEncoder().encode(password));
            user.setEmail(email);
            user.setRole(role);
            user.setEnabled(true);
            userRepository.save(user);
        }
    }
}
