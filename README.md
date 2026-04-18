# Система железнодорожная касса (Railroad)

Веб-приложение для автоматизации работы железнодорожной кассы: поиск рейсов, бронирование и покупка билетов, администрирование маршрутов.

## Техническое резюме для курсовой работы

**Дисциплина:** Моделирование систем автоматизации веб-ресурсов

---

## 1. ENTITY-КЛАССЫ И ИХ СВЯЗИ

### Список entity-классов

| Класс | Таблица БД | Описание |
|-------|-----------|----------|
| `User` | `users` | Пользователи системы (пассажиры и администраторы) |
| `Booking` | `bookings` | Заявки пассажиров на поездку |
| `Ticket` | `tickets` | Билеты, созданные после оплаты заявки |
| `Train` | `trains` | Поезда с уникальными номерами |
| `Station` | `stations` | Железнодорожные станции |
| `RouteStation` | `route_stations` | Станции маршрута конкретного поезда (связующая сущность) |

### Детальное описание классов

#### 1.1 User (`users`)

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Имя пользователя обязательно")
    @Size(min = 3, max = 50, message = "Имя пользователя должно быть от 3 до 50 символов")
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Пароль должен быть не менее 6 символов")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный email")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Номер телефона обязателен")
    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    public enum Role { USER, ADMIN }
}
```

**Поля:**
- `id` (Long, PK, auto-increment) — уникальный идентификатор
- `username` (VARCHAR(255), UNIQUE, NOT NULL) — имя пользователя
- `password` (VARCHAR(255), NOT NULL) — хэш пароля (BCrypt)
- `email` (VARCHAR(255), UNIQUE, NOT NULL) — email
- `phone` (VARCHAR(255), NOT NULL) — номер телефона (белорусский формат +375...)
- `enabled` (BOOLEAN, NOT NULL, DEFAULT TRUE) — статус учётной записи
- `role` (VARCHAR(255), NOT NULL, CHECK IN ('USER','ADMIN')) — роль пользователя

#### 1.2 Booking (`bookings`)

```java
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

    public enum BookingStatus { PENDING, TRAIN_SELECTED, PAID, CANCELLED }
}
```

**Поля:**
- `id` (Long, PK, auto-increment) — уникальный идентификатор заявки
- `passenger_name` (VARCHAR(255), NOT NULL) — ФИО пассажира
- `email` (VARCHAR(255), NOT NULL) — email пассажира
- `from_station_id` (BIGINT, FK → stations.id, NOT NULL) — станция отправления
- `to_station_id` (BIGINT, FK → stations.id, NOT NULL) — станция назначения
- `travel_datetime` (TIMESTAMP(6), NOT NULL) — дата и время поездки
- `selected_train_id` (BIGINT, FK → trains.id) — выбранный поезд
- `total_price` (NUMERIC(10,2)) — общая стоимость
- `status` (VARCHAR(255), NOT NULL, CHECK IN ('PENDING','TRAIN_SELECTED','PAID','CANCELLED'))
- `created_at` (TIMESTAMP(6), NOT NULL) — дата создания заявки
- Связь `tickets`: One-to-Many с `Ticket`, каскадирование ALL, orphanRemoval=true

**Связи:**
- `fromStation`: Many-to-One → `Station` (FetchType.LAZY)
- `toStation`: Many-to-One → `Station` (FetchType.LAZY)
- `selectedTrain`: Many-to-One → `Train` (FetchType.LAZY)
- `tickets`: One-to-Many ← `Ticket` (CascadeType.ALL, orphanRemoval=true)

#### 1.3 Ticket (`tickets`)

```java
@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_code", unique = true, length = 64)
    private String ticketCode;

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

    @NotNull(message = "Цена обязательна")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.RESERVED;

    public enum TicketStatus { RESERVED, PAID, USED, CANCELLED }
}
```

**Поля:**
- `id` (Long, PK, auto-increment) — уникальный идентификатор билета
- `ticket_code` (VARCHAR(64), UNIQUE) — уникальный код для QR-кода (формат: TICKET-YYYYMMDD-HHMMSS-RANDOM)
- `booking_id` (BIGINT, FK → bookings.id) — ссылка на заявку
- `train_id` (BIGINT, FK → trains.id, NOT NULL) — поезд
- `from_station_id` (BIGINT, FK → stations.id, NOT NULL) — станция отправления
- `to_station_id` (BIGINT, FK → stations.id, NOT NULL) — станция назначения
- `seat_number` (VARCHAR(255)) — номер места (формат: "вагон-место", например "5-42")
- `price` (NUMERIC(10,2), NOT NULL) — цена билета
- `status` (VARCHAR(255), NOT NULL, CHECK IN ('RESERVED','PAID','USED','CANCELLED'))

**Связи:**
- `booking`: Many-to-One → `Booking` (FetchType.LAZY)
- `train`: Many-to-One → `Train` (FetchType.LAZY)
- `fromStation`: Many-to-One → `Station` (FetchType.LAZY)
- `toStation`: Many-to-One → `Station` (FetchType.LAZY)

#### 1.4 Train (`trains`)

```java
@Entity
@Table(name = "trains")
public class Train {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Номер поезда обязателен")
    @Column(unique = true, nullable = false)
    private String trainNumber;

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RouteStation> routeStations = new ArrayList<>();

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ticket> tickets = new ArrayList<>();
}
```

**Поля:**
- `id` (Long, PK, auto-increment)
- `train_number` (VARCHAR(255), UNIQUE, NOT NULL) — номер поезда (например, "001А")

**Связи:**
- `routeStations`: One-to-Many ← `RouteStation` (CascadeType.ALL, orphanRemoval=true)
- `tickets`: One-to-Many ← `Ticket` (CascadeType.ALL, orphanRemoval=true)

#### 1.5 Station (`stations`)

```java
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
}
```

**Поля:**
- `id` (Long, PK, auto-increment)
- `name` (VARCHAR(255), NOT NULL) — название станции

**Связи:**
- `routeStations`: One-to-Many ← `RouteStation` (CascadeType.ALL, orphanRemoval=true)

#### 1.6 RouteStation (`route_stations`)

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
}
```

