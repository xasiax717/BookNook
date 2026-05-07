package com.booknook.booknook.entities;
import jakarta.persistence.*;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.DataAmount;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true)
    private String externalId;

    private String title;
    private String authors;

    @Column(columnDefinition = "TEXT")
    private String categories;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String isbn;
    private Integer firstPublishYear;
    private String coverUrl;
    private Integer numberOfPages;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    private List<UserBook> userBooks = new ArrayList<>();
}