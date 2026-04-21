package com.booknook.booknook.services;


import com.booknook.booknook.entities.User;
import com.booknook.booknook.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;




//package com.booknook.booknook.services;


import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}
