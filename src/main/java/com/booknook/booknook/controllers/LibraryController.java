package com.booknook.booknook.controllers;

import com.booknook.booknook.entities.Book;
import com.booknook.booknook.entities.User;
import com.booknook.booknook.entities.UserBook;
import com.booknook.booknook.repositories.BookRepository;
import com.booknook.booknook.repositories.UserBookRepository;
import com.booknook.booknook.repositories.UserRepository;
import com.booknook.booknook.services.OpenLibraryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/library")
public class LibraryController {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final UserBookRepository userBookRepository;
    private final OpenLibraryService openLibraryService;

    public LibraryController(UserRepository userRepository,
                             BookRepository bookRepository,
                             UserBookRepository userBookRepository,
                             OpenLibraryService openLibraryService) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.userBookRepository = userBookRepository;
        this.openLibraryService = openLibraryService;
    }

    @PostMapping("/add")
    public String addBookToLibrary(
            @RequestParam String externalId,
            @RequestParam String title,
            @RequestParam String authors,
            @RequestParam(required = false) String coverUrl,
            @RequestParam String newStatus,
            @RequestParam(required = false) String categories,
            @RequestParam(required = false) Integer numberOfPages,
            @RequestParam(defaultValue = "false") boolean fromDetails,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            System.out.println(numberOfPages);
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            // Pobieramy książkę lub ją tworzymy
            Book book = bookRepository.findByExternalId(externalId).orElseGet(() -> {
                Book newBook = new Book();
                newBook.setExternalId(externalId);
                newBook.setTitle(title);
                newBook.setAuthors(authors);
                newBook.setCoverUrl(coverUrl);
                newBook.setCategories(categories);
                newBook.setNumberOfPages(numberOfPages);
                return bookRepository.save(newBook);
            });

            if (userBookRepository.existsByUserAndBook(user, book)) {
                redirectAttributes.addFlashAttribute("error", "Masz już tę książkę w biblioteczce!");
            } else {
                UserBook userBook = new UserBook();
                userBook.setUser(user);
                userBook.setBook(book);
                userBook.setAddedAt(LocalDateTime.now());
                userBook.setStatus(newStatus); // Naprawiony średnik

                userBookRepository.save(userBook);
                redirectAttributes.addFlashAttribute("message", "Dodano książkę: " + title);
            }

        } catch (Exception e) {
            System.err.println("BŁĄD PODCZAS DODAWANIA: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd podczas dodawania książki.");
        }

        // --- BEZPIECZNE PRZEKIEROWANIE ---
        if (fromDetails) {
            return UriComponentsBuilder.fromPath("redirect:/library/book-details")
                    .queryParam("id", externalId)
                    .queryParam("coverUrl", coverUrl)
                    .queryParam("title", title)
                    .queryParam("authors", authors)
                    .queryParam("categories", categories)
                    .build()
                    .toUriString();
        }

        return "redirect:/home";
    }

    @GetMapping("/my-library")
    public String viewMyLibrary(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        List<UserBook> myBooks = userBookRepository.findByUser(user);

        model.addAttribute("userBooks", myBooks);
        model.addAttribute("username", user.getUsername());

        return "my-library";
    }

    @PostMapping("/update-status")
    public String updateBookStatus(
            @RequestParam Long userBookId,
            @RequestParam String newStatus,
            @RequestParam(defaultValue = "false") boolean fromDetails, // NOWE
            RedirectAttributes redirectAttributes) {

        UserBook userBook = userBookRepository.findById(userBookId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono pozycji na półce"));

        userBook.setStatus(newStatus);
        userBookRepository.save(userBook);

        updateUserPages(userBook.getUser(), newStatus);

        redirectAttributes.addFlashAttribute("message", "Zmieniono status książki!");

        if (fromDetails) {
            String externalId = userBook.getBook().getExternalId();
            return "redirect:/library/book-details?id=" + externalId;
        }

        return "redirect:/library/my-library";
    }

    @PostMapping("/remove")
    public String removeBookFromLibrary(@RequestParam Long userBookId,
                                        @RequestParam(value = "newStatus", required = false) String newStatus,
                                        @RequestParam(defaultValue = "false") boolean fromDetails,
                                        Principal principal) {
        try {
            // Pobieramy ID książki zanim ją usuniemy, żeby wiedzieć gdzie wrócić
            UserBook ub = userBookRepository.findById(userBookId).orElse(null);
            String bookExternalId = (ub != null) ? ub.getBook().getExternalId() : "";

            userBookRepository.deleteById(userBookId);

            if (fromDetails && !bookExternalId.isEmpty()) {
                // Wracamy na detale używając tylko ID - resztę dociągnie sobie sam GET
                return "redirect:/library/book-details?id=" + bookExternalId;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        return "redirect:/library/my-library";
    }

    @GetMapping("/book-details")
    public String showBookDetails(@RequestParam String id,
                                  @RequestParam(required = false) String coverUrl,
                                  @RequestParam(required = false) String title,
                                  @RequestParam(required = false) String authors,
                                  @RequestParam(required = false) String categories,
                                  Model model, Principal principal) {

        Book book = bookRepository.findByExternalId(id).orElse(null);
        if (book == null) {
            book = new Book();
            book.setExternalId(id);
            book.setCoverUrl(coverUrl);
            book.setTitle(title);
            book.setAuthors(authors);
            book.setCategories(categories);
        }


        Map<String, Object> extraDetails = openLibraryService.fetchBookDetails(id);

        // Przypisujemy pobrane dane do obiektu book
        if (book.getDescription() == null || book.getDescription().isEmpty()) {
            String apiDescription = (String) extraDetails.get("description");
            if (apiDescription != null) {
                book.setDescription(apiDescription);
            }
        }
        if (book.getCategories() == null || book.getCategories().isEmpty()) {
            String apiCategories = (String) extraDetails.get("categories");
            if (apiCategories != null) {
                book.setCategories(apiCategories);
            } else if (categories != null) {
                book.setCategories(categories);
            }
        }

        if (extraDetails.get("firstPublishYear") != null) {
            book.setFirstPublishYear((Integer) extraDetails.get("firstPublishYear"));
        }

        if (extraDetails.get("numberOfPages") != null) {
            book.setNumberOfPages((Integer) extraDetails.get("numberOfPages"));
        }

        if (principal != null) {
            model.addAttribute("username", principal.getName());
            User user = userRepository.findByUsername(principal.getName()).orElse(null);

            // Ważne: relację sprawdzamy tylko jeśli książka już istnieje w naszej bazie (ma ID)
            if (user != null && book.getId() != null) {
                var userBookOpt = userBookRepository.findByUserAndBook(user, book);
                if (userBookOpt.isPresent()) {
                    model.addAttribute("isSaved", true);
                    model.addAttribute("currentStatus", userBookOpt.get().getStatus());
                    model.addAttribute("userBookId", userBookOpt.get().getId());
                } else {
                    model.addAttribute("isSaved", false);
                }
            } else {
                model.addAttribute("isSaved", false);
            }
        }

        model.addAttribute("book", book);
        System.out.println("DEBUG: Rok z API: " + extraDetails.get("firstPublishYear"));
        System.out.println("DEBUG: Strony z API: " + extraDetails.get("numberOfPages"));
        return "book-details";
    }

    @PostMapping("/api/add")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> addApi(
            @RequestParam String externalId,
            @RequestParam String title,
            @RequestParam String authors,
            @RequestParam(required = false) String coverUrl,
            @RequestParam String newStatus,
            @RequestParam(required = false) String categories,
            @RequestParam(required = false) Integer numberOfPages,
            Principal principal) {
        try {
            User user = userRepository.findByUsername(principal.getName()).orElseThrow();

            // Ta sama logika co w Twoim add - szukamy lub tworzymy książkę
            Book book = bookRepository.findByExternalId(externalId).orElseGet(() -> {
                Book newBook = new Book();
                newBook.setExternalId(externalId);
                newBook.setTitle(title);
                newBook.setAuthors(authors);
                newBook.setCoverUrl(coverUrl);
                newBook.setCategories(categories);
                newBook.setNumberOfPages(numberOfPages);
                return bookRepository.save(newBook);
            });

            UserBook userBook = userBookRepository.findByUserAndBook(user, book)
                    .orElse(new UserBook());

            userBook.setUser(user);
            userBook.setBook(book);
            userBook.setStatus(newStatus);
            userBook.setAddedAt(LocalDateTime.now());

            UserBook saved = userBookRepository.save(userBook);

            updateUserPages(user, newStatus); // AKTUALIZACJA LICZNIKA

            // Zwracamy JSON z danymi, które JS wykorzysta do podmiany przycisku
            return org.springframework.http.ResponseEntity.ok(Map.of(
                    "status", "success",
                    "newStatus", newStatus,
                    "userBookId", saved.getId()
            ));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("status", "error"));
        }
    }

    @PostMapping("/api/remove")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> removeApi(@RequestParam Long userBookId) {
        try {
            // 1. Najpierw znajdujemy UserBook, żeby wiedzieć, czyj licznik aktualizować
            UserBook ub = userBookRepository.findById(userBookId).orElse(null);

            if (ub != null) {
                User owner = ub.getUser();

                // 2. Usuwamy powiązanie
                userBookRepository.delete(ub);

                //updateUserPages(owner);

                // 4. Dopiero teraz zwracamy odpowiedź (to musi być ostatnie!)
                return org.springframework.http.ResponseEntity.ok(Map.of("status", "deleted"));
            }

            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("status", "not_found"));

        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("status", "error"));
        }
    }

    private void updateUserPages(User user, String newStatus) {
        int totalPages = userBookRepository.findByUser(user).stream()
                .filter(ub -> "COMPLETED".equals(ub.getStatus()))
                .mapToInt(ub -> ub.getBook().getNumberOfPages() != null ? ub.getBook().getNumberOfPages() : 0)
                .sum();
        user.setTotalPagesRead(totalPages);
        userRepository.save(user);

    }
}