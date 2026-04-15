package com.railroad.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность "Заявка пассажира"
 * Хранит информацию о заявке на поездку: пассажир, маршрут, дата, статус, выбранный поезд
 */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Имя пассажира обязательно")
    @Column(nullable = false)
    private String passengerName;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный email")
    @Column(nullable = false)
    private String email;

    /** Станция отправления */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_station_id", nullable = false)
    private Station fromStation;

    /** Станция назначения */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_station_id", nullable = false)
    private Station toStation;

    /** Дата и время поездки */
    @NotNull(message = "Дата и время поездки обязательны")
    @Column(name = "travel_datetime", nullable = false)
    private LocalDateTime travelDateTime;

    /** Выбранный поезд (заполняется после выбора пассажиром) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_train_id")
    private Train selectedTrain;

    /** Общая стоимость поездки */
    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    /** Статус заявки: PENDING, TRAIN_SELECTED, PAID, CANCELLED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    /** Дата создания заявки */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Список билетов (связь один-ко-многим) */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ticket> tickets = new ArrayList<>();

    public enum BookingStatus {
        PENDING,      // Заявка создана, ожидает выбора поезда
        TRAIN_SELECTED, // Поезд выбран, ожидает оплаты
        PAID,         // Оплачено
        CANCELLED     // Отменено
    }

    public Booking() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Station getFromStation() {
        return fromStation;
    }

    public void setFromStation(Station fromStation) {
        this.fromStation = fromStation;
    }

    public Station getToStation() {
        return toStation;
    }

    public void setToStation(Station toStation) {
        this.toStation = toStation;
    }

    public LocalDateTime getTravelDateTime() {
        return travelDateTime;
    }

    public void setTravelDateTime(LocalDateTime travelDateTime) {
        this.travelDateTime = travelDateTime;
    }

    public Train getSelectedTrain() {
        return selectedTrain;
    }

    public void setSelectedTrain(Train selectedTrain) {
        this.selectedTrain = selectedTrain;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }
}
