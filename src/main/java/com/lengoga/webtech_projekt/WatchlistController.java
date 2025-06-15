package com.lengoga.webtech_projekt;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {"https://meinvueprojekt2.onrender.com"})
@RequestMapping("/watchlist")
public class WatchlistController {

    private final MovieService movieService;

    public WatchlistController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public List<Movie> getWatchlist() {
        return movieService.getAllMovies();
    }

    @PostMapping
    public Movie addMovie(@RequestBody Movie movie) {
        return movieService.addMovie(movie);
    }

    @DeleteMapping("/{id}")
    public void deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
    }

    @GetMapping("/watched")
    public List<Movie> getWatched() {
        return movieService.getWatchedMovies();
    }

    @GetMapping("/unwatched")
    public List<Movie> getUnwatched() {
        return movieService.getUnwatchedMovies();
    }
}