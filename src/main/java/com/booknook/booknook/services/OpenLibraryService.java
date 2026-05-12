package com.booknook.booknook.services;

import com.booknook.booknook.entities.Book;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OpenLibraryService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> fetchBookDetails(String externalId) {
        Map<String, Object> details = new HashMap<>();
        try {
            // 1. Pobieramy opis z Works
            String workUrl = "https://openlibrary.org" + externalId + ".json";
            Map<String, Object> workResponse = restTemplate.getForObject(workUrl, Map.class);

            if (workResponse != null) {
                details.put("description", extractDescription(workResponse));

                if (workResponse.containsKey("subjects")) {
                    List<String> subjects = (List<String>) workResponse.get("subjects");
                    if (subjects != null && !subjects.isEmpty()) {
                        // Bierzemy max 5 kategorii i łączymy przecinkiem
                        String cats = subjects.stream()
                                .limit(5)
                                .collect(Collectors.joining(", "));
                        details.put("categories", cats);
                    }
                }

                // 2. SZUKAMY DATY I STRON W EDYCJACH
                String editionsUrl = "https://openlibrary.org" + externalId + "/editions.json?limit=100";
                Map<String, Object> edResponse = restTemplate.getForObject(editionsUrl, Map.class);

                Integer oldestYear = null;
                Integer foundPages = null;

                if (edResponse != null && edResponse.containsKey("entries")) {
                    List<Map<String, Object>> entries = (List<Map<String, Object>>) edResponse.get("entries");

                    for (Map<String, Object> edition : entries) {
                        // Szukamy najstarszego roku
                        // Wewnątrz pętli for (Map<String, Object> edition : entries)
                        Integer year = parseYear(edition.get("publish_date"));
                        if (year != null && year > 1000) {
                            if (oldestYear == null || year < oldestYear) {
                                oldestYear = year;
                            }
                        }

                        // Szukamy liczby stron (pierwsza napotkana edycja, która je ma)
                        if (foundPages == null && edition.get("number_of_pages") != null) {
                            foundPages = (Integer) edition.get("number_of_pages");
                        }
                    }
                }

                // Rezerwowe szukanie roku w samym Works
                if (oldestYear == null) {
                    oldestYear = parseYear(workResponse.get("first_publish_year"));
                }

                details.put("firstPublishYear", oldestYear);
                details.put("numberOfPages", foundPages);
            }
        } catch (Exception e) {
            details.put("description", "Brak opisu.");
            System.err.println("Błąd detali: " + e.getMessage());
        }
        return details;
    }
    private String extractDescription(Map<String, Object> workResponse) {
        if (workResponse.containsKey("description")) {
            Object descObj = workResponse.get("description");
            String fullDescription = "";

            if (descObj instanceof Map) {
                fullDescription = (String) ((Map<?, ?>) descObj).get("value");
            } else {
                fullDescription = descObj.toString();
            }

            return cleanDescription(fullDescription);
        }
        return "Brak opisu.";
    }
    private Integer parseYear(Object yearObj) {
        if (yearObj == null) return null;
        try {
            String rawValue = yearObj.toString();
            // Szukamy ciągu dokładnie 4 cyfr (np. 1949, 2020)
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(1|2)\\d{3}");
            java.util.regex.Matcher matcher = pattern.matcher(rawValue);

            if (matcher.find()) {
                return Integer.parseInt(matcher.group());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String cleanDescription(String text) {
        if (text == null) return "";
        int sourceIndex = text.indexOf("([source]");
        int dashesIndex = text.indexOf("----------");
        int seeAlsoIndex = text.indexOf("See also:");
        int cutIndex = -1;
        if (sourceIndex != -1) cutIndex = sourceIndex;
        if (dashesIndex != -1 && (cutIndex == -1 || dashesIndex < cutIndex)) cutIndex = dashesIndex;
        if (seeAlsoIndex != -1 && (cutIndex == -1 || seeAlsoIndex < cutIndex)) cutIndex = seeAlsoIndex;
        return (cutIndex != -1) ? text.substring(0, cutIndex).trim() : text.trim();
    }

    public List<Book> searchBooksByCategory(String category, List<String> excludedIds) {
        List<Book> results = new ArrayList<>();
        try {
            String url = "https://openlibrary.org/search.json?subject="
                    + category.replace(" ", "+") + "&limit=10&fields=key,title,author_name,cover_i,subject";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("docs")) {
                List<Map<String, Object>> docs = (List<Map<String, Object>>) response.get("docs");
                for (Map<String, Object> doc : docs) {
                    String externalId = (String) doc.get("key");
                    if (externalId == null || excludedIds.contains(externalId)) continue;

                    Book book = new Book();
                    book.setExternalId(externalId);
                    book.setTitle((String) doc.get("title"));

                    List<String> authors = (List<String>) doc.get("author_name");
                    if (authors != null && !authors.isEmpty()) {
                        book.setAuthors(String.join(", ", authors));
                    }

                    Object coverId = doc.get("cover_i");
                    if (coverId != null) {
                        book.setCoverUrl("https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg");
                    }

                    List<String> subjects = (List<String>) doc.get("subject");
                    if (subjects != null && !subjects.isEmpty()) {
                        book.setCategories(subjects.stream().limit(5).collect(Collectors.joining(", ")));
                    }

                    results.add(book);
                }
            }
        } catch (Exception e) {
            System.err.println("Błąd pobierania rekomendacji z API: " + e.getMessage());
        }
        return results;
    }
}