package com.lengoga.webtech_projekt;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@CrossOrigin(origins = {"https://meinvueprojekt2.onrender.com", "http://localhost:5173"})
@RequestMapping("/watchlist")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
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
        System.out.println("Filme gefunden: " + movies.size());
        movies.forEach(m -> System.out.println(m.getTitle() + " - Typ: " + m.getType()));
        return movies;
    }

    @GetMapping("/series")
    public List<Media> getSeries() {
        List<Media> series = mediaService.getSeriesList();
        System.out.println("Serien gefunden: " + series.size());
        series.forEach(m -> System.out.println(m.getTitle() + " - Typ: " + m.getType()));
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
}