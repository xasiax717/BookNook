package com.booknook.booknook.controllers;

import com.booknook.booknook.entities.User;
import com.booknook.booknook.entities.UserBook;
import com.booknook.booknook.repositories.UserRepository;
import com.booknook.booknook.repositories.UserBookRepository;

import com.booknook.booknook.services.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@CrossOrigin(origins = "http://localhost:8080")
@Controller
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserBookRepository userBookRepository;
    @Autowired
    private UserRepository userRepository;

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

    @GetMapping("/statistics")
    public String showStatistics(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        try {
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<UserBook> allBooks = userBookRepository.findByUser(user);

            // 1. Liczniki (bezpieczne, bo size() zawsze zwróci co najmniej 0)
            long total = allBooks.size();
            long completed = allBooks.stream().filter(b -> "COMPLETED".equals(b.getStatus())).count();
            long reading = allBooks.stream().filter(b -> "READING".equals(b.getStatus())).count();
            long toRead = allBooks.stream().filter(b -> "TO_READ".equals(b.getStatus())).count();

            // 2. Bezpieczna suma stron (Ulepszona odporność na statusy i null-e)
            // 2. Pobieranie stron bezpośrednio z powiązanych obiektów Book
            int totalPages = allBooks.stream()
                    .filter(ub -> {
                        // Sprawdzamy czy status to COMPLETED (odporne na wielkość liter i spacje)
                        String status = (ub.getStatus() != null) ? ub.getStatus().trim() : "";
                        return "COMPLETED".equalsIgnoreCase(status);
                    })
                    .mapToInt(ub -> {
                        // Wyciągamy książkę z relacji UserBook -> Book
                        if (ub.getBook() != null && ub.getBook().getNumberOfPages() != null) {
                            int pages = ub.getBook().getNumberOfPages();
                            System.out.println("Znalazłem strony dla: " + ub.getBook().getTitle() + " -> " + pages);
                            return pages;
                        }
                        System.out.println("BRAK STRON DLA: " + (ub.getBook() != null ? ub.getBook().getTitle() : "Nieznana"));
                        return 0;
                    })
                    .sum();

            // 3. Obliczanie procentu (ZABEZPIECZENIE przed dzieleniem przez zero)
            int progressPercent = (total > 0) ? (int) (completed * 100 / total) : 0;

            model.addAttribute("total", total);
            model.addAttribute("completed", completed);
            model.addAttribute("reading", reading);
            model.addAttribute("toRead", toRead);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("progressPercent", progressPercent);
            model.addAttribute("username", user.getUsername());

            return "statistics";

        } catch (Exception e) {
            // Logujemy błąd w konsoli, żebyś wiedziała co się stało
            System.err.println("BŁĄD STATYSTYK: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/home"; // W razie awarii wróć do home zamiast pokazywać błąd 500
        }
    }

}