package com.booknook.booknook.repositories;

import com.booknook.booknook.entities.Book;
import com.booknook.booknook.entities.User;
import com.booknook.booknook.entities.UserBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBookRepository extends JpaRepository<UserBook, Long> {
    boolean existsByUserAndBook(User user, Book book);
    List<UserBook> findByUser(User user);
}