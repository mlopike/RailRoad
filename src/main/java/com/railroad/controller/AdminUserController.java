package com.railroad.controller;

import com.railroad.dto.UserRegistrationDto;
import com.railroad.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users";
    }

    @GetMapping("/new")
    public String newUserForm(Model model) {
        model.addAttribute("userDto", new UserRegistrationDto());
        return "admin/user-form";
    }

    @PostMapping
    public String createUser(@Valid @ModelAttribute("userDto") UserRegistrationDto userDto,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/user-form";
        }
        
        try {
            if ("ADMIN".equals(userDto.getRole())) {
                userService.registerAdmin(userDto.getUsername(), userDto.getPassword(), userDto.getEmail());
            } else {
                userService.registerUser(userDto.getUsername(), userDto.getPassword(), userDto.getEmail());
            }
            redirectAttributes.addFlashAttribute("success", "Пользователь создан успешно!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Пользователь удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/enable/{id}")
    public String enableUser(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        userService.enableUser(id);
        redirectAttributes.addFlashAttribute("success", "Пользователь активирован");
        return "redirect:/admin/users";
    }

    @PostMapping("/disable/{id}")
    public String disableUser(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        userService.disableUser(id);
        redirectAttributes.addFlashAttribute("success", "Пользователь деактивирован");
        return "redirect:/admin/users";
    }
}
