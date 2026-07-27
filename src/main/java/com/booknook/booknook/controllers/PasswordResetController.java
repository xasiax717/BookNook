package com.booknook.booknook.controllers;

import com.booknook.booknook.services.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email,
                                        HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        passwordResetService.sendResetLink(email, baseUrl);
        redirectAttributes.addFlashAttribute("message",
                "Jeśli podany email istnieje w systemie, wyślemy link do resetowania hasła.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPassword(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                       @RequestParam String password,
                                       RedirectAttributes redirectAttributes) {
        boolean success = passwordResetService.resetPassword(token, password);
        if (success) {
            redirectAttributes.addFlashAttribute("message", "Hasło zostało zmienione. Możesz się zalogować.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("error", "Link wygasł lub jest nieprawidłowy.");
            return "redirect:/forgot-password";
        }
    }
}