**Поля:**
- `id` (Long, PK, auto-increment)
- `train_id` (BIGINT, FK → trains.id, NOT NULL)
- `station_id` (BIGINT, FK → stations.id, NOT NULL)
- `stop_order` (INTEGER, NOT NULL) — порядковый номер остановки в маршруте
- `arrival_time` (TIMESTAMP(6)) — время прибытия
- `departure_time` (TIMESTAMP(6)) — время отправления
- `price` (NUMERIC(10,2), NOT NULL) — цена участка маршрута от предыдущей станции
- `is_final` (BOOLEAN) — флаг конечной станции

**Связи:**
- `train`: Many-to-One → `Train` (FetchType.LAZY)
- `station`: Many-to-One → `Station` (FetchType.LAZY)

---

## 2. СТРУКТУРА БАЗЫ ДАННЫХ

### Таблицы и поля

#### users
| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY, GENERATED | ID пользователя |
| username | VARCHAR(255) | UNIQUE, NOT NULL | Имя пользователя |
| password | VARCHAR(255) | NOT NULL | Хэш пароля |
| email | VARCHAR(255) | UNIQUE, NOT NULL | Email |
| phone | VARCHAR(255) | NOT NULL | Телефон |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | Активен ли |
| role | VARCHAR(255) | NOT NULL, CHECK IN ('USER','ADMIN') | Роль |

#### bookings
| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY, GENERATED | ID заявки |
| passenger_name | VARCHAR(255) | NOT NULL | ФИО пассажира |
| email | VARCHAR(255) | NOT NULL | Email |
| from_station_id | BIGINT | FOREIGN KEY → stations(id), NOT NULL | Станция отправления |
| to_station_id | BIGINT | FOREIGN KEY → stations(id), NOT NULL | Станция назначения |
| travel_datetime | TIMESTAMP(6) | NOT NULL | Дата поездки |
| selected_train_id | BIGINT | FOREIGN KEY → trains(id) | Выбранный поезд |
| total_price | NUMERIC(10,2) | | Общая стоимость |
| status | VARCHAR(255) | NOT NULL, CHECK IN ('PENDING','TRAIN_SELECTED','PAID','CANCELLED') | Статус |
| created_at | TIMESTAMP(6) | NOT NULL | Дата создания |

#### tickets
| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY, GENERATED | ID билета |
| ticket_code | VARCHAR(64) | UNIQUE | Код для QR-кода |
| booking_id | BIGINT | FOREIGN KEY → bookings(id) | Заявка |
| train_id | BIGINT | FOREIGN KEY → trains(id), NOT NULL | Поезд |
| from_station_id | BIGINT | FOREIGN KEY → stations(id), NOT NULL | Станция отправления |
| to_station_id | BIGINT | FOREIGN KEY → stations(id), NOT NULL | Станция назначения |
| seat_number | VARCHAR(255) | | Номер места |
| price | NUMERIC(10,2) | NOT NULL | Цена |
| status | VARCHAR(255) | NOT NULL, CHECK IN ('RESERVED','PAID','USED','CANCELLED') | Статус |

#### trains
| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY, GENERATED | ID поезда |
| train_number | VARCHAR(255) | UNIQUE, NOT NULL | Номер поезда |

#### stations
| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY, GENERATED | ID станции |
| name | VARCHAR(255) | NOT NULL | Название |

#### route_stations
| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY, GENERATED | ID записи |
| train_id | BIGINT | FOREIGN KEY → trains(id), NOT NULL | Поезд |
| station_id | BIGINT | FOREIGN KEY → stations(id), NOT NULL | Станция |
| stop_order | INTEGER | NOT NULL | Порядок остановки |
| arrival_time | TIMESTAMP(6) | | Время прибытия |
| departure_time | TIMESTAMP(6) | | Время отправления |
| price | NUMERIC(10,2) | NOT NULL | Цена участка |
| is_final | BOOLEAN | | Флаг конечной |

### ER-диаграмма (ASCII)

```
┌─────────────┐       ┌──────────────────┐       ┌─────────────┐
│   users     │       │    bookings      │       │   tickets   │
├─────────────┤       ├──────────────────┤       ├─────────────┤
│ id (PK)     │       │ id (PK)          │       │ id (PK)     │
│ username    │       │ passenger_name   │       │ ticket_code │
│ password    │       │ email            │◄──────│ booking_id  │
│ email       │       │ from_station_id  │───────│ train_id    │
│ phone       │       │ to_station_id    │───────│ from_sta_id │
│ enabled     │       │ travel_datetime  │       │ to_sta_id   │
│ role        │       │ selected_train_id│───────│ seat_number │
└─────────────┘       │ total_price      │       │ price       │
                      │ status           │       │ status      │
                      │ created_at       │       └─────────────┘
                      └──────────────────┘              ▲
         ▲                 ▲  ▲  ▲                      │
         │                 │  │  └──────────────────────┘
         │                 │  │
         │                 │  └────────────────┐
         │                 │                   │
         │                 ▼                   ▼
         │         ┌──────────────┐     ┌─────────────┐
         │         │   stations   │     │   trains    │
         │         ├──────────────┤     ├─────────────┤
         │         │ id (PK)      │     │ id (PK)     │
         │         │ name         │     │ train_number│
         │         └──────────────┘     └─────────────┘
         │                 ▲                   ▲
         │                 │                   │
         │                 │                   │
         │                 │                   │
         │         ┌───────────────────────────────┐
         │         │     route_stations            │
         │         ├───────────────────────────────┤
         │         │ id (PK)                       │
         │         │ train_id (FK) ────────────────┘
         │         │ station_id (FK) ──────────────┘
         │         │ stop_order                    │
         │         │ arrival_time                  │
         │         │ departure_time                │
         │         │ price                         │
         │         │ is_final                      │
         │         └───────────────────────────────┘
         │
         │ (пользователь создаёт booking)
         └─────────────────────────────────────────┘
```

