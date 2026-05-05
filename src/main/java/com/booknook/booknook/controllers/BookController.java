package com.booknook.booknook.controllers;

import com.booknook.booknook.entities.Book;
import com.booknook.booknook.services.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
public class BookController {

    @Autowired
    private BookService bookService;

    @GetMapping("/search")
    public String searchBooks(@RequestParam String query, Model model, Principal principal) {
        if (query == null || query.trim().isEmpty()) {
            return "redirect:/home";
        }
        if (principal != null) {
            model.addAttribute("username", principal.getName());
        }
        List<Book> books = bookService.searchBooks(query);

        System.out.println(books);
        model.addAttribute("books", books);
        model.addAttribute("query", query);
        return "search-results"; // Nowy plik HTML
    }
}