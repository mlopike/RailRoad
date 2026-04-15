package com.railroad.controller;

import com.railroad.entity.Booking;
import com.railroad.entity.RouteStation;
import com.railroad.entity.Station;
import com.railroad.entity.Train;
import com.railroad.entity.User;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Контроллер для работы с пассажиром
 * Обрабатывает запросы на поиск рейсов, создание заявок, оплату
 */
@Controller
@RequestMapping("/passenger")
public class PassengerController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private TrainService trainService;

    @Autowired
    private UserService userService;

    /**
     * Главная страница пассажира - форма поиска рейсов
     * @param model модель для передачи данных в представление
     * @return страница поиска рейсов
     */
    @GetMapping
    public String searchForm(Model model) {
        model.addAttribute("stations", trainService.getAllStations());
        return "passenger/search";
    }

    /**
     * Поиск доступных поездов по маршруту
     * @param fromStationId ID станции отправления
     * @param toStationId ID станции назначения
     * @param model модель для передачи данных
     * @param userDetails данные авторизованного пользователя
     * @return страница с результатами поиска
     */
    @PostMapping("/search")
    public String searchTrains(@RequestParam Long fromStationId,
                               @RequestParam Long toStationId,
                               Model model,
                               @AuthenticationPrincipal UserDetails userDetails) {
        Station fromStation = trainService.getStationById(fromStationId).orElse(null);
        Station toStation = trainService.getStationById(toStationId).orElse(null);
        
        List<Train> availableTrains = bookingService.findAvailableTrains(
                fromStationId, toStationId, null);
        
        // Получаем информацию о времени для каждого поезда
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
        model.addAttribute("fromStationId", fromStationId);
        model.addAttribute("toStationId", toStationId);
        model.addAttribute("fromStation", fromStation);
        model.addAttribute("toStation", toStation);
        model.addAttribute("trainTimes", trainTimes);
        
        if (availableTrains.isEmpty()) {
            model.addAttribute("noTrains", true);
        }
        
        return "passenger/search-results";
    }

    /**
     * Создание заявки при выборе поезда
     * Пассажир выбирает поезд из списка и вводит ФИО
     * Заявка сразу создаётся со статусом TRAIN_SELECTED и рассчитанной ценой
     * @param fromStationId ID станции отправления
     * @param toStationId ID станции назначения
     * @param trainId ID выбранного поезда
     * @param passengerName ФИО пассажира
     * @param userDetails данные авторизованного пользователя
     * @param redirectAttributes атрибуты для перенаправления
     * @return перенаправление на страницу заявки
     */
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
            
            // Используем текущую дату для поездки
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

    @GetMapping("/bookings")
    @Transactional(readOnly = true)
    public String listBookings(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
            if (user != null && user.getRole() == User.Role.ADMIN) {
                model.addAttribute("bookings", bookingService.getAllBookings());
            } else if (user != null) {
                // Используем email пользователя для поиска заявок
                model.addAttribute("bookings", bookingService.getBookingsByEmail(user.getEmail()));
            } else {
                model.addAttribute("bookings", bookingService.getBookingsByEmail(userDetails.getUsername()));
            }
        } else {
            model.addAttribute("bookings", bookingService.getAllBookings());
        }
        return "passenger/bookings";
    }

    @GetMapping("/bookings/{id}")
    public String viewBooking(@PathVariable Long id, Model model,
                              @AuthenticationPrincipal UserDetails userDetails) {
        Booking booking = bookingService.getBookingById(id);

        // Проверка прав доступа: только владелец заявки или администратор
        if (userDetails != null) {
            User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
            if (user != null && user.getRole() != User.Role.ADMIN) {
                if (!Objects.equals(booking.getEmail(), user.getEmail())) {
                    return "redirect:/passenger/bookings";
                }
            }
        }

        model.addAttribute("booking", booking);

        return "passenger/booking-detail";
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
