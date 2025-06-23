package com.lengoga.webtech_projekt;

import org.springframework.web.bind.annotation.*;

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
}