# Система железнодорожная касса

Техническая документация проекта веб-приложения "Система железнодорожная касса" на Spring Boot + Hibernate.

## Содержание

1. [Entity-классы](#1-entity-классы)
2. [Структура базы данных](#2-структура-базы-данных)
3. [Архитектура приложения](#3-архитектура-приложения)
4. [Шаблоны проектирования GoF](#4-шаблоны-проектирования-gof)
5. [Технологический стек](#5-технологический-стек)
6. [Бизнес-логика](#6-бизнес-логика)
7. [Примеры кода](#7-примеры-кода)
8. [Диаграммы](#8-диаграммы)

---

## 1. ENTITY-КЛАССЫ

### Список всех entity-классов:

| Класс | Таблица | Описание |
|-------|---------|----------|
| **User** | `users` | Пользователи системы (пассажиры и администраторы) |
| **Train** | `trains` | Поезда с номерами и маршрутами |
| **Station** | `stations` | Железнодорожные станции |
| **RouteStation** | `route_stations` | Станции в маршруте поезда (связующая сущность) |
| **Booking** | `bookings` | Заявки пассажиров на поездку |
| **Ticket** | `tickets` | Билеты, созданные на основе оплаченных заявок |

---

### Детальное описание каждого класса:

#### **1.1 User** (`users`)

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(min=3, max=50)
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank @Size(min=6)
    @Column(nullable = false)
    private String password;

    @NotBlank @Email
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;  // USER, ADMIN
}
```

**Поля:**

| Поле | Тип | Аннотации | Описание |
|------|-----|-----------|----------|
| id | Long | @Id, @GeneratedValue | Первичный ключ |
| username | String | @NotBlank, @Size, @Column(unique) | Имя пользователя |
| password | String | @NotBlank, @Size, @Column | Пароль (хэшированный) |
| email | String | @NotBlank, @Email, @Column(unique) | Email |
| enabled | boolean | @Column | Статус аккаунта |
| role | Role (enum) | @Enumerated(EnumType.STRING) | Роль: USER/ADMIN |

---

#### **1.2 Train** (`trains`)

```java
@Entity
@Table(name = "trains")
public class Train {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String trainNumber;

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RouteStation> routeStations = new ArrayList<>();

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ticket> tickets = new ArrayList<>();
}
```

**Поля:**

| Поле | Тип | Аннотации | Описание |
|------|-----|-----------|----------|
| id | Long | @Id, @GeneratedValue | Первичный ключ |
| trainNumber | String | @NotBlank, @Column(unique) | Номер поезда (уникальный) |
| routeStations | List<RouteStation> | @OneToMany | Связь один-ко-многим с остановками |
| tickets | List<Ticket> | @OneToMany | Связь один-ко-многим с билетами |

**Связи:**
- **One-to-Many** с `RouteStation` (один поезд → много остановок)
- **One-to-Many** с `Ticket` (один поезд → много билетов)

---

#### **1.3 Station** (`stations`)

```java
@Entity
@Table(name = "stations")
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max=100)
    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RouteStation> routeStations = new ArrayList<>();
}
```

**Поля:**

| Поле | Тип | Аннотации | Описание |
|------|-----|-----------|----------|
| id | Long | @Id, @GeneratedValue | Первичный ключ |
| name | String | @NotBlank, @Size(100), @Column | Название станции |
| routeStations | List<RouteStation> | @OneToMany | Связь с прохождениями поездов |

**Связи:**
- **One-to-Many** с `RouteStation` (одна станция → много прохождений поездов)

---

#### **1.4 RouteStation** (`route_stations`)

```java
@Entity
@Table(name = "route_stations")
public class RouteStation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @NotNull
    @Column(nullable = false)
    private Integer stopOrder;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_final")
    private boolean isFinal;
}
```

**Поля:**

| Поле | Тип | Аннотации | Описание |
|------|-----|-----------|----------|
| id | Long | @Id, @GeneratedValue | Первичный ключ |
| train | Train | @ManyToOne, @JoinColumn | Ссылка на поезд |
| station | Station | @ManyToOne, @JoinColumn | Ссылка на станцию |
| stopOrder | Integer | @NotNull, @Column | Порядок остановки в маршруте |
| arrivalTime | LocalDateTime | @Column | Время прибытия |
| departureTime | LocalDateTime | @Column | Время отправления |
| price | BigDecimal | @NotNull, @Column(precision=10, scale=2) | Цена участка |
| isFinal | boolean | @Column | Флаг конечной станции |

**Связи:**
- **Many-to-One** с `Train` (много остановок → один поезд)
- **Many-to-One** с `Station` (много прохождений → одна станция)

---

#### **1.5 Booking** (`bookings`)

```java
@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(nullable = false)
    private String passengerName;

    @NotBlank @Email @Column(nullable = false)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_station_id", nullable = false)
    private Station fromStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_station_id", nullable = false)
    private Station toStation;

    @NotNull
    @Column(name = "travel_datetime", nullable = false)
    private LocalDateTime travelDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_train_id")
    private Train selectedTrain;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ticket> tickets = new ArrayList<>();

    public enum BookingStatus {
        PENDING,      // Ожидает выбора поезда
        TRAIN_SELECTED, // Поезд выбран, ожидает оплаты
        PAID,         // Оплачено
        CANCELLED     // Отменено
    }
}
```

**Поля:**

| Поле | Тип | Аннотации | Описание |
|------|-----|-----------|----------|
| id | Long | @Id, @GeneratedValue | Первичный ключ |
| passengerName | String | @NotBlank, @Column | ФИО пассажира |
| email | String | @NotBlank, @Email, @Column | Email пассажира |
| fromStation | Station | @ManyToOne, @JoinColumn | Станция отправления |
| toStation | Station | @ManyToOne, @JoinColumn | Станция назначения |
| travelDateTime | LocalDateTime | @NotNull, @Column | Дата/время поездки |
| selectedTrain | Train | @ManyToOne, @JoinColumn | Выбранный поезд |
| totalPrice | BigDecimal | @Column(precision=10, scale=2) | Общая стоимость |
| status | BookingStatus (enum) | @Enumerated, @Column | Статус заявки |
| createdAt | LocalDateTime | @Column | Дата создания |
| tickets | List<Ticket> | @OneToMany | Связь с билетами |

**Связи:**
- **Many-to-One** с `Station` (fromStation)
- **Many-to-One** с `Station` (toStation)
- **Many-to-One** с `Train` (selectedTrain)
- **One-to-Many** с `Ticket`

---

#### **1.6 Ticket** (`tickets`)

```java
@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_station_id", nullable = false)
    private Station fromStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_station_id", nullable = false)
    private Station toStation;

    @Column(name = "seat_number")
    private String seatNumber;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TicketStatus status = TicketStatus.RESERVED;

    public enum TicketStatus {
        RESERVED,   // Забронировано
        PAID,       // Оплачено
        USED,       // Использовано
        CANCELLED   // Отменено
    }
}
```

**Поля:**

| Поле | Тип | Аннотации | Описание |
|------|-----|-----------|----------|
| id | Long | @Id, @GeneratedValue | Первичный ключ |
| booking | Booking | @ManyToOne, @JoinColumn | Связь с заявкой |
| train | Train | @ManyToOne, @JoinColumn | Поезд |
| fromStation | Station | @ManyToOne, @JoinColumn | Станция отправления |
| toStation | Station | @ManyToOne, @JoinColumn | Станция назначения |
| seatNumber | String | @Column | Номер места |
| price | BigDecimal | @NotNull, @Column(precision=10, scale=2) | Цена |
| status | TicketStatus (enum) | @Enumerated, @Column | Статус билета |

**Связи:**
- **Many-to-One** с `Booking`
- **Many-to-One** с `Train`
- **Many-to-One** с `Station` (fromStation)
- **Many-to-One** с `Station` (toStation)

---

## 2. СТРУКТУРА БАЗЫ ДАННЫХ

### ER-диаграмма (текстовое представление):

```
┌─────────────┐       ┌──────────────────┐       ┌─────────────┐
│   users     │       │    bookings      │       │   tickets   │
├─────────────┤       ├──────────────────┤       ├─────────────┤
│ id (PK)     │       │ id (PK)          │       │ id (PK)     │
│ username    │       │ passenger_name   │       │ booking_id  │──┐
│ password    │       │ email            │       │ train_id    │──┼──┐
│ email       │       │ from_station_id  │──┐    │ from_station│──┼──┼──┐
│ enabled     │       │ to_station_id    │──┼──┐ │ to_station  │──┼──┼──┼──┐
│ role        │       │ travel_datetime  │  │  │ │ seat_number │  │  │  │  │
└─────────────┘       │ selected_train_id│──┼──┼─┤ price       │  │  │  │  │
                      │ total_price      │  │  │ │ status      │  │  │  │  │
                      │ status           │  │  │ └─────────────┘  │  │  │  │
                      │ created_at       │  │  │                  │  │  │  │
                      └──────────────────┘  │  │                  │  │  │  │
                                            │  │                  │  │  │  │
┌─────────────┐       ┌──────────────────┐  │  │ ┌─────────────┐  │  │  │  │
│   trains    │       │  route_stations  │  │  │ │  stations   │  │  │  │  │
├─────────────┤       ├──────────────────┤  │  │ ├─────────────┤  │  │  │  │
│ id (PK)     │◄──────┤ id (PK)          │  │  │ │ id (PK)     │◄─┘  │  │  │
│ train_number│       │ train_id (FK)    │──┘  │ │ name        │     │  │  │
└─────────────┘       │ station_id (FK)  │─────┘ └─────────────┘     │  │  │
                      │ stop_order       │                          │  │  │
                      │ arrival_time     │                          │  │  │
                      │ departure_time   │                          │  │  │
                      │ price            │                          │  │  │
                      │ is_final         │                          │  │  │
                      └──────────────────┘                          │  │  │
                                                                    │  │  │
```

### Таблицы базы данных:

#### **users**

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | ID пользователя |
| username | VARCHAR(50) | UNIQUE, NOT NULL | Имя пользователя |
| password | VARCHAR(255) | NOT NULL | Хэш пароля (BCrypt) |
| email | VARCHAR(255) | UNIQUE, NOT NULL | Email |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | Активен ли |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'USER' | Роль (USER/ADMIN) |

#### **trains**

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | ID поезда |
| train_number | VARCHAR(255) | UNIQUE, NOT NULL | Номер поезда |

#### **stations**

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | ID станции |
| name | VARCHAR(100) | NOT NULL | Название станции |

#### **route_stations**

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | ID записи |
| train_id | BIGINT | FOREIGN KEY → trains.id, NOT NULL | Ссылка на поезд |
| station_id | BIGINT | FOREIGN KEY → stations.id, NOT NULL | Ссылка на станцию |
| stop_order | INT | NOT NULL | Порядок остановки |
| arrival_time | TIMESTAMP | NULL | Время прибытия |
| departure_time | TIMESTAMP | NULL | Время отправления |
| price | DECIMAL(10,2) | NOT NULL | Цена участка |
| is_final | BOOLEAN | DEFAULT FALSE | Конечная станция |

#### **bookings**

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | ID заявки |
| passenger_name | VARCHAR(255) | NOT NULL | ФИО пассажира |
| email | VARCHAR(255) | NOT NULL | Email |
| from_station_id | BIGINT | FOREIGN KEY → stations.id, NOT NULL | Станция отправления |
| to_station_id | BIGINT | FOREIGN KEY → stations.id, NOT NULL | Станция назначения |
| travel_datetime | TIMESTAMP | NOT NULL | Дата поездки |
| selected_train_id | BIGINT | FOREIGN KEY → trains.id, NULL | Выбранный поезд |
| total_price | DECIMAL(10,2) | NULL | Стоимость |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | Статус |
| created_at | TIMESTAMP | NOT NULL | Дата создания |

#### **tickets**

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | ID билета |
| booking_id | BIGINT | FOREIGN KEY → bookings.id, NULL | Ссылка на заявку |
| train_id | BIGINT | FOREIGN KEY → trains.id, NOT NULL | Поезд |
| from_station_id | BIGINT | FOREIGN KEY → stations.id, NOT NULL | Станция отправления |
| to_station_id | BIGINT | FOREIGN KEY → stations.id, NOT NULL | Станция назначения |
| seat_number | VARCHAR(255) | NULL | Номер места |
| price | DECIMAL(10,2) | NOT NULL | Цена |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'RESERVED' | Статус билета |

---

## 3. АРХИТЕКТУРА ПРИЛОЖЕНИЯ

### Структура пакетов:

```
com.railroad/
├── RailroadApplication.java          # Точка входа Spring Boot
├── config/
│   ├── SecurityConfig.java           # Настройка безопасности
│   └── DataInitializer.java          # Инициализация тестовых данных
├── controller/
│   ├── HomeController.java           # Главная страница
│   ├── AuthController.java           # Авторизация/регистрация
│   ├── PassengerController.java      # Пассажирский интерфейс
│   ├── AdminController.java          # Админ-панель (поезда, станции)
│   ├── AdminUserController.java      # Управление пользователями
│   ├── ProfileController.java        # Профиль пользователя
│   └── TrainTimeInfo.java            # DTO для времени поездов
├── dto/
│   └── UserRegistrationDto.java      # DTO для регистрации
├── entity/
│   ├── User.java                     # Сущность пользователя
│   ├── Train.java                    # Сущность поезда
│   ├── Station.java                  # Сущность станции
│   ├── RouteStation.java             # Сущность остановки
│   ├── Booking.java                  # Сущность заявки
│   └── Ticket.java                   # Сущность билета
├── repository/
│   ├── UserRepository.java           # Репозиторий пользователей
│   ├── TrainRepository.java          # Репозиторий поездов
│   ├── StationRepository.java        # Репозиторий станций
│   ├── RouteStationRepository.java   # Репозиторий остановок
│   ├── BookingRepository.java        # Репозиторий заявок
│   └── TicketRepository.java         # Репозиторий билетов
└── service/
    ├── UserService.java              # Сервис пользователей
    ├── TrainService.java             # Сервис поездов
    └── BookingService.java           # Сервис бронирования
```

---

### Контроллеры и REST-эндпоинты:

#### **HomeController**

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/` | Редирект на /admin или /passenger в зависимости от роли |
| GET | `/access-denied` | Страница отказа в доступе |

#### **AuthController** (`/auth`)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/login` | Страница входа |
| GET | `/register` | Страница регистрации |
| POST | `/register` | Регистрация нового пользователя |

#### **PassengerController** (`/passenger`)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/` | Форма поиска рейсов |
| POST | `/search` | Поиск поездов по маршруту |
| POST | `/book` | Создание заявки с выбором поезда |
| GET | `/bookings` | Список заявок пользователя |
| GET | `/bookings/{id}` | Детали заявки |
| POST | `/bookings/{id}/pay` | Оплата заявки |
| POST | `/bookings/{id}/cancel` | Отмена заявки |

#### **AdminController** (`/admin`)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/` | Админ-панель (сводка) |
| GET | `/trains` | Список поездов |
| GET | `/trains/new` | Форма создания поезда |
| POST | `/trains` | Создание поезда |
| GET | `/trains/{id}` | Детали поезда + маршрут |
| POST | `/trains/{id}/route` | Добавление станции в маршрут |
| POST | `/trains/delete/{id}` | Удаление поезда |
| GET | `/stations` | Список станций |
| POST | `/stations` | Создание станции |
| POST | `/stations/delete/{id}` | Удаление станции |
| GET | `/bookings` | Все заявки (админ) |

#### **AdminUserController** (`/admin/users`)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/` | Список пользователей |
| GET | `/new` | Форма создания пользователя |
| POST | `/` | Создание пользователя |
| POST | `/delete/{id}` | Удаление пользователя |
| POST | `/enable/{id}` | Активация пользователя |
| POST | `/disable/{id}` | Деактивация пользователя |

#### **ProfileController** (`/profile`)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/` | Страница профиля |
| POST | `/update` | Обновление профиля (email, пароль) |

---

### Основные сервисы и методы:

#### **UserService**

| Метод | Возвращает | Описание |
|-------|------------|----------|
| `loadUserByUsername(String)` | UserDetails | Загрузка для Spring Security |
| `findByUsername(String)` | Optional<User> | Поиск по имени |
| `findByEmail(String)` | Optional<User> | Поиск по email |
| `existsByUsername(String)` | boolean | Проверка существования |
| `existsByEmail(String)` | boolean | Проверка email |
| `registerUser(String, String, String)` | User | Регистрация обычного пользователя |
| `registerAdmin(String, String, String)` | User | Регистрация администратора |
| `getAllUsers()` | List<User> | Все пользователи |
| `deleteUser(Long)` | void | Удаление |
| `enableUser(Long)` | User | Активация |
| `disableUser(Long)` | User | Деактивация |
| `updateUser(User)` | User | Обновление |

#### **TrainService**

| Метод | Возвращает | Описание |
|-------|------------|----------|
| `getAllTrains()` | List<Train> | Все поезда |
| `getTrainById(Long)` | Optional<Train> | Поиск по ID |
| `getTrainByNumber(String)` | Optional<Train> | Поиск по номеру |
| `createTrain(String)` | Train | Создание поезда |
| `deleteTrain(Long)` | void | Удаление поезда |
| `getAllStations()` | List<Station> | Все станции |
| `getStationById(Long)` | Optional<Station> | Поиск станции по ID |
| `createStation(String)` | Station | Создание станции |
| `deleteStation(Long)` | void | Удаление станции |
| `addRouteStation(...)` | RouteStation | Добавить остановку в маршрут |
| `getTrainRoute(Long)` | List<RouteStation> | Маршрут поезда |
| `deleteRouteStation(Long)` | void | Удалить остановку из маршрута |

#### **BookingService**

| Метод | Возвращает | Описание |
|-------|------------|----------|
| `createBooking(...)` | Booking | Создание заявки без поезда |
| `createBookingWithTrain(...)` | Booking | Создание заявки с поездом |
| `getBookingsByEmail(String)` | List<Booking> | Заявки по email |
| `getBookingById(Long)` | Booking | Заявка по ID |
| `findAvailableTrains(...)` | List<Train> | Поиск доступных поездов |
| `selectTrain(Long, Long)` | Booking | Выбор поезда для заявки |
| `calculateTicketPrice(...)` | BigDecimal | Расчет стоимости |
| `payBooking(Long)` | Booking | Оплата заявки + создание билета |
| `cancelBooking(Long)` | Booking | Отмена заявки |
| `getAllBookings()` | List<Booking> | Все заявки |

---

### Repository-интерфейсы:

#### **UserRepository** (extends JpaRepository<User, Long>)
- `Optional<User> findByUsername(String)`
- `Optional<User> findByEmail(String)`
- `boolean existsByUsername(String)`
- `boolean existsByEmail(String)`

#### **TrainRepository** (extends JpaRepository<Train, Long>)
- `Optional<Train> findByTrainNumber(String)`
- `List<Train> findAllByOrderByTrainNumberAsc()`

#### **StationRepository** (extends JpaRepository<Station, Long>)
- `List<Station> findByNameContainingIgnoreCase(String)`

#### **RouteStationRepository** (extends JpaRepository<RouteStation, Long>)
- `List<RouteStation> findByTrainOrderByStopOrderAsc(Train)`
- `List<RouteStation> findByStation(Station)`
- `List<RouteStation> findAvailableTrains(...)` (JPQL query)
- `List<Train> findTrainsByRoute(...)` (JPQL query)

#### **BookingRepository** (extends JpaRepository<Booking, Long>)
- `List<Booking> findByEmailOrderByCreatedAtDesc(String)` (с @EntityGraph)
- `List<Booking> findByStatus(BookingStatus)` (с @EntityGraph)
- `Optional<Booking> findByIdWithDetails(Long)` (с @EntityGraph)

#### **TicketRepository** (extends JpaRepository<Ticket, Long>)
- `List<Ticket> findByBookingId(Long)`
- `List<Ticket> findByStatus(TicketStatus)`

---

## 4. ШАБЛОНЫ ПРОЕКТИРОВАНИЯ GoF

### Использованные паттерны:

#### **4.1 Repository Pattern**
**Где реализован:** Все интерфейсы в пакете `repository`  
**Описание:** Абстракция доступа к данным. Spring Data JPA автоматически генерирует реализации.

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

#### **4.2 Service Layer Pattern**
**Где реализован:** Все классы в пакете `service`  
**Описание:** Инкапсуляция бизнес-логики, транзакционность.

```java
@Service
@Transactional
public class BookingService {
    // Бизнес-логика бронирования
}
```

#### **4.3 MVC Pattern (Model-View-Controller)**
**Где реализован:** Вся архитектура приложения
- **Model:** Entity-классы, DTO
- **View:** Thymeleaf шаблоны (`.html`)
- **Controller:** Классы в пакете `controller`

#### **4.4 Dependency Injection (DI)**
**Где реализован:** Во всех компонентах через `@Autowired`  
**Описание:** Внедрение зависимостей через Spring IoC контейнер.

```java
@Autowired
private BookingService bookingService;
```

#### **4.5 Strategy Pattern (неявно)**
**Где реализован:** `SecurityConfig` с `DaoAuthenticationProvider`  
**Описание:** Различные стратегии аутентификации могут быть подключены.

#### **4.6 Factory Method (неявно)**
**Где реализован:** Spring создаёт бины через `@Bean` методы

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

#### **4.7 Observer Pattern (неявно)**
**Где реализован:** `CommandLineRunner` в `DataInitializer`  
**Описание:** Реагирование на событие запуска приложения.

---

## 5. ТЕХНОЛОГИЧЕСКИЙ СТЕК

| Компонент | Технология | Версия |
|-----------|------------|--------|
| **Фреймворк** | Spring Boot | 3.2.0 |
| **Java** | JDK | 17 |
| **ORM** | Hibernate (через Spring Data JPA) | 6.x (в составе SB 3.2.0) |
| **СУБД** | H2 Database | In-memory |
| **Сборка** | Maven | 3.x |
| **UI** | Thymeleaf | 3.x |
| **Безопасность** | Spring Security 6 | 6.x |
| **Валидация** | Hibernate Validator | Jakarta Validation 3.0 |
| **Пул соединений** | HikariCP (по умолчанию в SB) | 5.x |
| **Сервлет-контейнер** | Tomcat (встроенный) | 10.x |

### Конфигурация подключения к БД (`application.properties`):

```properties
# H2 Database Configuration
spring.datasource.url=jdbc:h2:mem:railroad_db
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Thymeleaf Configuration
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.mode=HTML
spring.thymeleaf.encoding=UTF-8

# Server
server.port=8080
server.servlet.context-path=/Railroad
```

---

## 6. БИЗНЕС-ЛОГИКА

### Основные алгоритмы:

#### **6.1 Поиск доступных поездов**
**Место:** `RouteStationRepository.findTrainsByRoute()`

```java
@Query("SELECT DISTINCT t FROM Train t " +
       "JOIN t.routeStations fromRs " +
       "JOIN t.routeStations toRs " +
       "WHERE fromRs.station = :fromStation " +
       "AND toRs.station = :toStation " +
       "AND fromRs.stopOrder < toRs.stopOrder " +
       "ORDER BY t.trainNumber")
List<Train> findTrainsByRoute(Station fromStation, Station toStation);
```

**Логика:** Находит все поезда, которые проходят через обе станции в правильном порядке.

---

#### **6.2 Расчет стоимости билета**
**Место:** `BookingService.calculateTicketPrice()`

```java
private BigDecimal calculateTicketPrice(Train train, Station from, Station to) {
    List<RouteStation> routeStations = routeStationRepository.findByTrainOrderByStopOrderAsc(train);
    
    RouteStation fromRs = null;
    RouteStation toRs = null;
    
    for (RouteStation rs : routeStations) {
        if (rs.getStation().getId().equals(from.getId())) fromRs = rs;
        if (rs.getStation().getId().equals(to.getId())) toRs = rs;
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
```

**Логика:** Суммирует цены всех участков маршрута между станциями отправления и назначения.

---

#### **6.3 Оформление и оплата заявки**
**Место:** `BookingService.payBooking()`

```java
public Booking payBooking(Long bookingId) {
    Booking booking = bookingRepository.findByIdWithDetails(bookingId)
            .orElseThrow(() -> new RuntimeException("Заявка не найдена"));
    
    if (booking.getStatus() != Booking.BookingStatus.TRAIN_SELECTED) {
        throw new RuntimeException("Нельзя оплатить заявку в текущем статусе");
    }
    
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
    booking.getTickets().add(ticket);
    
    return booking;
}
```

**Логика:** 
1. Проверяет статус заявки (должен быть TRAIN_SELECTED)
2. Проверяет, что билет ещё не создан
3. Меняет статус на PAID
4. Создаёт билет с уникальным номером места
5. Сохраняет билет и обновляет заявку

---

#### **6.4 Генерация номера места**
**Место:** `BookingService.generateSeatNumber()`

```java
private String generateSeatNumber() {
    int car = (int) (Math.random() * 10) + 1;   // Вагон 1-10
    int seat = (int) (Math.random() * 54) + 1;  // Место 1-54
    return car + "-" + seat;
}
```

---

### Работа с сессиями:
- **Spring Security** управляет HTTP-сессиями
- После логина создаётся `SecurityContext` с `Authentication`
- Данные пользователя доступны через `@AuthenticationPrincipal UserDetails`
- При logout сессия инвалидируется: `.invalidateHttpSession(true)`

---

### Фильтры и валидация:

#### **Валидация на уровне entity:**
```java
@NotBlank(message = "Имя пользователя обязательно")
@Size(min = 3, max = 50, message = "Имя пользователя должно быть от 3 до 50 символов")
private String username;
```

#### **Валидация в контроллерах:**
```java
@PostMapping("/register")
public String registerUser(@Valid @ModelAttribute("user") User user,
                           BindingResult result, ...) {
    if (result.hasErrors()) {
        return "auth/register";
    }
    // ...
}
```

#### **Проверка прав доступа:**
```java
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers("/passenger/**", "/profile").hasAnyRole("USER", "ADMIN")
```

---

## 7. ПРИМЕРЫ КОДА

### 7.1 Ключевые Entity-классы

#### **Booking.java** (полный пример)

```java
package com.railroad.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_station_id", nullable = false)
    private Station fromStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_station_id", nullable = false)
    private Station toStation;

    @NotNull(message = "Дата и время поездки обязательны")
    @Column(name = "travel_datetime", nullable = false)
    private LocalDateTime travelDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_train_id")
    private Train selectedTrain;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ticket> tickets = new ArrayList<>();

    public enum BookingStatus {
        PENDING,      // Заявка создана, ожидает выбора поезда
        TRAIN_SELECTED, // Поезд выбран, ожидает оплаты
        PAID,         // Оплачено
        CANCELLED     // Отменено
    }

    // Геттеры и сеттеры...
}
```

---

#### **RouteStation.java** (связующая сущность)

```java
package com.railroad.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "route_stations")
public class RouteStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @NotNull(message = "Порядковый номер остановки обязателен")
    @Column(nullable = false)
    private Integer stopOrder;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @NotNull(message = "Цена обязательна")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_final")
    private boolean isFinal;

    // Геттеры и сеттеры...
}
```

---

### 7.2 Контроллер с основными методами

#### **PassengerController.java**

```java
package com.railroad.controller;

import com.railroad.entity.*;
import com.railroad.service.BookingService;
import com.railroad.service.TrainService;
import com.railroad.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/passenger")
public class PassengerController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private TrainService trainService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String searchForm(Model model) {
        model.addAttribute("stations", trainService.getAllStations());
        return "passenger/search";
    }

    @PostMapping("/search")
    public String searchTrains(@RequestParam Long fromStationId,
                               @RequestParam Long toStationId,
                               Model model,
                               @AuthenticationPrincipal UserDetails userDetails) {
        Station fromStation = trainService.getStationById(fromStationId).orElse(null);
        Station toStation = trainService.getStationById(toStationId).orElse(null);
        
        List<Train> availableTrains = bookingService.findAvailableTrains(
                fromStationId, toStationId, null);
        
        Map<Long, TrainTimeInfo> trainTimes = new HashMap<>();
        for (Train train : availableTrains) {
            List<RouteStation> routeStations = trainService.getTrainRoute(train.getId());
            
            LocalDateTime departureTime = null;
            LocalDateTime arrivalTime = null;
            
            for (RouteStation rs : routeStations) {
                if (rs.getStation().getId().equals(fromStationId)) {
                    departureTime = rs.getDepartureTime();
                }
                if (rs.getStation().getId().equals(toStationId)) {
                    arrivalTime = rs.getArrivalTime();
                }
            }
            
            trainTimes.put(train.getId(), new TrainTimeInfo(departureTime, arrivalTime));
        }
        
        model.addAttribute("availableTrains", availableTrains);
        model.addAttribute("fromStation", fromStation);
        model.addAttribute("toStation", toStation);
        model.addAttribute("trainTimes", trainTimes);
        
        return "passenger/search-results";
    }

    @PostMapping("/book")
    public String createBooking(@RequestParam Long fromStationId,
                                @RequestParam Long toStationId,
                                @RequestParam Long trainId,
                                @RequestParam String passengerName,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            redirectAttributes.addFlashAttribute("error", "Необходимо войти в систему");
            return "redirect:/auth/login";
        }
        
        try {
            User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
            String email = user != null ? user.getEmail() : userDetails.getUsername();
            
            LocalDateTime travelDateTime = LocalDateTime.now().plusDays(1);
            
            Booking created = bookingService.createBookingWithTrain(
                    passengerName, email, fromStationId, toStationId,
                    travelDateTime, trainId);
            
            redirectAttributes.addFlashAttribute("success", "Заявка создана. Оплатите счёт.");
            return "redirect:/passenger/bookings/" + created.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/passenger";
        }
    }

    @PostMapping("/bookings/{id}/pay")
    public String payBooking(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        bookingService.payBooking(id);
        redirectAttributes.addFlashAttribute("success", "Оплата прошла успешно!");
        return "redirect:/passenger/bookings/" + id;
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancelBooking(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        bookingService.cancelBooking(id);
        redirectAttributes.addFlashAttribute("success", "Заявка отменена");
        return "redirect:/passenger/bookings";
    }
}
```

---

### 7.3 Сервис с бизнес-логикой

#### **BookingService.java** (ключевые методы)

```java
package com.railroad.service;

import com.railroad.entity.*;
import com.railroad.repository.*;
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

    /**
     * Создание заявки с выбранным поездом
     */
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

    /**
     * Расчет стоимости билета
     */
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

    /**
     * Оплата заявки и создание билета
     */
    public Booking payBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        if (booking.getStatus() != Booking.BookingStatus.TRAIN_SELECTED) {
            throw new RuntimeException("Нельзя опплатить заявку в текущем статусе");
        }

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
        
        if (booking.getTickets() == null) {
            booking.setTickets(new ArrayList<>());
        }
        booking.getTickets().add(ticket);

        return booking;
    }

    private String generateSeatNumber() {
        int car = (int) (Math.random() * 10) + 1;
        int seat = (int) (Math.random() * 54) + 1;
        return car + "-" + seat;
    }
}
```

---

### 7.4 Конфигурация подключения к БД

См. раздел 5 выше (`application.properties`).

---

## 8. ДИАГРАММЫ

### Диаграмма классов (UML, текстовое представление):

```
┌─────────────────────┐       ┌─────────────────────┐
│       User          │       │      Station        │
├─────────────────────┤       ├─────────────────────┤
│ -id: Long           │       │ -id: Long           │
│ -username: String   │       │ -name: String       │
│ -password: String   │       ├─────────────────────┤
│ -email: String      │       │ +getId()            │
│ -enabled: boolean   │       │ +getName()          │
│ -role: Role         │       │ +setName()          │
├─────────────────────┤       └─────────────────────┘
│ +getId()            │                 ▲
│ +getUsername()      │                 │
│ +getEmail()         │                 │
│ +getRole()          │                 │
└─────────────────────┘                 │
                                        │ 1
                                        │
                                        │
                                        │ *
                              ┌─────────────────────┐
                              │   RouteStation      │
                              ├─────────────────────┤
                              │ -id: Long           │
                              │ -stopOrder: Integer │
                              │ -arrivalTime:       │
                              │   LocalDateTime     │
                              │ -departureTime:     │
                              │   LocalDateTime     │
                              │ -price: BigDecimal  │
                              │ -isFinal: boolean   │
                              ├─────────────────────┤
                              │ +getId()            │
                              │ +getTrain()         │
                              │ +getStation()       │
                              │ +getStopOrder()     │
                              │ +getPrice()         │
                              └─────────────────────┘
                                        ▲
                                        │
                    ┌───────────────────┼───────────────────┐
                    │                   │                   │
                    │ *                 │ *                 │ *
┌─────────────────────┐       ┌─────────────────────┐       │
│       Train         │       │       Booking       │       │
├─────────────────────┤       ├─────────────────────┤       │
│ -id: Long           │       │ -id: Long           │       │
│ -trainNumber: String│       │ -passengerName:     │       │
├─────────────────────┤       │   String            │       │
│ +getId()            │       │ -email: String      │       │
│ +getTrainNumber()   │       │ -fromStation:       │       │
│ +getRouteStations() │       │   Station           │       │
│ +getTickets()       │       │ -toStation:         │       │
└─────────────────────┘       │   Station           │       │
        ▲                     │ -travelDateTime:    │       │
        │                     │   LocalDateTime     │       │
        │ *                   │ -selectedTrain:     │       │
        │                     │   Train             │       │
        │                     │ -totalPrice:        │       │
        │                     │   BigDecimal        │       │
        │                     │ -status:            │       │
        │                     │   BookingStatus     │       │
        │                     │ -createdAt:         │       │
        │                     │   LocalDateTime     │       │
        │                     ├─────────────────────┤       │
        │                     │ +getId()            │       │
        │                     │ +getPassengerName() │       │
        │                     │ +getEmail()         │       │
        │                     │ +getStatus()        │       │
        │                     │ +getTickets()       │       │
        │                     └─────────────────────┘       │
        │                               │                   │
        │                               │ 1                 │
        │                               │                   │
        │                               │ *                 │
        │                     ┌─────────────────────┐       │
        │                     │       Ticket        │◄──────┘
        │                     ├─────────────────────┤
        └─────────────────────┤ -id: Long           │
                              │ -booking: Booking   │
                              │ -train: Train       │
                              │ -fromStation:       │
                              │   Station           │
                              │ -toStation:         │
                              │   Station           │
                              │ -seatNumber: String │
                              │ -price: BigDecimal  │
                              │ -status:            │
                              │   TicketStatus      │
                              ├─────────────────────┤
                              │ +getId()            │
                              │ +getPrice()         │
                              │ +getStatus()        │
                              │ +getSeatNumber()    │
                              └─────────────────────┘
```

---

### ER-диаграмма базы данных:

```
+------------------+       +------------------+       +------------------+
|      users       |       |     bookings     |       |     tickets      |
+------------------+       +------------------+       +------------------+
| id (PK)          |       | id (PK)          |       | id (PK)          |
| username (UNQ)   |       | passenger_name   |       | booking_id (FK)  |----+
| password         |       | email            |       | train_id (FK)    |----+---> trains.id
| email (UNQ)      |       | from_station_id  |----+  | from_station(FK) |----+---> stations.id
| enabled          |       | to_station_id    |----+--|-- to_station(FK) |----+---> stations.id
| role             |       | travel_datetime  |    |  | seat_number      |    |
+------------------+       | selected_train_id|----+--|-- price           |    |
                           | total_price      |    |  | status           |    |
                           | status           |    |  +------------------+    |
                           | created_at       |    |                          |
                           +------------------+    |                          |
                                   ^               |                          |
                                   |               |                          |
                                   |               v                          v
                           +------------------+  +------------------+  +------------------+
                           |     trains       |  |    stations      |  |  route_stations  |
                           +------------------+  +------------------+  +------------------+
                           | id (PK)          |  | id (PK)          |  | id (PK)          |
                           | train_number(UNQ)|  | name             |  | train_id (FK)    |
                           +------------------+  +------------------+  | station_id (FK)  |
                                                                      | stop_order       |
                                                                      | arrival_time     |
                                                                      | departure_time   |
                                                                      | price            |
                                                                      | is_final         |
                                                                      +------------------+
```

---

## Резюме

Проект представляет собой полноценное Spring Boot приложение для управления железнодорожными перевозками с:

- **6 entity-классами** со сложными связями (One-to-Many, Many-to-One)
- **7 контроллерами** с ~30 эндпоинтами
- **3 сервисами** с бизнес-логикой бронирования и расчёта цен
- **6 репозиториями** с кастомными JPQL-запросами
- **Spring Security** с ролевой моделью (USER/ADMIN)
- **Thymeleaf** для серверного рендеринга
- **H2 in-memory БД** для разработки
- **Валидацией** на основе Jakarta Validation
- **Транзакционностью** через `@Transactional`

---

*Документ сгенерирован автоматически на основе анализа исходного кода проекта.*
