package com.lengoga.webtech_projekt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
@SuppressWarnings("unchecked")
public class TmdbService {

    @Value("${tmdb.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://api.themoviedb.org/3";

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

    // NEU: Intelligente ähnliche Medien abrufen
    public List<SimilarMediaResult> getSimilarMedia(Integer tmdbId, String type) {
        if (tmdbId == null) return new ArrayList<>();

        List<SimilarMediaResult> allSimilar = new ArrayList<>();

        List<SimilarMediaResult> recommendations = getRecommendations(tmdbId, type);
        allSimilar.addAll(recommendations);

        List<SimilarMediaResult> keywordBased = getSimilarByKeywords(tmdbId, type);
        allSimilar.addAll(keywordBased);

        if (allSimilar.size() < 4) {
            List<SimilarMediaResult> standardSimilar = getStandardSimilar(tmdbId, type);
            allSimilar.addAll(standardSimilar);
        }

        return removeDuplicatesAndSort(allSimilar);
    }

    // NEU: Detaillierte Medien-Informationen abrufen (inkl. Genre)
    public TmdbDetailsResult getMediaDetails(Integer tmdbId, String type) {
        if (tmdbId == null) return null;

        String endpoint = "/" + (type.equals("MOVIE") ? "movie" : "tv") + "/" + tmdbId;
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + endpoint)
                .queryParam("api_key", apiKey)
                .queryParam("language", "de-DE")
                .toUriString();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                String title = type.equals("MOVIE") ?
                        (String) response.get("title") :
                        (String) response.get("name");

                String overview = (String) response.get("overview");
                String posterPath = (String) response.get("poster_path");
                String posterUrl = posterPath != null ?
                        "https://image.tmdb.org/t/p/w300" + posterPath : null;

                // Genre extrahieren
                String genre = "Unbekannt";
                List<Map<String, Object>> genres = (List<Map<String, Object>>) response.get("genres");
                if (genres != null && !genres.isEmpty()) {
                    genre = (String) genres.get(0).get("name");
                }

                return new TmdbDetailsResult(title, genre, overview, posterUrl);
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen der Details: " + e.getMessage());
        }

