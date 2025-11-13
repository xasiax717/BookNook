package com.booknook.booknook.services;


import com.booknook.booknook.entities.User;
import com.booknook.booknook.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;


    public User saveUser(User user) {
//        System.out.println(user);
        User savedUser = userRepository.save(user);
//        System.out.println("zapisany:" + savedUser);
        return savedUser;

    }
}