### Внешние ключи

```sql
ALTER TABLE bookings 
  ADD CONSTRAINT FK_bookings_from_station FOREIGN KEY (from_station_id) REFERENCES stations(id);
ALTER TABLE bookings 
  ADD CONSTRAINT FK_bookings_to_station FOREIGN KEY (to_station_id) REFERENCES stations(id);
ALTER TABLE bookings 
  ADD CONSTRAINT FK_bookings_train FOREIGN KEY (selected_train_id) REFERENCES trains(id);

ALTER TABLE tickets 
  ADD CONSTRAINT FK_tickets_booking FOREIGN KEY (booking_id) REFERENCES bookings(id);
ALTER TABLE tickets 
  ADD CONSTRAINT FK_tickets_train FOREIGN KEY (train_id) REFERENCES trains(id);
ALTER TABLE tickets 
  ADD CONSTRAINT FK_tickets_from_station FOREIGN KEY (from_station_id) REFERENCES stations(id);
ALTER TABLE tickets 
  ADD CONSTRAINT FK_tickets_to_station FOREIGN KEY (to_station_id) REFERENCES stations(id);

ALTER TABLE route_stations 
  ADD CONSTRAINT FK_route_stations_train FOREIGN KEY (train_id) REFERENCES trains(id);
ALTER TABLE route_stations 
  ADD CONSTRAINT FK_route_stations_station FOREIGN KEY (station_id) REFERENCES stations(id);
```

---

## 3. АРХИТЕКТУРА ПРИЛОЖЕНИЯ

### Структура пакетов

```
com.railroad/
├── RailroadApplication.java          # Точка входа Spring Boot
├── entity/                           # JPA-сущности
│   ├── User.java
│   ├── Booking.java
│   ├── Ticket.java
│   ├── Train.java
│   ├── Station.java
│   └── RouteStation.java
├── repository/                       # Data JPA репозитории
│   ├── UserRepository.java
│   ├── BookingRepository.java
│   ├── TicketRepository.java
│   ├── TrainRepository.java
│   ├── StationRepository.java
│   └── RouteStationRepository.java
├── service/                          # Бизнес-логика
│   ├── UserService.java
│   ├── BookingService.java
│   └── TrainService.java
├── controller/                       # MVC контроллеры
│   ├── HomeController.java
│   ├── AuthController.java
│   ├── PassengerController.java
│   ├── AdminController.java
│   ├── AdminUserController.java
│   ├── ProfileController.java
│   ├── TicketController.java
│   └── TrainTimeInfo.java
├── dto/                              # Data Transfer Objects
│   └── UserRegistrationDto.java
├── config/                           # Конфигурация
│   ├── SecurityConfig.java
│   └── DataInitializer.java
└── util/                             # Утилиты
    └── QRCodeGenerator.java
```

### Контроллеры и эндпоинты

| Контроллер | Метод | Путь | Описание |
|------------|-------|------|----------|
| `HomeController` | GET | `/` | Перенаправление на главную в зависимости от роли |
| `HomeController` | GET | `/access-denied` | Страница отказа в доступе |
| `AuthController` | GET | `/auth/login` | Страница входа |
| `AuthController` | POST | `/auth/login` | Обработка формы входа (Spring Security) |
| `AuthController` | GET | `/auth/register` | Страница регистрации |
| `AuthController` | POST | `/auth/register` | Регистрация нового пользователя |
| `AuthController` | GET/POST | `/auth/logout` | Выход из системы |
| `PassengerController` | GET | `/passenger` | Форма поиска рейсов |
| `PassengerController` | POST | `/passenger/search` | Поиск поездов по маршруту |
| `PassengerController` | POST | `/passenger/book` | Создание заявки с выбором поезда |
| `PassengerController` | GET | `/passenger/bookings` | Список заявок пользователя |
| `PassengerController` | GET | `/passenger/bookings/{id}` | Просмотр деталей заявки |
| `PassengerController` | POST | `/passenger/bookings/{id}/pay` | Оплата заявки |
| `PassengerController` | POST | `/passenger/bookings/{id}/cancel` | Отмена заявки |
| `AdminController` | GET | `/admin` | Главная панель администратора |
| `AdminController` | GET | `/admin/trains` | Список поездов |
| `AdminController` | GET | `/admin/trains/new` | Форма создания поезда |
| `AdminController` | POST | `/admin/trains` | Создание поезда |
| `AdminController` | GET | `/admin/trains/{id}` | Детали поезда с маршрутом |
| `AdminController` | POST | `/admin/trains/{id}/route` | Добавление станции в маршрут |
| `AdminController` | POST | `/admin/trains/delete/{id}` | Удаление поезда |
| `AdminController` | GET | `/admin/stations` | Список станций |
| `AdminController` | POST | `/admin/stations` | Создание станции |
| `AdminController` | POST | `/admin/stations/delete/{id}` | Удаление станции |
| `AdminController` | GET | `/admin/bookings` | Все заявки (для админа) |
| `AdminUserController` | GET | `/admin/users` | Список пользователей |
| `AdminUserController` | GET | `/admin/users/new` | Форма создания пользователя |
| `AdminUserController` | POST | `/admin/users` | Создание пользователя |
| `AdminUserController` | POST | `/admin/users/delete/{id}` | Удаление пользователя |
| `AdminUserController` | POST | `/admin/users/enable/{id}` | Активация пользователя |
| `AdminUserController` | POST | `/admin/users/disable/{id}` | Деактивация пользователя |
| `ProfileController` | GET | `/profile` | Страница профиля |
| `ProfileController` | POST | `/profile/update` | Обновление профиля |
| `TicketController` | GET | `/tickets/{id}` | Просмотр билета |
| `TicketController` | GET | `/tickets/{id}/print` | Печать билета (с QR-кодом) |

### Сервисы и их методы

#### UserService

