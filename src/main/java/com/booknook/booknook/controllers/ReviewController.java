package com.booknook.booknook.controllers;

import com.booknook.booknook.entities.User;
import com.booknook.booknook.repositories.UserRepository;
import com.booknook.booknook.services.ReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public ReviewController(ReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    @PostMapping("/save")
    public String saveReview(@RequestParam String externalId,
                             @RequestParam Integer rating,
                             @RequestParam(required = false) String reviewText,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        try {
            reviewService.saveReview(user, externalId, rating, reviewText);
            redirectAttributes.addFlashAttribute("message", "Recenzja zapisana!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas zapisywania recenzji.");
        }

        return "redirect:/library/book-details?id=" + externalId;
    }

    @PostMapping("/delete")
    public String deleteReview(@RequestParam Long reviewId,
                               @RequestParam String externalId,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        try {
            reviewService.deleteReview(user, reviewId);
            redirectAttributes.addFlashAttribute("message", "Recenzja usunięta.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas usuwania recenzji.");
        }

        return "redirect:/library/book-details?id=" + externalId;
    }
}