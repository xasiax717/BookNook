package com.booknook.booknook.services;

import com.booknook.booknook.entities.*;
import com.booknook.booknook.repositories.BookRepository;
import com.booknook.booknook.repositories.ReviewRepository;
import com.booknook.booknook.repositories.UserBookRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final UserBookRepository userBookRepository;
    private final BookRepository bookRepository;
    private final OpenLibraryService openLibraryService;
    private final ReviewRepository reviewRepository;

    private final FriendshipService friendshipService;
    public RecommendationService(UserBookRepository userBookRepository,
                                 BookRepository bookRepository,
                                 ReviewRepository reviewRepository,
                                 OpenLibraryService openLibraryService,
                                 FriendshipService friendshipService) {
        this.userBookRepository = userBookRepository;
        this.bookRepository = bookRepository;
        this.openLibraryService = openLibraryService;
        this.reviewRepository = reviewRepository;
        this.friendshipService = friendshipService;
    }

    public List<Book> getRecommendations(User user) {
        // 1. Zbieramy książki użytkownika (przeczytane + czytane)
        List<UserBook> userBooks = userBookRepository.findByUser(user);
        List<String> userExternalIds = userBooks.stream()
                .map(ub -> ub.getBook().getExternalId())
                .collect(Collectors.toList());

        // 2. Wyciągamy gatunki z książek użytkownika i liczymy częstotliwość
        Map<String, Long> categoryFrequency = userBooks.stream()
                .filter(ub -> {
                    // Uwzględniamy tylko książki bez oceny LUB z oceną >= 3
                    Optional<Review> review = reviewRepository.findByUserAndBook(ub.getUser(), ub.getBook());
                    return review.isEmpty() || review.get().getRating() >= 3;
                })
                .map(ub -> ub.getBook().getCategories())
                .filter(cat -> cat != null && !cat.isEmpty())
                .flatMap(cat -> Arrays.stream(cat.split(",")))
                .map(String::trim)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

        if (categoryFrequency.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Bierzemy top 3 najczęstsze gatunki
        List<String> topCategories = categoryFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 4. Szukamy w lokalnej bazie
        Set<Book> recommendations = new LinkedHashSet<>();
        for (String category : topCategories) {
            List<Book> localBooks = bookRepository
                    .findByCategoryContainingAndExternalIdNotIn(category, userExternalIds);
            recommendations.addAll(localBooks);
        }

        // 5. Jeśli za mało (mniej niż 6) — uzupełniamy z OpenLibrary API
        if (recommendations.size() < 6) {
            for (String category : topCategories) {
                if (recommendations.size() >= 6) break;
                List<Book> apiBooks = openLibraryService.searchBooksByCategory(category, userExternalIds);
                recommendations.addAll(apiBooks);
            }
        }

        return new ArrayList<>(recommendations).subList(0, Math.min(12, recommendations.size()));
    }

    public List<Book> getFriendsRecommendations(User user) {
        // 1. Pobieramy zaakceptowanych znajomych
        List<Friendship> friendships = friendshipService.getAcceptedFriends(user);

        // 2. Wyciągamy obiekty User znajomych
        List<User> friends = friendships.stream()
                .map(f -> f.getRequester().getId().equals(user.getId())
                        ? f.getAddressee()
                        : f.getRequester())
                .collect(Collectors.toList());

        if (friends.isEmpty()) return Collections.emptyList();

        // 3. Zbieramy externalId książek które użytkownik już ma
        List<String> userExternalIds = userBookRepository.findByUser(user).stream()
                .map(ub -> ub.getBook().getExternalId())
                .collect(Collectors.toList());

        // 4. Zbieramy książki znajomych których użytkownik nie ma
        // Priorytetujemy książki z wysoką oceną (>= 4) od znajomych
        Set<Book> recommendations = new LinkedHashSet<>();

        // Najpierw: książki które znajomi wysoko ocenili
        for (User friend : friends) {
            userBookRepository.findByUser(friend).stream()
                    .filter(ub -> !userExternalIds.contains(ub.getBook().getExternalId()))
                    .filter(ub -> {
                        Optional<Review> review = reviewRepository.findByUserAndBook(friend, ub.getBook());
                        return review.isPresent() && review.get().getRating() >= 4;
                    })
                    .map(UserBook::getBook)
                    .forEach(recommendations::add);
        }

        // Potem: książki znajomych bez oceny lub z oceną >= 3
        for (User friend : friends) {
            userBookRepository.findByUser(friend).stream()
                    .filter(ub -> !userExternalIds.contains(ub.getBook().getExternalId()))
                    .filter(ub -> {
                        Optional<Review> review = reviewRepository.findByUserAndBook(friend, ub.getBook());
                        return review.isEmpty() || review.get().getRating() >= 3;
                    })
                    .map(UserBook::getBook)
                    .forEach(recommendations::add);
        }

        return new ArrayList<>(recommendations).subList(0, Math.min(12, recommendations.size()));
    }
}