| Метод | Возвращаемый тип | Параметры | Описание |
|-------|-----------------|-----------|----------|
| `loadUserByUsername(String)` | UserDetails | username | Загрузка пользователя для Spring Security |
| `findByUsername(String)` | Optional<User> | username | Поиск по имени пользователя |
| `findByEmail(String)` | Optional<User> | email | Поиск по email |
| `existsByUsername(String)` | boolean | username | Проверка существования по имени |
| `existsByEmail(String)` | boolean | email | Проверка существования по email |
| `registerUserWithPhone(...)` | User | username, password, email, phone | Регистрация обычного пользователя |
| `registerAdminWithPhone(...)` | User | username, password, email, phone | Регистрация администратора |
| `getAllUsers()` | List<User> | - | Получить всех пользователей |
| `deleteUser(Long)` | void | id | Удалить пользователя |
| `enableUser(Long)` | User | id | Активировать пользователя |
| `disableUser(Long)` | User | id | Деактивировать пользователя |
| `updateUser(User)` | User | user | Обновить данные пользователя |

#### BookingService

| Метод | Возвращаемый тип | Параметры | Описание |
|-------|-----------------|-----------|----------|
| `createBooking(...)` | Booking | passengerName, email, fromStationId, toStationId, travelDateTime | Создание заявки без поезда |
| `createBookingWithTrain(...)` | Booking | passengerName, email, fromStationId, toStationId, travelDateTime, trainId | Создание заявки с поездом и расчётом цены |
| `getBookingsByEmail(String)` | List<Booking> | email | Получить заявки по email |
| `getBookingById(Long)` | Booking | id | Получить заявку по ID (с деталями) |
| `findAvailableTrains(...)` | List<Train> | fromStationId, toStationId, dateTime | Поиск доступных поездов |
| `selectTrain(Long, Long)` | Booking | bookingId, trainId | Выбор поезда для заявки |
| `payBooking(Long)` | Booking | bookingId | Оплата заявки, создание билета |
| `cancelBooking(Long)` | Booking | bookingId | Отмена заявки |
| `getAllBookings()` | List<Booking> | - | Все заявки |
| `getTicketById(Long)` | Ticket | id | Получить билет по ID |

#### TrainService

| Метод | Возвращаемый тип | Параметры | Описание |
|-------|-----------------|-----------|----------|
| `getAllTrains()` | List<Train> | - | Все поезда |
| `getTrainById(Long)` | Optional<Train> | id | Поезд по ID |
| `getTrainByNumber(String)` | Optional<Train> | trainNumber | Поезд по номеру |
| `createTrain(String)` | Train | trainNumber | Создать поезд |
| `deleteTrain(Long)` | void | id | Удалить поезд |
| `getAllStations()` | List<Station> | - | Все станции |
| `getStationById(Long)` | Optional<Station> | id | Станция по ID |
| `createStation(String)` | Station | name | Создать станцию |
| `deleteStation(Long)` | void | id | Удалить станцию |
| `addRouteStation(...)` | RouteStation | trainId, stationId, stopOrder, arrivalTime, departureTime, price, isFinal | Добавить станцию в маршрут |
| `getTrainRoute(Long)` | List<RouteStation> | trainId | Маршрут поезда |
| `deleteRouteStation(Long)` | void | id | Удалить станцию из маршрута |

### Repository-интерфейсы и кастомные запросы

#### UserRepository
```java
interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

#### BookingRepository
```java
interface BookingRepository extends JpaRepository<Booking, Long> {
    @EntityGraph(attributePaths = {"fromStation", "toStation", "selectedTrain", "tickets"})
    List<Booking> findByEmailOrderByCreatedAtDesc(String email);
    
    @EntityGraph(attributePaths = {"fromStation", "toStation", "selectedTrain", "tickets"})
    List<Booking> findByStatus(BookingStatus status);
    
    @EntityGraph(attributePaths = {"fromStation", "toStation", "selectedTrain", "tickets"})
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);
}
```

#### RouteStationRepository
```java
interface RouteStationRepository extends JpaRepository<RouteStation, Long> {
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
```

#### TrainRepository
```java
interface TrainRepository extends JpaRepository<Train, Long> {
    Optional<Train> findByTrainNumber(String trainNumber);
    List<Train> findAllByOrderByTrainNumberAsc();
}
```

#### StationRepository
```java
interface StationRepository extends JpaRepository<Station, Long> {
    List<Station> findByNameContainingIgnoreCase(String name);
}
```

#### TicketRepository
```java
interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByBookingId(Long bookingId);
    List<Ticket> findByStatus(TicketStatus status);
}
```

---

## 4. ШАБЛОНЫ ПРОЕКТИРОВАНИЯ GoF И АРХИТЕКТУРНЫЕ ПАТТЕРНЫ

### Реализованные паттерны

#### 4.1 Repository Pattern
**Где применяется:** Все интерфейсы в пакете `repository` (`UserRepository`, `BookingRepository`, и т.д.)

**Роль:** Абстрагирует доступ к данным, инкапсулирует логику работы с БД. Spring Data JPA автоматически генерирует реализации на основе имен методов и аннотаций `@Query`.

**Пример:**
```java
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @EntityGraph(attributePaths = {"fromStation", "toStation", "selectedTrain", "tickets"})
    List<Booking> findByEmailOrderByCreatedAtDesc(String email);
}
```

#### 4.2 Service Layer Pattern
**Где применяется:** Классы `UserService`, `BookingService`, `TrainService`

**Роль:** Инкапсулирует бизнес-логику, обеспечивает транзакционность (`@Transactional`), координирует работу нескольких репозиториев.

**Пример:**
```java
@Service
@Transactional
public class BookingService {
    @Autowired private BookingRepository bookingRepository;
    @Autowired private RouteStationRepository routeStationRepository;
    
