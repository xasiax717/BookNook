package com.booknook.booknook.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
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
    @Email(message = "Niepoprawny format adresu e-mail")
    @NotBlank(message = "E-mail nie może być pusty")
    @Column(unique = true) // To już masz pewnie dodane
    private String email;
    private String password;
    @Transient
    private String confirmPassword;
    private String name;
    private String lastName;
    private Long phoneNumber;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    @Past(message = "Nieprawidłowa data urodzenia")
    private LocalDate dateOfBirth;
    private String sex;
    private Integer totalPagesRead = 0;



    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserBook> userBooks = new ArrayList<>();


}
