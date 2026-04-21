package com.booknook.booknook.repositories;

import com.booknook.booknook.entities.Friendship;
import com.booknook.booknook.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // Szukamy konkretnej relacji między dwoma osobami (niezależnie kto zaczął)
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester = :u1 AND f.addressee = :u2) OR " +
            "(f.requester = :u2 AND f.addressee = :u1)")
    Optional<Friendship> findRelation(@Param("u1") User u1, @Param("u2") User u2);

    // W FriendshipRepository.java
    @Query("SELECT f FROM Friendship f WHERE f.addressee = :user AND f.status = 'PENDING'")
    List<Friendship> findAllPendingRequests(@Param("user") User user);

    // Lista wszystkich zaakceptowanych znajomych dla danego użytkownika
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester = :user OR f.addressee = :user) " +
            "AND f.status = com.booknook.booknook.entities.Friendship.FriendshipStatus.ACCEPTED")
    List<Friendship> findAllAcceptedFriends(@Param("user") User user);

    // Oczekujące zaproszenia dla mnie (jestem addressee)
    List<Friendship> findAllByAddresseeAndStatus(User addressee, Friendship.FriendshipStatus status);
}