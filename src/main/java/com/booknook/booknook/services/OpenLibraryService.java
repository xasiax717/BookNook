package com.booknook.booknook.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class OpenLibraryService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String fetchDescription(String externalId) {
        String url = "https://openlibrary.org" + externalId + ".json";
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("description")) {
                Object descObj = response.get("description");
                String fullDescription = "";

                if (descObj instanceof Map) {
                    fullDescription = (String) ((Map<?, ?>) descObj).get("value");
                } else {
                    fullDescription = descObj.toString();
                }

                return cleanDescription(fullDescription);
            }
        } catch (Exception e) {
            return "Brak opisu.";
        }
        return "Brak opisu.";
    }

    // Pomocnicza metoda do usuwania śmieci z tekstu
    private String cleanDescription(String text) {
        if (text == null) return "";

        int sourceIndex = text.indexOf("([source]");
        int dashesIndex = text.indexOf("----------");
        int seeAlsoIndex = text.indexOf("See also:");

        int cutIndex = -1;

        if (sourceIndex != -1) cutIndex = sourceIndex;
        if (dashesIndex != -1 && (cutIndex == -1 || dashesIndex < cutIndex)) cutIndex = dashesIndex;
        if (seeAlsoIndex != -1 && (cutIndex == -1 || seeAlsoIndex < cutIndex)) cutIndex = seeAlsoIndex;

        if (cutIndex != -1) {
            return text.substring(0, cutIndex).trim();
        }

        return text.trim();
    }
}