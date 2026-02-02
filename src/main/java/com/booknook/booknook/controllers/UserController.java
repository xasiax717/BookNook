package com.booknook.booknook.controllers;

import com.booknook.booknook.entities.User;
import com.booknook.booknook.repositories.UserRepository;

import com.booknook.booknook.services.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String showHomePage() {
        return "home";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
//        System.out.println("controller");
        userService.saveUser(user);
//        System.out.println("User registered successfully: " + user.getUsername());

//        System.out.println("Registered User: " + user.getUsername());
        model.addAttribute("message", "Registration successful for user: " + user.getUsername());
        return "redirect:/login"; // Redirect to login page after registration
    }

}