package com.railroad.controller;

import com.railroad.entity.RouteStation;
import com.railroad.entity.Station;
import com.railroad.entity.Train;
import com.railroad.service.BookingService;
import com.railroad.service.TrainService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private TrainService trainService;

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public String adminPanel(Model model) {
        model.addAttribute("trains", trainService.getAllTrains());
        model.addAttribute("stations", trainService.getAllStations());
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "admin/admin-panel";
    }

    @GetMapping("/trains")
    public String listTrains(Model model) {
        model.addAttribute("trains", trainService.getAllTrains());
        return "admin/trains";
    }

    @GetMapping("/trains/new")
    public String newTrainForm(Model model) {
        model.addAttribute("train", new Train());
        return "admin/train-form";
    }

    @PostMapping("/trains")
    public String createTrain(@Valid @ModelAttribute("train") Train train,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/train-form";
        }

        try {
            trainService.createTrain(train.getTrainNumber());
            redirectAttributes.addFlashAttribute("success", "Поезд создан успешно!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Поезд с таким номером уже существует");
            return "redirect:/admin/trains/new";
        }

        return "redirect:/admin/trains";
    }

    @GetMapping("/trains/{id}")
    public String viewTrain(@PathVariable Long id, Model model) {
        Train train = trainService.getTrainById(id)
                .orElseThrow(() -> new RuntimeException("Поезд не найден"));
        model.addAttribute("train", train);
        model.addAttribute("routeStations", trainService.getTrainRoute(id));
        model.addAttribute("stations", trainService.getAllStations());
        return "admin/train-detail";
    }

    @PostMapping("/trains/{id}/route")
    public String addRouteStation(@PathVariable Long id,
                                  @RequestParam Long stationId,
                                  @RequestParam Integer stopOrder,
                                  @RequestParam String arrivalTime,
                                  @RequestParam String departureTime,
                                  @RequestParam BigDecimal price,
                                  @RequestParam(defaultValue = "false") boolean isFinal,
                                  RedirectAttributes redirectAttributes) {
        try {
            LocalDateTime arrival = LocalDateTime.parse(arrivalTime);
            LocalDateTime departure = LocalDateTime.parse(departureTime);
            
            // Дополнительная валидация в контроллере
            if (departure.isBefore(arrival)) {
                throw new RuntimeException("Время отправления не может быть раньше времени прибытия");
            }
            
            trainService.addRouteStation(id, stationId, stopOrder, arrival, departure, price, isFinal);
            redirectAttributes.addFlashAttribute("success", "Станция добавлена в маршрут");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Некорректный формат даты/времени. Используйте формат YYYY-MM-DDTHH:MM");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/trains/" + id;
    }

    @PostMapping("/trains/delete/{id}")
    public String deleteTrain(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        trainService.deleteTrain(id);
        redirectAttributes.addFlashAttribute("success", "Поезд удален");
        return "redirect:/admin/trains";
    }

    @GetMapping("/stations")
    public String listStations(Model model) {
        model.addAttribute("stations", trainService.getAllStations());
        return "admin/stations";
    }

    @PostMapping("/stations")
    public String createStation(@RequestParam String name,
                                RedirectAttributes redirectAttributes) {
        trainService.createStation(name);
        redirectAttributes.addFlashAttribute("success", "Станция создана успешно!");
        return "redirect:/admin/stations";
    }

    @PostMapping("/stations/delete/{id}")
    public String deleteStation(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        trainService.deleteStation(id);
        redirectAttributes.addFlashAttribute("success", "Станция удалена");
        return "redirect:/admin/stations";
    }

    @GetMapping("/bookings")
    public String listBookings(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "admin/bookings";
    }
}
