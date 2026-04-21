package com.booknook.booknook.controllers;

import com.booknook.booknook.entities.Friendship;
import com.booknook.booknook.entities.User;
import com.booknook.booknook.services.FriendshipService;
import com.booknook.booknook.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/friends")
public class FriendController {

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private UserService userService;

    // 1. Widok główny znajomych (POŁĄCZONY - obsługuje znajomych i zaproszenia)
    @GetMapping
    public String showFriendsPage(Model model, @AuthenticationPrincipal UserDetails currentUser, Principal principal) {
        User user = userService.findByUsername(currentUser.getUsername());

        // Pobieramy dane z serwisu
        List<Friendship> friends = friendshipService.getAcceptedFriends(user);
        List<Friendship> pending = friendshipService.getPendingRequests(user);

        // Przekazujemy je do widoku pod konkretnymi nazwami
        model.addAttribute("friends", friends);
        model.addAttribute("pendingRequests", pending);
        model.addAttribute("username", principal.getName());

        return "friends";
    }

    // 2. Wyszukiwanie użytkowników
    @GetMapping("/search")
    public String searchUsers(@RequestParam("q") String query, Model model, @AuthenticationPrincipal UserDetails currentUser) {
        User user = userService.findByUsername(currentUser.getUsername());

        List<User> searchResults = userService.searchOtherUsers(query, user.getUsername());
        model.addAttribute("searchResults", searchResults);

        // Musimy doładować resztę danych, żeby sekcje znajomych i zaproszeń nie zniknęły przy wyszukiwaniu
        model.addAttribute("friends", friendshipService.getAcceptedFriends(user));
        model.addAttribute("pendingRequests", friendshipService.getPendingRequests(user));

        return "friends";
    }

    // 3. Akcja: Dodaj znajomego (wyślij zaproszenie)
    @PostMapping("/add/{id}")
    public String addFriend(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        User requester = userService.findByUsername(currentUser.getUsername());
        User addressee = userService.findById(id);

        friendshipService.sendFriendRequest(requester, addressee);

        return "redirect:/friends";
    }

    // 4. Akcja: Zaakceptuj zaproszenie
    @PostMapping("/accept/{id}")
    public String acceptFriend(@PathVariable Long id) {
        friendshipService.acceptFriendship(id);
        return "redirect:/friends";
    }

    // 5. Akcja: Usuń znajomego / Odrzuć zaproszenie
    @PostMapping("/remove/{id}")
    public String removeFriend(@PathVariable Long id) {
        friendshipService.deleteFriendship(id);
        return "redirect:/friends";
    }
}