    public Booking payBooking(Long bookingId) {
        // Бизнес-логика оплаты с созданием билета
    }
}
```

#### 4.3 MVC (Model-View-Controller)
**Где применяется:** Все контроллеры (`AuthController`, `PassengerController`, и т.д.) + Thymeleaf шаблоны

**Роль:** Разделение ответственности:
- **Model:** Entity-классы и DTO
- **View:** Thymeleaf шаблоны в `src/main/resources/templates/`
- **Controller:** Обработка HTTP-запросов, вызов сервисов, возврат имени view

**Пример:**
```java
@Controller
@RequestMapping("/passenger")
public class PassengerController {
    @GetMapping
    public String searchForm(Model model) {
        model.addAttribute("stations", trainService.getAllStations());
        return "passenger/search";  // View
    }
}
```

#### 4.4 Dependency Injection (DI) / Inversion of Control (IoC)
**Где применяется:** Во всём приложении через аннотации `@Autowired`, конструкторную инъекцию

**Роль:** Управление зависимостями между компонентами Spring-контейнером. Уменьшает связанность, упрощает тестирование.

**Пример:**
```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
}
```

#### 4.5 Strategy Pattern (через Enum)
**Где применяется:** `Booking.BookingStatus`, `Ticket.TicketStatus`, `User.Role`

**Роль:** Определение состояний сущностей с различным поведением в зависимости от состояния. Хотя явного переключения поведения нет, enum задаёт допустимые переходы состояний.

**Пример:**
```java
public enum BookingStatus {
    PENDING,      // Заявка создана
    TRAIN_SELECTED, // Поезд выбран
    PAID,         // Оплачено
    CANCELLED     // Отменено
}
```

#### 4.6 Factory Method (неявно через Spring)
**Где применяется:** Создание бинов Spring (`@Bean` в `SecurityConfig`)

**Роль:** Централизованное создание сложных объектов (PasswordEncoder, SecurityFilterChain).

**Пример:**
```java
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

#### 4.7 Singleton Pattern
**Где применяется:** Все Spring-бины (контроллеры, сервисы, репозитории) по умолчанию являются синглтонами

**Роль:** Гарантия единственного экземпляра компонента в контексте приложения.

#### 4.8 Template Method (через Spring Security)
**Где применяется:** `UserDetailsService.loadUserByUsername()`

**Роль:** Фреймворк вызывает определённый метод в заданный момент времени для загрузки данных пользователя.

---

## 5. ТЕХНОЛОГИЧЕСКИЙ СТЕК

### Версии технологий

| Технология | Версия | Назначение |
|------------|--------|------------|
| Java | 17 | Язык программирования |
| Spring Boot | 3.2.0 | Фреймворк приложения |
| Spring Framework | 6.1.1 | Базовый фреймворк |
| Hibernate ORM | 6.3.1.Final | JPA-провайдер |
| Spring Data JPA | 3.2.0 | Абстракция доступа к данным |
| H2 Database | 2.2.224 (в составе Spring Boot) | Встроенная СУБД |
| Thymeleaf | 3.1.x (в составе Spring Boot) | Шаблонизатор HTML |
| Spring Security | 6.2.0 (в составе Spring Boot) | Безопасность |
| Jakarta Validation | 3.x | Валидация форм |
| HikariCP | 5.x (в составе Spring Boot) | Пул соединений |
| Tomcat | 10.1.16 (embedded) | Веб-сервер |
| Maven | 3.x | Система сборки |
| ZXing | 3.5.1 | Генерация QR-кодов |

### Ключевые настройки application.properties

```properties
# Application
spring.application.name=Railroad
server.port=8080
server.servlet.context-path=/Railroad

# Database (H2 in-memory)
spring.datasource.url=jdbc:h2:mem:railroad_db
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Thymeleaf
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.mode=HTML
spring.thymeleaf.encoding=UTF-8

# Logging
logging.level.com.railroad=DEBUG
logging.level.org.springframework.web=INFO
logging.level.org.hibernate.SQL=DEBUG
```

### Пояснения к настройкам

- **ddl-auto=create-drop**: База данных пересоздаётся при каждом запуске (для разработки)
- **h2:mem:railroad_db**: In-memory база данных (данные теряются после остановки)
- **thymeleaf.cache=false**: Отключён кэш шаблонов для горячей перезагрузки
- **context-path=/Railroad**: Приложение доступно по пути `/Railroad`

---

## 6. БИЗНЕС-ЛОГИКА И КЛЮЧЕВЫЕ АЛГОРИТМЫ

### 6.1 Поиск доступных поездов по маршруту

**Логика:**
1. Получение объектов станций отправления и назначения по ID
2. Выполнение JPQL-запроса для поиска поездов, у которых обе станции есть в маршруте
3. Проверка порядка остановок: станция отправления должна идти раньше станции назначения

**JPQL-запрос (RouteStationRepository):**
```java
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
```

**Метод сервиса:**
```java
public List<Train> findAvailableTrains(Long fromStationId, Long toStationId, LocalDateTime dateTime) {
    Station fromStation = stationRepository.findById(fromStationId)
            .orElseThrow(() -> new RuntimeException("Станция отправления не найдена"));
    Station toStation = stationRepository.findById(toStationId)
            .orElseThrow(() -> new RuntimeException("Станция назначения не найдена"));
    
    return routeStationRepository.findTrainsByRoute(fromStation, toStation);
}
```

### 6.2 Расчёт стоимости билета

**Алгоритм:**
1. Получение всех станций маршрута поезда, отсортированных по `stopOrder`
2. Нахождение позиций станций отправления и назначения в маршруте
3. Валидация: станция отправления должна идти раньше станции назначения
4. Суммирование цен всех участков маршрута между станциями отправления и назначения

**Реализация (BookingService.calculateTicketPrice):**
```java
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
```

**Пример:**
Если маршрут: Москва (0₽) → Тверь (15.50₽) → Санкт-Петербург (32.00₽)
- Билет Москва → Тверь: 15.50₽
- Билет Москва → СПб: 15.50 + 32.00 = 47.50₽
- Билет Тверь → СПб: 32.00₽

### 6.3 Жизненный цикл заявки (State Machine)

**Состояния (BookingStatus):**
```
PENDING → TRAIN_SELECTED → PAID
                        ↘ CANCELLED
```

