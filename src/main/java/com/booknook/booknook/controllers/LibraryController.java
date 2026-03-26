package com.booknook.booknook.controllers;

import com.booknook.booknook.entities.Book;
import com.booknook.booknook.entities.User;
import com.booknook.booknook.entities.UserBook;
import com.booknook.booknook.repositories.BookRepository;
import com.booknook.booknook.repositories.UserBookRepository;
import com.booknook.booknook.repositories.UserRepository;
import com.booknook.booknook.services.OpenLibraryService;
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
            @RequestParam(defaultValue = "false") boolean fromDetails, // Ustawiamy domyślną wartość
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            // Pobieramy książkę lub ją tworzymy
            Book book = bookRepository.findByExternalId(externalId).orElseGet(() -> {
                Book newBook = new Book();
                newBook.setExternalId(externalId);
                newBook.setTitle(title);
                newBook.setAuthors(authors);
                newBook.setCoverUrl(coverUrl);
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

        redirectAttributes.addFlashAttribute("message", "Zmieniono status książki!");

        if (fromDetails) {
            String externalId = userBook.getBook().getExternalId();
            return "redirect:/library/book-details?id=" + externalId;
        }

        return "redirect:/library/my-library";
    }

    @PostMapping("/remove")
    public String removeBookFromLibrary(@RequestParam Long userBookId, RedirectAttributes redirectAttributes) {
        userBookRepository.deleteById(userBookId);

        redirectAttributes.addFlashAttribute("message", "Usunięto książkę z Twojej biblioteczki.");
        return "redirect:/library/my-library";
    }
    @GetMapping("/book-details")
    public String showBookDetails(@RequestParam String id,
                                  @RequestParam(required = false) String coverUrl,
                                  @RequestParam(required = false) String title,
                                  @RequestParam(required = false) String authors,
                                  Model model, Principal principal) {

        // 1. Podstawowe dane książki (tak jak robiliśmy)
        Book book = bookRepository.findByExternalId(id).orElse(null);
        if (book == null) {
            book = new Book();
            book.setExternalId(id);
            book.setCoverUrl(coverUrl);
            book.setTitle(title);
            book.setAuthors(authors);
        }

        // 2. SPRAWDZANIE STATUSU UŻYTKOWNIKA (To jest klucz do Twojego pytania)
        if (principal != null) {
            model.addAttribute("username", principal.getName());
            User user = userRepository.findByUsername(principal.getName()).orElse(null);

            if (user != null && book.getId() != null) {
                // Szukamy, czy ta konkretna książka jest na półce tego użytkownika
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

        String description = openLibraryService.fetchDescription(id);
        book.setDescription(description);
        model.addAttribute("book", book);

        return "book-details";
    }
}