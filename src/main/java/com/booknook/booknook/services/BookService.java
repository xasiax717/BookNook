package com.booknook.booknook.services;

import com.booknook.booknook.dto.OpenLibraryResponse;
import com.booknook.booknook.entities.Book;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookService {

    private final RestTemplate restTemplate = new RestTemplate();

    public List<Book> searchBooks(String query) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>();

        // 1. Uproszczony URL - bez agresywnych filtrów, które mogą ukrywać "Fourth Wing"
        String url = UriComponentsBuilder
                .fromHttpUrl("https://openlibrary.org/search.json")
                .queryParam("q", query)
                .queryParam("lang", "pol,eng")
                .queryParam("limit", 150) // Pobieramy dużo, żeby Java miała co filtrować
                .toUriString();

        try {
            OpenLibraryResponse response = restTemplate.getForObject(url, OpenLibraryResponse.class);
            List<Book> books = new ArrayList<>();
            // Set będzie trzymał ID (key), a nie tytuły, żeby nie blokować części serii o podobnych nazwach
            java.util.Set<String> seenKeys = new java.util.HashSet<>();

            if (response != null && response.getDocs() != null) {
                for (var doc : response.getDocs()) {

                    // FILTR 1: Musi mieć okładkę (jakość)
                    if (doc.getCoverI() == null) continue;

                    // FILTR 2: Unikalność po kluczu Open Library (zapobiega identycznym wydaniom)
                    if (seenKeys.contains(doc.getKey())) continue;

                    // FILTR 3: Język (robimy to w Javie, a nie w URL, żeby być bardziej elastycznym)
                    boolean isProperLang = false;
                    if (doc.getLanguage() == null) {
                        isProperLang = true; // Jeśli nie wpisano języka, ryzykujemy i pokazujemy
                    } else {
                        for (String l : doc.getLanguage()) {
                            if (l.startsWith("pol") || l.startsWith("eng") || l.equals("pl") || l.equals("en")) {
                                isProperLang = true;
                                break;
                            }
                        }
                    }

                    if (isProperLang) {
                        books.add(mapOpenLibraryToEntity(doc));
                        seenKeys.add(doc.getKey());
                    }

                    if (books.size() >= 30) break; // Zwiększamy limit do 30, żeby Harry miał wszystkie części
                }
            }
            return books;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Book mapOpenLibraryToEntity(OpenLibraryResponse.Doc doc) {
        Book book = new Book();
        book.setExternalId(doc.getKey());
        book.setTitle(doc.getTitle());

        book.setAuthors(doc.getAuthorName() != null ? String.join(", ", doc.getAuthorName()) : "Nieznany autor");

        if (doc.getFirstPublishYear() != null) {
            book.setPublishedYear(String.valueOf(doc.getFirstPublishYear()));
        }

        if (doc.getCoverI() != null) {
            book.setCoverUrl("https://covers.openlibrary.org/b/id/" + doc.getCoverI() + "-M.jpg");
        }

        if (doc.getIsbn() != null && !doc.getIsbn().isEmpty()) {
            book.setIsbn(doc.getIsbn().get(0));
        }

        return book;
    }
}