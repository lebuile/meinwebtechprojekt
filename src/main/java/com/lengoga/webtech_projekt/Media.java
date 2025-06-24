package com.lengoga.webtech_projekt;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String genre;
    private boolean watched;
    private Integer rating;

    @Column(length = 1000)
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "rating_date")
    private LocalDateTime ratingDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private MediaType type;

    @Column(name = "tmdb_id")
    private Integer tmdbId;

    @Column(name = "trailer_url", length = 500)
    private String trailerUrl;

    public Media() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Media(String title, String genre, boolean watched) {
        this.title = title;
        this.genre = genre;
        this.watched = watched;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isWatched() {
        return watched;
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
        this.updatedAt = LocalDateTime.now();
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType type) {
        this.type = type;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating muss zwischen 1 und 5 liegen");
        }
        this.rating = rating;
        this.updatedAt = LocalDateTime.now();

        if (rating != null) {
            this.ratingDate = LocalDateTime.now();
        }
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
        this.updatedAt = LocalDateTime.now();

        if (comment != null && !comment.trim().isEmpty()) {
            this.ratingDate = LocalDateTime.now();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRatingDate() {
        return ratingDate;
    }

    public void setRatingDate(LocalDateTime ratingDate) {
        this.ratingDate = ratingDate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Integer tmdbId) {
        this.tmdbId = tmdbId;
        this.updatedAt = LocalDateTime.now();
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateRatingWithDate(Integer rating, String comment) {
        this.rating = rating;
        this.comment = comment;
        this.ratingDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}