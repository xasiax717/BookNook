package com.booknook.booknook.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;



@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String username;
    private String email;
    private String password;
    @Transient
    private String confirmPassword;
    private String name;
    private String lastName;
    private Long phoneNumber;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate dateOfBirth;
    private String sex;

//    public User(String username, String passwordHash) {
//        this.username = username;
//        this.password = password;
//    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserBook> userBooks = new ArrayList<>();


}
