package com.booknook.booknook.controllers;

import com.booknook.booknook.entities.User;
import com.booknook.booknook.repositories.UserRepository;

import com.booknook.booknook.services.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@CrossOrigin(origins = "http://localhost:8080")
@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/home")
    public String showHomePage(Model model, Principal principal) {
        if (principal != null) {
            model.addAttribute("username", principal.getName());
        }
        return "home";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               BindingResult bindingResult, // Przechowuje błędy walidacji
                               @RequestParam("confirmPassword") String confirmPassword,
                               Model model) {

        // 1. Walidacja techniczna
        if (bindingResult.hasErrors()) {
            return "register";
        }

        // 2. Walidacja logiczna haseł
        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Hasła nie są identyczne!");
            return "register";
        }

        try {
            // 3. Próba zapisu przez serwis
            userService.save(user);
        } catch (RuntimeException e) {
            // Obsługa błędu, np. gdy login jest już zajęty
            model.addAttribute("error", "Błąd rejestracji: " + e.getMessage());
            return "register";
        }

        return "redirect:/login?success";
    }
}