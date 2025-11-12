package com.booknook.booknook.entities;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "user_book")
public class UserBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;


    private String status;
    private LocalDate addedDate;
    private Integer rating;

}

