package com.railroad.service;

import com.railroad.entity.Booking;
import com.railroad.entity.RouteStation;
import com.railroad.entity.Station;
import com.railroad.entity.Ticket;
import com.railroad.entity.Train;
import com.railroad.repository.BookingRepository;
import com.railroad.repository.RouteStationRepository;
import com.railroad.repository.StationRepository;
import com.railroad.repository.TicketRepository;
import com.railroad.repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private RouteStationRepository routeStationRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TrainRepository trainRepository;

    public Booking createBooking(String passengerName, String email,
                                 Long fromStationId, Long toStationId,
                                 LocalDateTime travelDateTime) {
        Station fromStation = stationRepository.findById(fromStationId)
                .orElseThrow(() -> new RuntimeException("Станция отправления не найдена"));
        Station toStation = stationRepository.findById(toStationId)
                .orElseThrow(() -> new RuntimeException("Станция назначения не найдена"));

        Booking booking = new Booking();
        booking.setPassengerName(passengerName);
        booking.setEmail(email);
        booking.setFromStation(fromStation);
        booking.setToStation(toStation);
        booking.setTravelDateTime(travelDateTime);
        booking.setStatus(Booking.BookingStatus.PENDING);

        return bookingRepository.save(booking);
    }

    public Booking createBookingWithTrain(String passengerName, String email,
                                          Long fromStationId, Long toStationId,
                                          LocalDateTime travelDateTime, Long trainId) {
        Station fromStation = stationRepository.findById(fromStationId)
                .orElseThrow(() -> new RuntimeException("Станция отправления не найдена"));
        Station toStation = stationRepository.findById(toStationId)
                .orElseThrow(() -> new RuntimeException("Станция назначения не найдена"));
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new RuntimeException("Поезд не найден"));

        Booking booking = new Booking();
        booking.setPassengerName(passengerName);
        booking.setEmail(email);
        booking.setFromStation(fromStation);
        booking.setToStation(toStation);
        booking.setTravelDateTime(travelDateTime);
        booking.setSelectedTrain(train);
        
        // Рассчитываем цену и устанавливаем статус
        BigDecimal price = calculateTicketPrice(train, fromStation, toStation);
        booking.setTotalPrice(price);
        booking.setStatus(Booking.BookingStatus.TRAIN_SELECTED);

        return bookingRepository.save(booking);
    }

    public List<Booking> getBookingsByEmail(String email) {
        return bookingRepository.findByEmailOrderByCreatedAtDesc(email);
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));
    }

    public List<Train> findAvailableTrains(Long fromStationId, Long toStationId, LocalDateTime dateTime) {
        Station fromStation = stationRepository.findById(fromStationId)
                .orElseThrow(() -> new RuntimeException("Станция отправления не найдена"));
        Station toStation = stationRepository.findById(toStationId)
                .orElseThrow(() -> new RuntimeException("Станция назначения не найдена"));

        return routeStationRepository.findTrainsByRoute(fromStation, toStation);
    }

    public Booking selectTrain(Long bookingId, Long trainId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new RuntimeException("Поезд не найден"));

        booking.setSelectedTrain(train);

        BigDecimal price = calculateTicketPrice(train, booking.getFromStation(), booking.getToStation());
        booking.setTotalPrice(price);
        booking.setStatus(Booking.BookingStatus.TRAIN_SELECTED);

        return bookingRepository.save(booking);
    }

    private BigDecimal calculateTicketPrice(Train train, Station from, Station to) {
        List<RouteStation> routeStations = routeStationRepository.findByTrainOrderByStopOrderAsc(train);

        RouteStation fromRs = null;
        RouteStation toRs = null;

        for (RouteStation rs : routeStations) {
            if (rs.getStation().getId().equals(from.getId())) {
                fromRs = rs;
            }
            if (rs.getStation().getId().equals(to.getId())) {
                toRs = rs;
            }
        }

        if (fromRs == null || toRs == null || fromRs.getStopOrder() >= toRs.getStopOrder()) {
            throw new RuntimeException("Некорректный маршрут");
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (RouteStation rs : routeStations) {
            if (rs.getStopOrder() > fromRs.getStopOrder() && rs.getStopOrder() <= toRs.getStopOrder()) {
                totalPrice = totalPrice.add(rs.getPrice());
            }
        }

        return totalPrice;
    }

    public Booking payBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        if (booking.getStatus() != Booking.BookingStatus.TRAIN_SELECTED) {
            throw new RuntimeException("Нельзя оплатить заявку в текущем статусе");
        }

        // Проверка, что билет ещё не создан
        if (booking.getTickets() != null && !booking.getTickets().isEmpty()) {
            throw new RuntimeException("Билет уже создан для этой заявки");
        }

        booking.setStatus(Booking.BookingStatus.PAID);
        booking = bookingRepository.save(booking);

        Ticket ticket = new Ticket();
        ticket.setBooking(booking);
        ticket.setTrain(booking.getSelectedTrain());
        ticket.setFromStation(booking.getFromStation());
        ticket.setToStation(booking.getToStation());
        ticket.setPrice(booking.getTotalPrice());
        ticket.setStatus(Ticket.TicketStatus.PAID);
        ticket.setSeatNumber(generateSeatNumber());

        ticketRepository.save(ticket);
        
        // Добавляем билет в коллекцию заявки
        if (booking.getTickets() == null) {
            booking.setTickets(new java.util.ArrayList<>());
        }
        booking.getTickets().add(ticket);

        return booking;
    }

    private String generateSeatNumber() {
        // Генерация уникального номера места с проверкой
        int car = (int) (Math.random() * 10) + 1;
        int seat = (int) (Math.random() * 54) + 1;
        return car + "-" + seat;
    }

    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
}
