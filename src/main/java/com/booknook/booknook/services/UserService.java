package com.booknook.booknook.services;


import com.booknook.booknook.entities.Friendship;
import com.booknook.booknook.entities.User;
import com.booknook.booknook.repositories.FriendshipRepository;
import com.booknook.booknook.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;




//package com.booknook.booknook.services;


import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FriendshipRepository friendshipRepository;

    @Transactional // Zapewnia spójność bazy danych
    public User save(User user) {
        log.info("Próba rejestracji użytkownika: {}", user.getUsername());

        // 1. Sprawdzenie unikalności (żeby uniknąć błędu SQL w konsoli)
        if (userRepository.existsByUsername(user.getUsername())) {
            log.error("Rejestracja nieudana: Login {} jest zajęty", user.getUsername());
            throw new RuntimeException("Nazwa użytkownika jest już zajęta.");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            log.error("Rejestracja nieudana: Email {} jest już w bazie", user.getEmail());
            throw new RuntimeException("Adres e-mail jest już zarejestrowany.");
        }

        // 2. Szyfrowanie hasła
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 4. Zapis
        User savedUser = userRepository.save(user);
        log.info("Użytkownik {} został pomyślnie zarejestrowany", savedUser.getUsername());

        return savedUser;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Użytkownik o nazwie " + username + " nie został znaleziony."));
    }

    public String getFriendshipStatus(User me, User other) {
        Optional<Friendship> relation = friendshipRepository.findRelation(me, other);

        if (relation.isEmpty()) return "NONE";

        Friendship f = relation.get();
        if (f.getStatus() == Friendship.FriendshipStatus.ACCEPTED) return "FRIENDS";
        if (f.getStatus() == Friendship.FriendshipStatus.BLOCKED) return "BLOCKED";

        // Jeśli status to PENDING, musimy wiedzieć, kto wysłał
        if (f.getRequester().equals(me)) return "SENT";
        return "RECEIVED";
    }

    // Dodaj to do UserService.java

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika o ID: " + id));
    }

    public List<User> searchOtherUsers(String query, String currentUsername) {
        // Szukamy osób, których nazwa zawiera query, ale wykluczamy siebie
        return userRepository.findByUsernameContainingIgnoreCaseAndUsernameNot(query, currentUsername);
    }
}
