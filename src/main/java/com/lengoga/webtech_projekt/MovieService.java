package com.lengoga.webtech_projekt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class MovieService {

    @Autowired
    private final MovieRepository repo;

    public MovieService(MovieRepository movieRepository) {
        this.repo = movieRepository;
    }

    public List<Movie> getAllMovies() {
        return repo.findAll();
    }

    public List<Movie> getWatchedMovies() {
        return repo.findByWatched(true);
    }

    public List<Movie> getUnwatchedMovies() {
        return repo.findByWatched(false);
    }

    public Movie addMovie(Movie movie) {
        return repo.save(movie);
    }

    public void deleteMovie(Long id) {
        repo.deleteById(id);
    }
}
