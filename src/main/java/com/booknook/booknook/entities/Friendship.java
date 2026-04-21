package com.booknook.booknook.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "friendships")
@Data
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "requester_id")
    private User requester; // Osoba, która wysłała zaproszenie / zablokowała

    @ManyToOne
    @JoinColumn(name = "addressee_id")
    private User addressee; // Osoba otrzymująca zaproszenie / zablokowana

    @Enumerated(EnumType.STRING)
    private FriendshipStatus status; // PENDING, ACCEPTED, BLOCKED

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum FriendshipStatus {
        PENDING, ACCEPTED, BLOCKED
    }
}


