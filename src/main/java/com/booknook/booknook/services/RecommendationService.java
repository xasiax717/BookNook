package com.booknook.booknook.services;

import com.booknook.booknook.entities.Book;
import com.booknook.booknook.entities.User;
import com.booknook.booknook.entities.UserBook;
import com.booknook.booknook.repositories.BookRepository;
import com.booknook.booknook.repositories.UserBookRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final UserBookRepository userBookRepository;
    private final BookRepository bookRepository;
    private final OpenLibraryService openLibraryService;

    public RecommendationService(UserBookRepository userBookRepository,
                                 BookRepository bookRepository,
                                 OpenLibraryService openLibraryService) {
        this.userBookRepository = userBookRepository;
        this.bookRepository = bookRepository;
        this.openLibraryService = openLibraryService;
    }

    public List<Book> getRecommendations(User user) {
        // 1. Zbieramy książki użytkownika (przeczytane + czytane)
        List<UserBook> userBooks = userBookRepository.findByUser(user);
        List<String> userExternalIds = userBooks.stream()
                .map(ub -> ub.getBook().getExternalId())
                .collect(Collectors.toList());

        // 2. Wyciągamy gatunki z książek użytkownika i liczymy częstotliwość
        Map<String, Long> categoryFrequency = userBooks.stream()
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
}