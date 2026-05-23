package com.booknook.booknook.repositories;

import com.booknook.booknook.entities.Book;
import com.booknook.booknook.entities.Review;
import com.booknook.booknook.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByBookOrderByCreatedAtDesc(Book book);

    Optional<Review> findByUserAndBook(User user, Book book);

    List<Review> findByUser(User user);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book = :book")
    Double findAverageRatingByBook(@Param("book") Book book);
}