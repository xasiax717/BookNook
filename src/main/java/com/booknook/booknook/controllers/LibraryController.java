package com.booknook.booknook.controllers;

import com.booknook.booknook.entities.Book;
import com.booknook.booknook.entities.User;
import com.booknook.booknook.entities.UserBook;
import com.booknook.booknook.repositories.BookRepository;
import com.booknook.booknook.repositories.UserBookRepository;
import com.booknook.booknook.repositories.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/library")
public class LibraryController {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final UserBookRepository userBookRepository;

    public LibraryController(UserRepository userRepository,
                             BookRepository bookRepository,
                             UserBookRepository userBookRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.userBookRepository = userBookRepository;
    }

    @PostMapping("/add")
    public String addBookToLibrary(
            @RequestParam String externalId,
            @RequestParam String title,
            @RequestParam String authors,
            @RequestParam(required = false) String coverUrl,
            @RequestParam String status,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            // 1. Pobierz użytkownika
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            // 2. Znajdź lub stwórz książkę
            Book book = bookRepository.findByExternalId(externalId).orElseGet(() -> {
                Book newBook = new Book();
                newBook.setExternalId(externalId);
                newBook.setTitle(title);
                newBook.setAuthors(authors);
                newBook.setCoverUrl(coverUrl);
                return bookRepository.save(newBook);
            });

            // 3. Sprawdź duplikaty na półce użytkownika
            if (userBookRepository.existsByUserAndBook(user, book)) {
                redirectAttributes.addFlashAttribute("error", "Masz już tę książkę w biblioteczce!");
            } else {
                // 4. Bezpieczne tworzenie UserBook (bez polegania na konstruktorze)
                UserBook userBook = new UserBook();
                userBook.setUser(user);
                userBook.setBook(book);
                userBook.setAddedAt(LocalDateTime.now());
                userBook.setStatus(status);; // Domyślny status

                userBookRepository.save(userBook);
                redirectAttributes.addFlashAttribute("message", "Dodano książkę: " + title);
            }

        } catch (Exception e) {
            // Jeśli coś pójdzie nie tak, zobaczysz to w konsoli IntelliJ
            System.err.println("BŁĄD PODCZAS DODAWANIA: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd podczas dodawania książki.");
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
    public String updateBookStatus(@RequestParam Long userBookId, @RequestParam String newStatus, RedirectAttributes redirectAttributes) {
        UserBook userBook = userBookRepository.findById(userBookId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono pozycji na półce"));

        userBook.setStatus(newStatus);
        userBookRepository.save(userBook);

        redirectAttributes.addFlashAttribute("message", "Przeniesiono książkę!");
        return "redirect:/library/my-library";
    }

    @PostMapping("/remove")
    public String removeBookFromLibrary(@RequestParam Long userBookId, RedirectAttributes redirectAttributes) {
        userBookRepository.deleteById(userBookId);

        redirectAttributes.addFlashAttribute("message", "Usunięto książkę z Twojej biblioteczki.");
        return "redirect:/library/my-library";
    }
}