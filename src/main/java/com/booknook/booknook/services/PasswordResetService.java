package com.booknook.booknook.services;

import com.booknook.booknook.entities.PasswordResetToken;
import com.booknook.booknook.entities.User;
import com.booknook.booknook.repositories.PasswordResetTokenRepository;
import com.booknook.booknook.repositories.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                JavaMailSender mailSender,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void sendResetLink(String email, String baseUrl) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        // Znajdź i usuń stary token jeśli istnieje
        tokenRepository.findByUserId(user.getId()).ifPresent(tokenRepository::delete);
        tokenRepository.flush();

        String token = UUID.randomUUID().toString();
        tokenRepository.save(new PasswordResetToken(token, user));

        String resetUrl = baseUrl + "/reset-password?token=" + token;
        System.out.println("DEBUG: reset URL: " + resetUrl);
        System.out.println("DEBUG mail config: host=" +
                ((JavaMailSenderImpl) mailSender).getHost() +
                " user=" + ((JavaMailSenderImpl) mailSender).getUsername());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("BookNook — reset hasła");
            message.setText("Kliknij link aby zresetować hasło (ważny 1h):\n\n" + resetUrl +
                    "\n\nJeśli to nie Ty, zignoruj tę wiadomość.");
            mailSender.send(message);
            System.out.println("DEBUG: mail wysłany!");
        } catch (Exception e) {
            System.err.println("BŁĄD WYSYŁANIA MAILA: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) return false;

        User user = tokenOpt.get().getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(tokenOpt.get());
        return true;
    }
}