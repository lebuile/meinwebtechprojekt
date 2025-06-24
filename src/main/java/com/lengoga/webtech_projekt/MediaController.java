package com.lengoga.webtech_projekt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@CrossOrigin(origins = {"https://meinvueprojekt2.onrender.com", "http://localhost:5173"})
@RequestMapping("/watchlist")
public class MediaController {

    private static final Logger logger = LoggerFactory.getLogger(MediaController.class);

    private final MediaService mediaService;
    private final TmdbService tmdbService;

    public MediaController(MediaService mediaService, TmdbService tmdbService) {
        this.mediaService = mediaService;
        this.tmdbService = tmdbService;
    }

    @GetMapping
    public List<Media> getWatchlist() {
        return mediaService.getAllMedias();
    }

    @PostMapping
    public Media addMedia(@RequestBody Media media) {
        return mediaService.addMedia(media);
    }

    @DeleteMapping("/{id}")
    public void deleteMedia(@PathVariable Long id) {
        mediaService.deleteMedia(id);
    }

    @PutMapping("/{id}")
    public Media updateMedia(@PathVariable Long id, @RequestBody Media media) {
        return mediaService.updateMedia(id, media);
    }

    @GetMapping("/watched")
    public List<Media> getWatched() {
        return mediaService.getWatchedMedias();
    }

    @GetMapping("/unwatched")
    public List<Media> getUnwatched() {
        return mediaService.getUnwatchedMedias();
    }

    @GetMapping("/movies")
    public List<Media> getMovies() {
        List<Media> movies = mediaService.getMoviesList();
        logger.info("Filme gefunden: {}", movies.size());
        movies.forEach(m -> logger.info("{} - Typ: {}", m.getTitle(), m.getType()));
        return movies;
    }

    @GetMapping("/series")
    public List<Media> getSeries() {
        List<Media> series = mediaService.getSeriesList();
        logger.info("Serien gefunden: {}", series.size());
        series.forEach(m -> logger.info("{} - Typ: {}", m.getTitle(), m.getType()));
        return series;
    }

    @GetMapping("/filter")
    public List<Media> filterMedia(
            @RequestParam(required = false) List<String> genres,
            @RequestParam(required = false) MediaType type) {
        return mediaService.filterMedia(genres, type);
    }

    @GetMapping("/rated")
    public List<Media> getRatedMedias() {
        return mediaService.getRatedMedias();
    }

    @PatchMapping("/{id}/rating")
    public Media updateRating(@PathVariable Long id, @RequestBody RatingRequest request) {
        return mediaService.updateRating(id, request.getRating(), request.getComment());
    }

    @GetMapping("/latest-rating-date")
    public LatestRatingResponse getLatestRatingDate() {
        LocalDateTime latestDate = mediaService.getLatestRatingDate();
        return new LatestRatingResponse(latestDate);
    }

    @GetMapping("/{id}/trailer")
    public ResponseEntity<TrailerResponse> getTrailer(@PathVariable Long id) {
        Media media = mediaService.getMediaById(id);
        if (media == null) {
            return ResponseEntity.notFound().build();
        }

        if (media.getTrailerUrl() != null && !media.getTrailerUrl().isEmpty()) {
            return ResponseEntity.ok(new TrailerResponse(media.getTrailerUrl()));
        }

        TmdbService.TmdbSearchResult result = tmdbService.searchMedia(media.getTitle(), media.getType().toString());
        if (result != null && result.getTrailerUrl() != null) {
            media.setTmdbId(result.getTmdbId());
            media.setTrailerUrl(result.getTrailerUrl());
            mediaService.saveMedia(media);
            return ResponseEntity.ok(new TrailerResponse(result.getTrailerUrl()));
        }

        return ResponseEntity.ok(new TrailerResponse(null));
    }

    @PostMapping("/search-tmdb")
    public TmdbSearchResponse searchTmdb(@RequestBody TmdbSearchRequest request) {
        TmdbService.TmdbSearchResult result = tmdbService.searchMedia(request.getTitle(), request.getType());
        if (result != null) {
            return new TmdbSearchResponse(result.getTmdbId(), result.getTrailerUrl(), true);
        }
        return new TmdbSearchResponse(null, null, false);
    }

    public static class RatingRequest {
        private Integer rating;
        private String comment;

        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public static class LatestRatingResponse {
        private LocalDateTime latestRatingDate;

        public LatestRatingResponse(LocalDateTime latestRatingDate) {
            this.latestRatingDate = latestRatingDate;
        }

        public LocalDateTime getLatestRatingDate() { return latestRatingDate; }
        public void setLatestRatingDate(LocalDateTime latestRatingDate) { this.latestRatingDate = latestRatingDate; }
    }

    public static class TrailerResponse {
        private String trailerUrl;

        public TrailerResponse(String trailerUrl) {
            this.trailerUrl = trailerUrl;
        }

        public String getTrailerUrl() {
            return trailerUrl;
        }

        public void setTrailerUrl(String trailerUrl) {
            this.trailerUrl = trailerUrl;
        }
    }

    public static class TmdbSearchRequest {
        private String title;
        private String type;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class TmdbSearchResponse {
        private Integer tmdbId;
        private String trailerUrl;
        private boolean found;

        public TmdbSearchResponse(Integer tmdbId, String trailerUrl, boolean found) {
            this.tmdbId = tmdbId;
            this.trailerUrl = trailerUrl;
            this.found = found;
        }

        public Integer getTmdbId() { return tmdbId; }
        public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }
        public String getTrailerUrl() { return trailerUrl; }
        public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }
        public boolean isFound() { return found; }
        public void setFound(boolean found) { this.found = found; }
    }
}