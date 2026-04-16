package com.railroad.controller;

import com.railroad.entity.Ticket;
import com.railroad.service.BookingService;
import com.railroad.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Objects;

/**
 * Контроллер для работы с билетами
 * Обрабатывает запросы на просмотр и печать билетов
 */
@Controller
@RequestMapping("/tickets")
public class TicketController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    /**
     * Просмотр билета по ID
     * @param ticketId ID билета
     * @param model модель для передачи данных
     * @param userDetails данные авторизованного пользователя
     * @return страница просмотра билета
     */
    @GetMapping("/{id}")
    public String viewTicket(@PathVariable Long id, Model model,
                             @AuthenticationPrincipal UserDetails userDetails) {
        // Получаем заявку через сервис (там есть доступ к билетам)
        // Нам нужно найти билет по ID - добавим метод в сервис
        Ticket ticket = bookingService.getTicketById(id);

        if (ticket == null || ticket.getBooking() == null) {
            return "redirect:/passenger/bookings";
        }

        // Проверка прав доступа: только владелец билета или администратор
        if (userDetails != null) {
            var user = userService.findByUsername(userDetails.getUsername()).orElse(null);
            if (user != null && user.getRole() != com.railroad.entity.User.Role.ADMIN) {
                if (!Objects.equals(ticket.getBooking().getEmail(), user.getEmail())) {
                    return "redirect:/passenger/bookings";
                }
            }
        }

        model.addAttribute("ticket", ticket);
        model.addAttribute("booking", ticket.getBooking());

        return "tickets/ticket-view";
    }

    /**
     * Печать билета (открывает ту же страницу в режиме печати)
     * @param ticketId ID билета
     * @param model модель для передачи данных
     * @param userDetails данные авторизованного пользователя
     * @return страница печати билета
     */
    @GetMapping("/{id}/print")
    public String printTicket(@PathVariable Long id, Model model,
                              @AuthenticationPrincipal UserDetails userDetails) {
        Ticket ticket = bookingService.getTicketById(id);

        if (ticket == null || ticket.getBooking() == null) {
            return "redirect:/passenger/bookings";
        }

        // Проверка прав доступа
        if (userDetails != null) {
            var user = userService.findByUsername(userDetails.getUsername()).orElse(null);
            if (user != null && user.getRole() != com.railroad.entity.User.Role.ADMIN) {
                if (!Objects.equals(ticket.getBooking().getEmail(), user.getEmail())) {
                    return "redirect:/passenger/bookings";
                }
            }
        }

        model.addAttribute("ticket", ticket);
        model.addAttribute("booking", ticket.getBooking());

        return "tickets/ticket-print";
    }
}