**Переходы:**
1. **PENDING** (начальное): Заявка создана, пассажир не выбрал поезд
   - Допустимый переход: → TRAIN_SELECTED (при выборе поезда)
   
2. **TRAIN_SELECTED**: Поезд выбран, цена рассчитана, ожидает оплаты
   - Допустимые переходы: → PAID (при оплате), → CANCELLED (при отмене)
   
3. **PAID**: Оплата прошла успешно, создан билет
   - Переходы запрещены (финальное состояние)
   
4. **CANCELLED**: Заявка отменена
   - Переходы запрещены (финальное состояние)

**Валидация переходов (BookingService.payBooking):**
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
    // Создание билета...
}
```

### 6.4 Генерация номера места и создание билета

**Генерация номера места:**
```java
private String generateSeatNumber() {
    int car = (int) (Math.random() * 10) + 1;    // Вагон: 1-10
    int seat = (int) (Math.random() * 54) + 1;   // Место: 1-54
    return car + "-" + seat;                     // Формат: "5-42"
}
```

**Создание билета при оплате:**
```java
// В методе payBooking()
Ticket ticket = new Ticket();
ticket.setBooking(booking);
ticket.setTrain(booking.getSelectedTrain());
ticket.setFromStation(booking.getFromStation());
ticket.setToStation(booking.getToStation());
ticket.setPrice(booking.getTotalPrice());
ticket.setStatus(Ticket.TicketStatus.PAID);
ticket.setSeatNumber(generateSeatNumber());
ticket.setTicketCode(generateTicketCode(booking));

