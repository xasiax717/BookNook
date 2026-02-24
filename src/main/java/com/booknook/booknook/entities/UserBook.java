package com.booknook.booknook.entities;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_books")
@Data
@NoArgsConstructor
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

    private LocalDateTime addedAt;
    private String status; // np. "TO_READ", "READING", "COMPLETED"

    public UserBook(User user, Book book) {
        this.user = user;
        this.book = book;
        this.addedAt = LocalDateTime.now();
        this.status = "TO_READ"; // domyślny status
    }
}

