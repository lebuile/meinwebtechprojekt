package com.lengoga.webtech_projekt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TmdbService {

    @Value("${tmdb.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://api.themoviedb.org/3";

    // ===== WHERE TO WATCH FUNKTIONALITÄT =====
    public WhereToWatchResult getWhereToWatch(Integer tmdbId, String type) {
        if (tmdbId == null) return null;

        String endpoint = "/" + (type.equals("MOVIE") ? "movie" : "tv") + "/" + tmdbId + "/watch/providers";
        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + endpoint)
                .queryParam("api_key", apiKey)
                .build()
                .toUri();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response != null && response.containsKey("results")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> results = (Map<String, Object>) response.get("results");

                // Priorität: Deutschland, dann Österreich, dann USA als Fallback
                String[] regions = {"DE", "AT", "US"};

                for (String region : regions) {
                    if (results.containsKey(region)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> regionData = (Map<String, Object>) results.get(region);
                        WhereToWatchResult result = parseWatchProviders(regionData, region);
                        if (result != null && hasProviders(result)) {
                            return result;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen der Watch Provider: " + e.getMessage());
        }

        return null;
    }

    private boolean hasProviders(WhereToWatchResult result) {
        return !result.getFlatrate().isEmpty() ||
                !result.getRent().isEmpty() ||
                !result.getBuy().isEmpty();
    }

    private WhereToWatchResult parseWatchProviders(Map<String, Object> regionData, String region) {
        List<StreamingProvider> flatrate = parseProviderList(regionData, "flatrate");
        List<StreamingProvider> rent = parseProviderList(regionData, "rent");
        List<StreamingProvider> buy = parseProviderList(regionData, "buy");
        String link = (String) regionData.get("link");

        return new WhereToWatchResult(flatrate, rent, buy, link, region);
    }

    private List<StreamingProvider> parseProviderList(Map<String, Object> regionData, String key) {
        List<StreamingProvider> providers = new ArrayList<>();

        if (regionData.containsKey(key)) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> providerList = (List<Map<String, Object>>) regionData.get(key);
            for (Map<String, Object> provider : providerList) {
                providers.add(mapToStreamingProvider(provider));
            }
        }

        return providers;
    }

    private StreamingProvider mapToStreamingProvider(Map<String, Object> providerData) {
        Integer providerId = (Integer) providerData.get("provider_id");
        String providerName = (String) providerData.get("provider_name");
        String logoPath = (String) providerData.get("logo_path");
        String logoUrl = logoPath != null ? "https://image.tmdb.org/t/p/w92" + logoPath : null;

        return new StreamingProvider(providerId, providerName, logoUrl);
    }

    // ===== BESTEHENDE FUNKTIONALITÄTEN =====
    public TmdbSearchResult searchMedia(String title, String type) {
        String endpoint = type.equals("MOVIE") ? "/search/movie" : "/search/tv";

        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + endpoint)
                .queryParam("api_key", apiKey)
                .queryParam("query", title)
                .queryParam("language", "de-DE")
                .build()
                .toUri();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response != null && response.containsKey("results")) {
                @SuppressWarnings("unchecked")
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

    public List<SimilarMediaResult> getSimilarMedia(Integer tmdbId, String type) {
        if (tmdbId == null) return new ArrayList<>();

        List<SimilarMediaResult> allSimilar = new ArrayList<>();

        allSimilar.addAll(getRecommendations(tmdbId, type));
        allSimilar.addAll(getSimilarByKeywords(tmdbId, type));

        if (allSimilar.size() < 4) {
            allSimilar.addAll(getStandardSimilar(tmdbId, type));
        }

        return removeDuplicatesAndSort(allSimilar);
    }

    public TmdbDetailsResult getMediaDetails(Integer tmdbId, String type) {
        if (tmdbId == null) return null;

        String endpoint = "/" + (type.equals("MOVIE") ? "movie" : "tv") + "/" + tmdbId;
        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + endpoint)
                .queryParam("api_key", apiKey)
                .queryParam("language", "de-DE")
                .build()
                .toUri();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response != null) {
                String title = type.equals("MOVIE") ?
                        (String) response.get("title") :
                        (String) response.get("name");

                String overview = (String) response.get("overview");
                String posterPath = (String) response.get("poster_path");
                String posterUrl = posterPath != null ?
                        "https://image.tmdb.org/t/p/w300" + posterPath : null;

                String genre = extractGenre(response);

                return new TmdbDetailsResult(title, genre, overview, posterUrl);
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen der Details: " + e.getMessage());
        }

        return null;
    }

    private String extractGenre(Map<String, Object> response) {
        String genre = "Unbekannt";
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> genres = (List<Map<String, Object>>) response.get("genres");
        if (genres != null && !genres.isEmpty()) {
            genre = (String) genres.get(0).get("name");
        }
        return genre;
    }

    private List<SimilarMediaResult> getRecommendations(Integer tmdbId, String type) {
        return getSimilarMediaFromEndpoint(tmdbId, type, "recommendations", 4);
    }

    private List<SimilarMediaResult> getStandardSimilar(Integer tmdbId, String type) {
        return getSimilarMediaFromEndpoint(tmdbId, type, "similar", 3);
    }

    private List<SimilarMediaResult> getSimilarMediaFromEndpoint(Integer tmdbId, String type, String endpoint, int maxResults) {
        List<SimilarMediaResult> results = new ArrayList<>();

        try {
            String path = "/" + (type.equals("MOVIE") ? "movie" : "tv") + "/" + tmdbId + "/" + endpoint;
            URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + path)
                    .queryParam("api_key", apiKey)
                    .queryParam("language", "de-DE")
                    .queryParam("page", "1")
                    .build()
                    .toUri();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response != null && response.containsKey("results")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> mediaResults = (List<Map<String, Object>>) response.get("results");

                int limit = Math.min(maxResults, mediaResults.size());
                for (int i = 0; i < limit; i++) {
                    Map<String, Object> item = mediaResults.get(i);
                    SimilarMediaResult similar = mapToSimilarResult(item, type);
                    if (similar != null) {
                        results.add(similar);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler bei " + endpoint + ": " + e.getMessage());
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
                    .collect(Collectors.toList());

            for (String keyword : relevantKeywords) {
                results.addAll(searchByKeyword(keyword, type, tmdbId));
            }
        } catch (Exception e) {
            System.err.println("Fehler bei Keywords-Suche: " + e.getMessage());
        }

        return results;
    }

    private List<SimilarMediaResult> searchByKeyword(String keyword, String type, Integer excludeId) {
        List<SimilarMediaResult> results = new ArrayList<>();

        try {
            String endpoint = "/discover/" + (type.equals("MOVIE") ? "movie" : "tv");
            URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + endpoint)
                    .queryParam("api_key", apiKey)
                    .queryParam("with_keywords", keyword)
                    .queryParam("language", "de-DE")
                    .queryParam("sort_by", "vote_average.desc")
                    .queryParam("vote_count.gte", "100")
                    .queryParam("page", "1")
                    .build()
                    .toUri();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response != null && response.containsKey("results")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> discoveryResults = (List<Map<String, Object>>) response.get("results");

                int maxPerKeyword = Math.min(2, discoveryResults.size());
                for (int i = 0; i < maxPerKeyword; i++) {
                    Map<String, Object> item = discoveryResults.get(i);
                    Integer resultId = (Integer) item.get("id");

                    if (!resultId.equals(excludeId)) {
                        SimilarMediaResult similar = mapToSimilarResult(item, type);
                        if (similar != null) {
                            results.add(similar);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler bei Keyword-Suche für '" + keyword + "': " + e.getMessage());
        }

        return results;
    }

    private List<String> getMediaKeywords(Integer tmdbId, String type) {
        List<String> keywords = new ArrayList<>();

        try {
            String endpoint = "/" + (type.equals("MOVIE") ? "movie" : "tv") + "/" + tmdbId + "/keywords";
            URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + endpoint)
                    .queryParam("api_key", apiKey)
                    .build()
                    .toUri();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response != null) {
                List<Map<String, Object>> keywordList = getKeywordList(response, type);

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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getKeywordList(Map<String, Object> response, String type) {
        if (type.equals("MOVIE") && response.containsKey("keywords")) {
            return (List<Map<String, Object>>) response.get("keywords");
        } else if (type.equals("SERIES") && response.containsKey("results")) {
            return (List<Map<String, Object>>) response.get("results");
        }
        return null;
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
                .collect(Collectors.toList());
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

        String[] languages = {"de-DE", "en-US", ""};

        for (String language : languages) {
            String trailerUrl = searchTrailerInLanguage(tmdbId, type, language);
            if (trailerUrl != null) {
                return trailerUrl;
            }
        }

        System.out.println("❌ Kein Trailer gefunden für " + type + " ID " + tmdbId + " in allen Sprachen");
        return null;
    }

    private String searchTrailerInLanguage(Integer tmdbId, String type, String language) {
        System.out.println("🌍 Suche Trailer für TMDB ID " + tmdbId + " (" + type + ") in Sprache: " +
                (language.isEmpty() ? "Alle" : language));

        String endpoint = type.equals("MOVIE") ? "/movie/" : "/tv/";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + endpoint + tmdbId + "/videos")
                .queryParam("api_key", apiKey);

        if (!language.isEmpty()) {
            builder.queryParam("language", language);
        }

        URI uri = builder.build().toUri();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response != null && response.containsKey("results")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> videos = (List<Map<String, Object>>) response.get("results");

                System.out.println("📹 Videos gefunden (" + language + "): " + videos.size());

                if (!videos.isEmpty()) {
                    String trailerUrl = findBestTrailer(videos, language);
                    if (trailerUrl != null) {
                        return trailerUrl;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Fehler bei Sprache " + language + ": " + e.getMessage());
        }

        return null;
    }

    private String findBestTrailer(List<Map<String, Object>> videos, String language) {
        String[] preferredTypes = {
                "Trailer", "Official Trailer", "Teaser", "Clip",
                "Featurette", "Opening Credits", "Behind the Scenes"
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

        // Fallback: Nimm das erste YouTube Video
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

        return null;
    }

    // ===== DATENKLASSEN (kompakte Implementierung) =====

    public static final class TmdbSearchResult {
        private final Integer tmdbId;
        private final String trailerUrl;

        public TmdbSearchResult(Integer tmdbId, String trailerUrl) {
            this.tmdbId = tmdbId;
            this.trailerUrl = trailerUrl;
        }

        public Integer getTmdbId() { return tmdbId; }
        public String getTrailerUrl() { return trailerUrl; }
    }

    public static final class TmdbDetailsResult {
        private final String title;
        private final String genre;
        private final String overview;
        private final String posterUrl;

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
    }

    public static final class SimilarMediaResult {
        private final Integer tmdbId;
        private final String title;
        private final String overview;
        private final String posterUrl;
        private final Double rating;
        private final String type;

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
    }

    public static final class WhereToWatchResult {
        private final List<StreamingProvider> flatrate;
        private final List<StreamingProvider> rent;
        private final List<StreamingProvider> buy;
        private final String link;
        private final String region;

        public WhereToWatchResult(List<StreamingProvider> flatrate, List<StreamingProvider> rent,
                                  List<StreamingProvider> buy, String link, String region) {
            this.flatrate = flatrate != null ? new ArrayList<>(flatrate) : new ArrayList<>();
            this.rent = rent != null ? new ArrayList<>(rent) : new ArrayList<>();
            this.buy = buy != null ? new ArrayList<>(buy) : new ArrayList<>();
            this.link = link;
            this.region = region;
        }

        public List<StreamingProvider> getFlatrate() { return new ArrayList<>(flatrate); }
        public List<StreamingProvider> getRent() { return new ArrayList<>(rent); }
        public List<StreamingProvider> getBuy() { return new ArrayList<>(buy); }
        public String getLink() { return link; }
        public String getRegion() { return region; }
    }

    public static final class StreamingProvider {
        private final Integer providerId;
        private final String providerName;
        private final String logoUrl;

        public StreamingProvider(Integer providerId, String providerName, String logoUrl) {
            this.providerId = providerId;
            this.providerName = providerName;
            this.logoUrl = logoUrl;
        }

        public Integer getProviderId() { return providerId; }
        public String getProviderName() { return providerName; }
        public String getLogoUrl() { return logoUrl; }
    }
}