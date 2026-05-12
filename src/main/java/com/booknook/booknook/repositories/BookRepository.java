package com.booknook.booknook.repositories;

import com.booknook.booknook.entities.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByExternalId(String externalId);

    @Query("SELECT b FROM Book b WHERE b.categories LIKE %:category% AND b.externalId NOT IN :excludedIds")
    List<Book> findByCategoryContainingAndExternalIdNotIn(
            @Param("category") String category,
            @Param("excludedIds") List<String> excludedIds
    );
}