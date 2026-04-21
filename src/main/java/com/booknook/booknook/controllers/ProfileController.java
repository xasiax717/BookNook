package com.booknook.booknook.controllers;

import com.booknook.booknook.entities.User;
import com.booknook.booknook.entities.UserBook;
import com.booknook.booknook.repositories.UserBookRepository;
import com.booknook.booknook.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBookRepository userBookRepository;

    @GetMapping
    public String showProfile(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        // 1. Znajdź zalogowanego użytkownika
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Pobierz jego książki
        List<UserBook> userBooks = userBookRepository.findByUser(user);

        // 3. Oblicz statystyki
        long totalBooks = userBooks.size();

        long completedCount = userBooks.stream()
                .filter(ub -> "COMPLETED".equals(ub.getStatus()))
                .count();

        long readingCount = userBooks.stream()
                .filter(ub -> "READING".equals(ub.getStatus()))
                .count();

        long toReadCount = userBooks.stream()
                .filter(ub -> "TO_READ".equals(ub.getStatus()))
                .count();

        // SUMA STRON (tylko dla przeczytanych książek)
        int totalPagesRead = userBooks.stream()
                .filter(ub -> "COMPLETED".equals(ub.getStatus()))
                .filter(ub -> ub.getBook().getNumberOfPages() != null)
                .mapToInt(ub -> ub.getBook().getNumberOfPages())
                .sum();

        // 4. Przekaż wszystko do HTMLa
        model.addAttribute("username", user.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("readingCount", readingCount);
        model.addAttribute("toReadCount", toReadCount);
        model.addAttribute("totalPagesRead", totalPagesRead);

        return "profile";
    }
}