        return null;
    }

    private List<SimilarMediaResult> getRecommendations(Integer tmdbId, String type) {
        List<SimilarMediaResult> results = new ArrayList<>();

        try {
            String endpoint = "/" + (type.equals("MOVIE") ? "movie" : "tv") + "/" + tmdbId + "/recommendations";
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + endpoint)
                    .queryParam("api_key", apiKey)
                    .queryParam("language", "de-DE")
                    .queryParam("page", "1")
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> recommendations = (List<Map<String, Object>>) response.get("results");

                int maxResults = Math.min(4, recommendations.size());
                for (int i = 0; i < maxResults; i++) {
                    Map<String, Object> item = recommendations.get(i);
                    SimilarMediaResult similar = mapToSimilarResult(item, type);
                    if (similar != null) {
                        results.add(similar);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler bei Recommendations: " + e.getMessage());
        }

        return results;
    }

    private List<SimilarMediaResult> getSimilarByKeywords(Integer tmdbId, String type) {
        List<SimilarMediaResult> results = new ArrayList<>();

        try {
            List<String> keywords = getMediaKeywords(tmdbId, type);

            if (keywords.isEmpty()) return results;

            List<String> relevantKeywords = keywords.stream()
                    .filter(keyword -> keyword.length() > 3)
                    .filter(keyword -> !keyword.toLowerCase().matches(".*\\d{4}.*"))
                    .limit(2)
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);

            for (String keyword : relevantKeywords) {
                String endpoint = "/discover/" + (type.equals("MOVIE") ? "movie" : "tv");
                String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + endpoint)
                        .queryParam("api_key", apiKey)
                        .queryParam("with_keywords", keyword)
                        .queryParam("language", "de-DE")
                        .queryParam("sort_by", "vote_average.desc")
                        .queryParam("vote_count.gte", "100")
                        .queryParam("page", "1")
                        .toUriString();

                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                if (response != null && response.containsKey("results")) {
                    List<Map<String, Object>> discoveryResults = (List<Map<String, Object>>) response.get("results");

                    int maxPerKeyword = Math.min(2, discoveryResults.size());
                    for (int i = 0; i < maxPerKeyword; i++) {
                        Map<String, Object> item = discoveryResults.get(i);
                        Integer resultId = (Integer) item.get("id");

                        if (!resultId.equals(tmdbId)) {
                            SimilarMediaResult similar = mapToSimilarResult(item, type);
                            if (similar != null) {
                                results.add(similar);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler bei Keywords-Suche: " + e.getMessage());
        }

        return results;
    }

    private List<SimilarMediaResult> getStandardSimilar(Integer tmdbId, String type) {
        List<SimilarMediaResult> results = new ArrayList<>();

        try {
            String endpoint = "/" + (type.equals("MOVIE") ? "movie" : "tv") + "/" + tmdbId + "/similar";
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + endpoint)
                    .queryParam("api_key", apiKey)
                    .queryParam("language", "de-DE")
                    .queryParam("page", "1")
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> similarResults = (List<Map<String, Object>>) response.get("results");

                int maxResults = Math.min(3, similarResults.size());
                for (int i = 0; i < maxResults; i++) {
                    Map<String, Object> item = similarResults.get(i);
                    SimilarMediaResult similar = mapToSimilarResult(item, type);
                    if (similar != null) {
                        results.add(similar);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler bei Standard Similar: " + e.getMessage());
        }

        return results;
    }

    private List<String> getMediaKeywords(Integer tmdbId, String type) {
        List<String> keywords = new ArrayList<>();

        try {
            String endpoint = "/" + (type.equals("MOVIE") ? "movie" : "tv") + "/" + tmdbId + "/keywords";
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + endpoint)
                    .queryParam("api_key", apiKey)
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                List<Map<String, Object>> keywordList = null;

                if (type.equals("MOVIE") && response.containsKey("keywords")) {
                    keywordList = (List<Map<String, Object>>) response.get("keywords");
                } else if (type.equals("SERIES") && response.containsKey("results")) {
                    keywordList = (List<Map<String, Object>>) response.get("results");
                }

                if (keywordList != null) {
                    for (Map<String, Object> keywordObj : keywordList) {
                        String keyword = (String) keywordObj.get("name");
                        if (keyword != null && !keyword.trim().isEmpty()) {
                            keywords.add(keyword);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen der Keywords: " + e.getMessage());
        }

        return keywords;
    }

    private List<SimilarMediaResult> removeDuplicatesAndSort(List<SimilarMediaResult> allSimilar) {
        Map<Integer, SimilarMediaResult> uniqueResults = new LinkedHashMap<>();

        for (SimilarMediaResult result : allSimilar) {
            if (!uniqueResults.containsKey(result.getTmdbId())) {
                uniqueResults.put(result.getTmdbId(), result);
            }
        }

        return uniqueResults.values().stream()
                .sorted((a, b) -> {
                    Double ratingA = a.getRating() != null ? a.getRating() : 0.0;
                    Double ratingB = b.getRating() != null ? b.getRating() : 0.0;
                    return ratingB.compareTo(ratingA);
                })
                .limit(6)
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }

    private SimilarMediaResult mapToSimilarResult(Map<String, Object> item, String type) {
        try {
            Integer id = (Integer) item.get("id");
            String title = type.equals("MOVIE") ?
                    (String) item.get("title") :
                    (String) item.get("name");
            String overview = (String) item.get("overview");
            String posterPath = (String) item.get("poster_path");

            Object voteAverageObj = item.get("vote_average");
            Double voteAverage = null;
            if (voteAverageObj instanceof Number) {
                voteAverage = ((Number) voteAverageObj).doubleValue();
            }

            String posterUrl = posterPath != null ?
                    "https://image.tmdb.org/t/p/w300" + posterPath : null;

            if (title != null && !title.trim().isEmpty()) {
                return new SimilarMediaResult(id, title, overview, posterUrl, voteAverage, type);
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Mappen der Ergebnisse: " + e.getMessage());
        }
        return null;
    }

    public String getTrailerUrl(Integer tmdbId, String type) {
        if (tmdbId == null) return null;

        // Versuche verschiedene Sprachen: Deutsch, Englisch, alle
        String[] languages = {"de-DE", "en-US", ""};

        for (String language : languages) {
            System.out.println("🌍 Suche Trailer für TMDB ID " + tmdbId + " (" + type + ") in Sprache: " +
                    (language.isEmpty() ? "Alle" : language));

            String endpoint = type.equals("MOVIE") ? "/movie/" : "/tv/";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + endpoint + tmdbId + "/videos")
                    .queryParam("api_key", apiKey);

            if (!language.isEmpty()) {
                builder.queryParam("language", language);
            }

            String url = builder.toUriString();

            try {
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                if (response != null && response.containsKey("results")) {
                    List<Map<String, Object>> videos = (List<Map<String, Object>>) response.get("results");

                    System.out.println("📹 Videos gefunden (" + language + "): " + videos.size());

                    if (!videos.isEmpty()) {
                        // Prioritäten für Video-Typen (von höchster zu niedrigster Priorität)
                        String[] preferredTypes = {
                                "Trailer",
                                "Official Trailer",
                                "Teaser",
                                "Clip",
                                "Featurette",
                                "Opening Credits",
                                "Behind the Scenes"
                        };

                        // Versuche die bevorzugten Typen in Reihenfolge
                        for (String preferredType : preferredTypes) {
                            for (Map<String, Object> video : videos) {
                                String site = (String) video.get("site");
                                String videoType = (String) video.get("type");
                                String name = (String) video.get("name");

                                if ("YouTube".equals(site) && preferredType.equals(videoType)) {
                                    String key = (String) video.get("key");
                                    String trailerUrl = "https://www.youtube.com/watch?v=" + key;
                                    System.out.println("✅ " + preferredType + " gefunden (" + language + "): " + name);
                                    return trailerUrl;
                                }
                            }
                        }

                        // Fallback: Nimm das erste YouTube Video, egal welcher Typ
                        for (Map<String, Object> video : videos) {
                            String site = (String) video.get("site");
                            if ("YouTube".equals(site)) {
                                String key = (String) video.get("key");
                                String name = (String) video.get("name");
                                String videoType = (String) video.get("type");
                                String trailerUrl = "https://www.youtube.com/watch?v=" + key;
                                System.out.println("🔄 Fallback Video verwendet (" + language + "): " + name + " (" + videoType + ")");
                                return trailerUrl;
                            }
                        }

                        System.out.println("❌ Keine YouTube Videos in " + language);
                    } else {
                        System.out.println("❌ Keine Videos verfügbar in " + language);
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Fehler bei Sprache " + language + ": " + e.getMessage());
            }
        }

        System.out.println("❌ Kein Trailer gefunden für " + type + " ID " + tmdbId + " in allen Sprachen");
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

    public static class TmdbDetailsResult {
        private String title;
        private String genre;
        private String overview;
        private String posterUrl;

        public TmdbDetailsResult(String title, String genre, String overview, String posterUrl) {
            this.title = title;
            this.genre = genre;
            this.overview = overview;
            this.posterUrl = posterUrl;
        }

        public String getTitle() { return title; }
        public String getGenre() { return genre; }
        public String getOverview() { return overview; }
        public String getPosterUrl() { return posterUrl; }

        public void setTitle(String title) { this.title = title; }
        public void setGenre(String genre) { this.genre = genre; }
        public void setOverview(String overview) { this.overview = overview; }
        public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    }

    public static class SimilarMediaResult {
        private Integer tmdbId;
        private String title;
        private String overview;
        private String posterUrl;
        private Double rating;
        private String type;

        public SimilarMediaResult(Integer tmdbId, String title, String overview,
                                  String posterUrl, Double rating, String type) {
            this.tmdbId = tmdbId;
            this.title = title;
            this.overview = overview;
            this.posterUrl = posterUrl;
            this.rating = rating;
            this.type = type;
        }

        public Integer getTmdbId() { return tmdbId; }
        public String getTitle() { return title; }
        public String getOverview() { return overview; }
        public String getPosterUrl() { return posterUrl; }
        public Double getRating() { return rating; }
        public String getType() { return type; }

        public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }
        public void setTitle(String title) { this.title = title; }
        public void setOverview(String overview) { this.overview = overview; }
        public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
        public void setRating(Double rating) { this.rating = rating; }
        public void setType(String type) { this.type = type; }
    }
}