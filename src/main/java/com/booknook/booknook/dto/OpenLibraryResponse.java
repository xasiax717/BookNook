package com.booknook.booknook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OpenLibraryResponse {
    @JsonProperty("docs")
    private List<Doc> docs;

    @Data
    public static class Doc {
        private String key; // unikalne ID (zamiast googleId)
        private String title;

        @JsonProperty("author_name")
        private List<String> authorName;

        @JsonProperty("first_publish_year")
        private Integer firstPublishYear;

        @JsonProperty("language")
        private List<String> language;

        @JsonProperty("isbn")
        private List<String> isbn;

        @JsonProperty("number_of_pages_median")
        private Integer numberOfPagesMedian;

        @JsonProperty("cover_i")
        private Integer coverI; // numer do wygenerowania okładki
    }
}