ticketRepository.save(ticket);
```

**Генерация уникального кода билета:**
```java
private String generateTicketCode(Booking booking) {
    String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    String randomPart = String.format("%04d", (int)(Math.random() * 10000));
    return "TICKET-" + timestamp + "-" + randomPart;
}
```
Формат: `TICKET-20260418153045-7823`

### 6.5 Работа с сессиями и безопасность

**Spring Security конфигурация:**
- Форм-аутентификация с собственной страницей входа
- CSRF отключён (для упрощения разработки)
- Ролевая модель: USER, ADMIN
- URL `/auth/**` доступны всем, `/admin/**` только ADMIN, `/passenger/**` USER или ADMIN

**Фильтры безопасности (порядок выполнения):**
1. DisableEncodeUrlFilter
2. WebAsyncManagerIntegrationFilter
3. SecurityContextHolderFilter
4. HeaderWriterFilter
5. CorsFilter
6. LogoutFilter
7. UsernamePasswordAuthenticationFilter
8. RequestCacheAwareFilter
9. SecurityContextHolderAwareRequestFilter
10. AnonymousAuthenticationFilter
11. ExceptionTranslationFilter
12. AuthorizationFilter

### 6.6 Валидация форм

**На уровне DTO (UserRegistrationDto):**
```java
public class UserRegistrationDto {
    @NotBlank(message = "Имя пользователя обязательно")
    @Size(min = 3, max = 50, message = "Имя пользователя должно быть от 3 до 50 символов")
    private String username;
    
    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Пароль должен быть не менее 6 символов")
    private String password;
    
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный email")
    private String email;
    
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Номер телефона должен содержать от 10 до 15 цифр")
    private String phone;
}
```

**Обработка в контроллере:**
```java
@PostMapping("/register")
public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto userDto,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
    if (result.hasErrors()) {
        return "auth/register";  // Возврат на форму с ошибками
    }
    // Регистрация...
}
```

### 6.7 Обработка ошибок

- **Валидация:** Ошибки отображаются рядом с полями формы через Thymeleaf + `${#fields.errors()}`
- **Бизнес-ошибки:** Исключения `RuntimeException` с сообщениями, которые выводятся через `redirectAttributes.addFlashAttribute("error", ...)`
- **403 Forbidden:** Страница `/access-denied`
- **404 Not Found:** Стандартная страница ошибки Tomcat

---

## 7. ПРИМЕРЫ КОДА

### 7.1 Entity-класс с полными JPA-аннотациями

#### Booking.java
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
        PENDING,
        TRAIN_SELECTED,
        PAID,
        CANCELLED
    }

    // Геттеры и сеттеры опущены для краткости
}
```

### 7.2 Контроллер с обработкой HTTP-запросов

#### PassengerController.java (фрагмент)
```java
package com.railroad.controller;

import com.railroad.entity.Booking;
import com.railroad.entity.User;
import com.railroad.service.BookingService;
import com.railroad.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/passenger")
public class PassengerController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

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
                    passengerName,
                    email,
                    fromStationId,
                    toStationId,
                    travelDateTime,
                    trainId
            );
            
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
}
```

### 7.3 Сервисный метод с бизнес-логикой

#### BookingService.payBooking()
```java
package com.railroad.service;

import com.railroad.entity.Booking;
import com.railroad.entity.Ticket;
import com.railroad.repository.BookingRepository;
import com.railroad.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    public Booking payBooking(Long bookingId) {
        // 1. Загрузка заявки с деталями
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        // 2. Проверка статуса (можно оплачивать только TRAIN_SELECTED)
        if (booking.getStatus() != Booking.BookingStatus.TRAIN_SELECTED) {
            throw new RuntimeException("Нельзя оплатить заявку в текущем статусе");
        }

        // 3. Проверка, что билет ещё не создан
        if (booking.getTickets() != null && !booking.getTickets().isEmpty()) {
            throw new RuntimeException("Билет уже создан для этой заявки");
        }

        // 4. Изменение статуса
        booking.setStatus(Booking.BookingStatus.PAID);
        booking = bookingRepository.save(booking);

        // 5. Создание билета
        Ticket ticket = new Ticket();
        ticket.setBooking(booking);
        ticket.setTrain(booking.getSelectedTrain());
        ticket.setFromStation(booking.getFromStation());
        ticket.setToStation(booking.getToStation());
        ticket.setPrice(booking.getTotalPrice());
        ticket.setStatus(Ticket.TicketStatus.PAID);
        ticket.setSeatNumber(generateSeatNumber());
        ticket.setTicketCode(generateTicketCode(booking));

        ticketRepository.save(ticket);
        
        // 6. Добавление билета в коллекцию заявки
        if (booking.getTickets() == null) {
            booking.setTickets(new java.util.ArrayList<>());
        }
        booking.getTickets().add(ticket);

        return booking;
    }

    private String generateSeatNumber() {
        int car = (int) (Math.random() * 10) + 1;
        int seat = (int) (Math.random() * 54) + 1;
        return car + "-" + seat;
    }

    private String generateTicketCode(Booking booking) {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%04d", (int)(Math.random() * 10000));
        return "TICKET-" + timestamp + "-" + randomPart;
    }
}
```

### 7.4 Конфигурация Spring Security и БД

#### SecurityConfig.java
```java
package com.railroad.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
            .authenticationProvider(authenticationProvider)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/auth/**", "/css/**", "/js/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/passenger/**", "/profile").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/access-denied").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .defaultSuccessUrl("/passenger/bookings", true)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            )
            .csrf(csrf -> csrf.disable());

        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
```

#### Подключение к БД (DataInitializer.java)
```java
package com.railroad.config;

import com.railroad.entity.RouteStation;
import com.railroad.entity.Station;
import com.railroad.entity.Train;
import com.railroad.entity.User;
import com.railroad.repository.*;
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
        // Создание тестовых пользователей с белорусскими номерами
        createUserIfNotExists("admin", "admin", "admin@railroad.by", "+375291000001", User.Role.ADMIN);
        createUserIfNotExists("user", "user", "user@railroad.by", "+375291000002", User.Role.USER);

        // Создание станций
        Station moscow = createStation("Москва");
        Station minsk = createStation("Минск");
        Station spb = createStation("Санкт-Петербург");
        // ... другие станции

        // Создание поездов и маршрутов
        Train train1 = createTrain("001А");
        addRouteStation(train1, moscow, 1,
                LocalDateTime.now().plusDays(1).withHour(8),
                LocalDateTime.now().plusDays(1).withHour(8).plusMinutes(30),
                new BigDecimal("0"), false);
        // ... остальные остановки
    }

    private void createUserIfNotExists(String username, String password, String email, String phone, User.Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(new BCryptPasswordEncoder().encode(password));
            user.setEmail(email);
            user.setPhone(phone);
            user.setRole(role);
            user.setEnabled(true);
            userRepository.save(user);
        }
    }
}
```

---

## 8. ДИАГРАММЫ (текстовое представление)

### 8.1 UML-диаграмма классов

```
┌─────────────────────────────────────────────────────────────────────┐
│                            User                                     │
├─────────────────────────────────────────────────────────────────────┤
│ - id: Long                                                          │
│ - username: String                                                  │
│ - password: String                                                  │
│ - email: String                                                     │
│ - phone: String                                                     │
│ - enabled: boolean                                                  │
│ - role: Role                                                        │
├─────────────────────────────────────────────────────────────────────┤
│ + getId(): Long                                                     │
│ + getUsername(): String                                             │
│ + getPassword(): String                                             │
│ + getEmail(): String                                                │
│ + getPhone(): String                                                │
│ + isEnabled(): boolean                                              │
│ + getRole(): Role                                                   │
└─────────────────────────────────────────────────────────────────────┘
                              ▲
                              │
                              │ (создаёт)
                              │
┌─────────────────────────────────────────────────────────────────────┐
│                           Booking                                   │
├─────────────────────────────────────────────────────────────────────┤
│ - id: Long                                                          │
│ - passengerName: String                                             │
│ - email: String                                                     │
│ - fromStation: Station                                              │
│ - toStation: Station                                                │
│ - travelDateTime: LocalDateTime                                     │
│ - selectedTrain: Train                                              │
│ - totalPrice: BigDecimal                                            │
│ - status: BookingStatus                                             │
│ - createdAt: LocalDateTime                                          │
│ - tickets: List<Ticket>                                             │
├─────────────────────────────────────────────────────────────────────┤
│ + createBookingWithTrain(...): Booking                              │
│ + payBooking(): Booking                                             │
│ + cancelBooking(): Booking                                          │
└─────────────────────────────────────────────────────────────────────┘
         │                    │                    │
         │ 1                  │ 1                  │ 1..*
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│    Station      │  │     Train       │  │     Ticket      │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│ - id: Long      │  │ - id: Long      │  │ - id: Long      │
│ - name: String  │  │ - trainNumber   │  │ - ticketCode    │
│ - routeStations │  │ - routeStations │  │ - booking       │
├─────────────────┤  │ - tickets       │  │ - train         │
│ + getName()     │  │ - trainNumber   │  │ - fromStation   │
└─────────────────┘  │ - getRoute()    │  │ - toStation     │
         ▲           └─────────────────┘  │ - seatNumber    │
         │                                │ - price         │
         │          ┌─────────────────┐   │ - status        │
         │          │  RouteStation   │   └─────────────────┘
         └─────────►│ - id: Long      │           ▲
                    │ - train: Train  │           │
                    │ - station       │           │ (владелец)
                    │ - stopOrder     │           │
                    │ - arrivalTime   │           │
                    │ - departureTime │           │
                    │ - price         │           │
                    │ - isFinal       │           │
                    └─────────────────┘           │
                                                  │
                    ╔═════════════════════════════╝
                    ║  Примечание:
                    ║  Ticket связан с Booking
                    ║  через booking_id
                    ╚══════════════════════════════
```

### 8.2 ER-диаграмма базы данных

```
╔════════════════════════════════════════════════════════════════════╗
║                         ER-DIAGRAM                                 ║
╠════════════════════════════════════════════════════════════════════╣

  ┌──────────────┐
  │    users     │
  ├──────────────┤
  │ PK id        │
  │ UK username  │
  │ UK email     │
  │    password  │
  │    phone     │
  │    enabled   │
  │    role      │
  └──────────────┘

  ┌──────────────────────────────────────────────────────────────────┐
  │                         bookings                                  │
  ├──────────────────────────────────────────────────────────────────┤
  │ PK id                         │                                  │
  │    passenger_name             │◄───────┐                         │
  │    email                      │        │                         │
  │ FK from_station_id ───────────┼───────►│  ┌──────────────┐       │
  │ FK to_station_id ─────────────┼───────►│  │   stations   │       │
  │    travel_datetime            │        │  ├──────────────┤       │
  │ FK selected_train_id ─────────┼───────►│  │ PK id        │       │
  │    total_price                │        │  │    name      │       │
  │    status                     │        │  └──────────────┘       │
  │    created_at                 │        │                         │
  └──────────────────────────────────────────────────────────────────┘
         │                                    ▲
         │ 1                                  │ N
         │                                    │
         ▼                                    │
  ┌──────────────────────────────────────────────────────────────────┐
  │                          tickets                                 │
  ├──────────────────────────────────────────────────────────────────┤
  │ PK id                         │                                  │
  │ UK ticket_code                  │                                  │
  │ FK booking_id ──────────────────┘                                  │
  │ FK train_id ──────────────────────────────────┐                   │
  │ FK from_station_id ───────────────────────────┼───┐               │
  │ FK to_station_id ─────────────────────────────┼───┼───┐           │
  │    seat_number                │               │   │   │           │
  │    price                      │               │   │   │           │
  │    status                     │               │   │   │           │
  └──────────────────────────────────────────────────────────────────┘
         │                               │       │   │   │
         │                               │       │   │   │
         ▼                               ▼       ▼   ▼   ▼
  ┌──────────────┐              ┌──────────────┐  ┌──────────────┐
  │    trains    │              │   stations   │  │   stations   │
  ├──────────────┤              ├──────────────┤  ├──────────────┤
  │ PK id        │              │ PK id        │  │ PK id        │
  │ UK train_num │              │    name      │  │    name      │
  └──────────────┘              └──────────────┘  └──────────────┘
         ▲
         │ N
         │
  ┌──────────────────────────────────────────────────────────────────┐
  │                      route_stations                              │
  ├──────────────────────────────────────────────────────────────────┤
  │ PK id                        │                                   │
  │ FK train_id ─────────────────┘                                   │
  │ FK station_id ─────────────────────────────► [stations]          │
  │    stop_order                │                                   │
  │    arrival_time              │                                   │
  │    departure_time            │                                   │
  │    price                     │                                   │
  │    is_final                  │                                   │
  └──────────────────────────────────────────────────────────────────┘

LEGEND:
  PK = Primary Key
  UK = Unique Key
  FK = Foreign Key
  ───► = Reference (Many-to-One)
```

### 8.3 Диаграмма состояний (State Machine) для Booking

```
┌─────────────────────────────────────────────────────────────────────┐
│                    STATE MACHINE: Booking                           │
└─────────────────────────────────────────────────────────────────────┘

     ┌─────────────┐
     │   [START]   │
     └──────┬──────┘
            │
            │ createBooking()
            ▼
     ┌─────────────┐
     │   PENDING   │◄────────────────────────┐
     └──────┬──────┘                         │
            │                                │
            │ selectTrain()                  │ cancel()
            ▼                                │
     ┌─────────────────┐                     │
     │ TRAIN_SELECTED  │─────────────────────┘
     └──────┬──────────┘
            │
      ┌─────┴─────┐
      │           │
      │ pay()     │ cancel()
      │           │
      ▼           ▼
┌──────────┐  ┌───────────┐
│   PAID   │  │ CANCELLED │
└──────────┘  └───────────┘
     │              │
     │ (final)      │ (final)
     ▼              ▼
┌─────────────────────────┐
│      [END]              │
└─────────────────────────┘


STATE TRANSITIONS TABLE:
┌──────────────────┬──────────────┬─────────────────┬────────────────┐
│ Current State    │ Action       │ New State       │ Conditions     │
├──────────────────┼──────────────┼─────────────────┼────────────────┤
│ PENDING          │ selectTrain  │ TRAIN_SELECTED  │ train exists   │
│ TRAIN_SELECTED   │ pay          │ PAID            │ status check   │
│ TRAIN_SELECTED   │ cancel       │ CANCELLED       │ -              │
│ PAID             │ -            │ -               │ (final)        │
│ CANCELLED        │ -            │ -               │ (final)        │
└──────────────────┴──────────────┴─────────────────┴────────────────┘
```

---

## TODO и известные ограничения

1. **База данных H2 in-memory**: Данные теряются после перезапуска приложения. Для продакшена требуется PostgreSQL/MySQL.
2. **Генерация мест**: Нет проверки уникальности номера места в пределах поезда.
3. **QR-код**: Генерируется на клиенте (в браузере) при просмотре билета, не хранится в БД.
4. **Email-уведомления**: Не реализованы (планируется интеграция с почтовым сервисом).
5. **Платёжный шлюз**: Оплата симулирована, реальная интеграция отсутствует.
6. **Валидация дат**: Частичная проверка на прошлое время, нет проверки на слишком далёкое будущее.
7. **Конкурентность**: Нет блокировок при одновременной покупке одного места.

---

## Заключение

Веб-приложение "Система железнодорожная касса" реализует полный цикл работы с заявками на покупку билетов: от поиска рейсов до генерации билета с QR-кодом. Архитектура построена на принципах многослойности (MVC + Service + Repository), что обеспечивает модульность и тестируемость кода. Использование Spring Boot позволило быстро развернуть приложение с минимальной конфигурацией, а Spring Security обеспечило ролевую модель доступа.

**Ключевые особенности реализации:**
- Автоматический расчёт стоимости на основе тарифных участков маршрута
- Уникальные коды билетов для генерации QR-кодов
- Жизненный цикл заявки с валидацией переходов состояний
- Белорусский формат номеров телефонов (+375) для пользователей
- Админ-панель для управления поездами, станциями и пользователями
