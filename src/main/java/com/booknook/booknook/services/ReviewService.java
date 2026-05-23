package com.booknook.booknook.services;

import com.booknook.booknook.entities.Book;
import com.booknook.booknook.entities.Review;
import com.booknook.booknook.entities.User;
import com.booknook.booknook.repositories.BookRepository;
import com.booknook.booknook.repositories.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;

    public ReviewService(ReviewRepository reviewRepository, BookRepository bookRepository) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
    }

    public Review saveReview(User user, String externalId, Integer rating, String reviewText) {
        Book book = bookRepository.findByExternalId(externalId)
                .orElseThrow(() -> new RuntimeException("Książka nie znaleziona"));

        Review review = reviewRepository.findByUserAndBook(user, book)
                .orElse(new Review());

        review.setUser(user);
        review.setBook(book);
        review.setRating(rating);
        review.setReviewText(reviewText);

        return reviewRepository.save(review);
    }

    public void deleteReview(User user, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Recenzja nie znaleziona"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Brak uprawnień");
        }

        reviewRepository.delete(review);
    }

    public List<Review> getReviewsForBook(Book book) {
        return reviewRepository.findByBookOrderByCreatedAtDesc(book);
    }

    public Optional<Review> getUserReviewForBook(User user, Book book) {
        return reviewRepository.findByUserAndBook(user, book);
    }

    public Double getAverageRating(Book book) {
        return reviewRepository.findAverageRatingByBook(book);
    }
}