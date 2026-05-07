package com.booknook.booknook.services;

import com.booknook.booknook.dto.OpenLibraryResponse;
import com.booknook.booknook.entities.Book;
import org.springframework.beans.factory.annotation.Autowired;
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

        String solrQuery = "title:(" + query + ") OR author:(" + query + ")";

        System.out.println(solrQuery + " --- NOWY SOLR QUERY ---");


        /**
        String url = UriComponentsBuilder
                .fromHttpUrl("https://openlibrary.org/search.json")
                .queryParam("q", solrQuery)
                //.queryParam("lang", "pol,eng")
                .queryParam("limit", 150)
                //.queryParam("author", query)
                .toUriString();
         */

        //String url = "https://openlibrary.org/search.json?q=" + query.replace(" ", "+") + "&limit=150";
        String url = "https://openlibrary.org/search.json?q=" + query.replace(" ", "+")
                + "&limit=150&fields=key,title,author_name,first_publish_year,cover_i,isbn,language,number_of_pages_median,subject";

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

//                    System.out.println("Książka: " + doc.getTitle() + " Języki: " + doc.getLanguage());

                    if (books.size() >= 30) break;
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
            book.setFirstPublishYear(doc.getFirstPublishYear());
        }

        if (doc.getCoverI() != null) {
            book.setCoverUrl("https://covers.openlibrary.org/b/id/" + doc.getCoverI() + "-M.jpg");
        }

        if (doc.getIsbn() != null && !doc.getIsbn().isEmpty()) {
            book.setIsbn(doc.getIsbn().get(0));
        }

        if (doc.getSubject()!= null && !doc.getSubject().isEmpty()) {
            String tags = doc.getSubject().stream()
                    .limit(5)
                    .collect(java.util.stream.Collectors.joining(", "));
            book.setCategories(tags);
            System.out.println("Zmapowane kategorie: " + tags);
        } else {
            book.setCategories("General");
        }
        return book;
    }

    @Autowired
    private com.booknook.booknook.repositories.BookRepository bookRepository;

    public Book findByExternalId(String externalId) {
        return bookRepository.findByExternalId(externalId).orElse(null);
    }
}