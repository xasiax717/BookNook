package com.booknook.booknook.dto;

import lombok.Data;
import java.util.List;


@Data
public class GoogleBookResponse {

    private List<Item> items;

    @Data
    public static class Item {
        private String id;
        private VolumeInfo volumeInfo;
    }

    @Data
    public static class VolumeInfo {
        private String title;
        private List<String> authors;
        private String description;
        private List<IndustryIdentifier> industryIdentifiers;
        private String language;
        private ImageLinks imageLinks;
        private String publishedDate;
        private List<String> categories;
    }

    @Data
    public static class IndustryIdentifier {
        private String type;
        private String identifier;
    }

    @Data
    public static class ImageLinks {
        private String thumbnail;
    }
}

