package com.booknook.booknook.security;

import com.booknook.booknook.services.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity

public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
     return httpSecurity

             .authorizeHttpRequests(registry -> {
                 registry.requestMatchers("/register", "/login", "/css/**", "/js/**", "/error", "/forgot-password", "/reset-password").permitAll();
                 registry.anyRequest().authenticated();
             })
             .formLogin(httpForm -> {
                 httpForm
                         .loginPage("/login")
                         .defaultSuccessUrl("/home", true) // Przekierowanie na stronę główną po sukcesie
                         .failureUrl("/login?error=true") // Przekierowanie z powrotem na login po błędzie
                         .permitAll();
             })
             .logout(logout -> logout
                     .logoutSuccessUrl("/login?logout=true")
                     .permitAll()
             )
             .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}
