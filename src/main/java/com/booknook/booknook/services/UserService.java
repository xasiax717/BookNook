package com.booknook.booknook.services;


import com.booknook.booknook.entities.User;
import com.booknook.booknook.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;




@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public User saveUser(User user) {

        if (!user.getPassword().equals(user.getConfirmPassword())) {
            throw new IllegalArgumentException("Hasła nie są identyczne!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        return savedUser;
    }


}
