package com.lengoga.webtech_projekt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class TmdbService {

    @Value("${tmdb.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String BASE_URL = "https://api.themoviedb.org/3";

    public TmdbSearchResult searchMedia(String title, String type) {
        String endpoint = type.equals("MOVIE") ? "/search/movie" : "/search/tv";

        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + endpoint)
                .queryParam("api_key", apiKey)
                .queryParam("query", title)
                .queryParam("language", "de-DE")
                .toUriString();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                if (!results.isEmpty()) {
                    Map<String, Object> firstResult = results.get(0);
                    Integer tmdbId = (Integer) firstResult.get("id");
                    return new TmdbSearchResult(tmdbId, getTrailerUrl(tmdbId, type));
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Suchen in TMDb: " + e.getMessage());
        }
        return null;
    }

    public String getTrailerUrl(Integer tmdbId, String type) {
        if (tmdbId == null) return null;

        String endpoint = type.equals("MOVIE") ? "/movie/" : "/tv/";
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + endpoint + tmdbId + "/videos")
                .queryParam("api_key", apiKey)
                .queryParam("language", "de-DE")
                .toUriString();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> videos = (List<Map<String, Object>>) response.get("results");

                for (Map<String, Object> video : videos) {
                    String site = (String) video.get("site");
                    String videoType = (String) video.get("type");
                    if ("YouTube".equals(site) && "Trailer".equals(videoType)) {
                        String key = (String) video.get("key");
                        return "https://www.youtube.com/watch?v=" + key;
                    }
                }

                if (!videos.isEmpty()) {
                    Map<String, Object> firstVideo = videos.get(0);
                    if ("YouTube".equals(firstVideo.get("site"))) {
                        String key = (String) firstVideo.get("key");
                        return "https://www.youtube.com/watch?v=" + key;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen der Videos: " + e.getMessage());
        }
        return null;
    }

    public static class TmdbSearchResult {
        private Integer tmdbId;
        private String trailerUrl;

        public TmdbSearchResult(Integer tmdbId, String trailerUrl) {
            this.tmdbId = tmdbId;
            this.trailerUrl = trailerUrl;
        }

        public Integer getTmdbId() {
            return tmdbId;
        }

        public String getTrailerUrl() {
            return trailerUrl;
        }
    }
}