package com.lengoga.webtech_projekt;

public class Movie {
    private String title;
    private String genre;
    private boolean watched;

    public Movie(String title, String genre, boolean watched) {
        this.title = title;
        this.genre = genre;
        this.watched = watched;
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
}
