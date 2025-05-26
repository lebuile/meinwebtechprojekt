package com.lengoga.webtech_projekt;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = {"https://meinvueprojekt2.onrender.com"})
public class WatchlistController {

    @GetMapping("/watchlist")
    public List<Movie> getWatchlist() {
        return List.of(
                new Movie("Inception", "Sci-Fi", false),
                new Movie("The Office", "Comedy", true),
                new Movie("Dark", "Mystery", false)
        );
    }
}