package com.lengoga.webtech_projekt;

import jakarta.persistence.*;

@Entity
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String genre;
    private boolean watched;

    @Enumerated(EnumType.STRING)
    private MediaType type;

    public Media() {
    }

    public Media(String title, String genre, boolean watched) {
        this.title = title;
        this.genre = genre;
        this.watched = watched;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public boolean isWatched() {
        return watched;
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
    }

    public MediaType getType() { return type; }

    public void setType(MediaType type) { this.type = type; }
}
