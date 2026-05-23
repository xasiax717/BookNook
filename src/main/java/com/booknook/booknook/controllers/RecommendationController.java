package com.booknook.booknook.controllers;

import com.booknook.booknook.entities.Book;
import com.booknook.booknook.entities.User;
import com.booknook.booknook.repositories.UserRepository;
import com.booknook.booknook.services.RecommendationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    public RecommendationController(RecommendationService recommendationService,
                                    UserRepository userRepository) {
        this.recommendationService = recommendationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/recommendations")
    public String showRecommendations(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        model.addAttribute("recommendations", recommendationService.getRecommendations(user));
        model.addAttribute("friendsRecommendations", recommendationService.getFriendsRecommendations(user)); // ← dodaj
        model.addAttribute("username", principal.getName());

        return "recommendations";
    }
}