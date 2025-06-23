package com.lengoga.webtech_projekt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class MediaService {


    private final MediaRepository repo;

    @Autowired
    public MediaService(MediaRepository mediaRepository) {
        this.repo = mediaRepository;
    }

    public List<Media> getAllMedias() {
        return repo.findAll();
    }

    public List<Media> getWatchedMedias() {
        return repo.findByWatched(true);
    }

    public List<Media> getUnwatchedMedias() {
        return repo.findByWatched(false);
    }

    public Media addMedia(Media media) {
        return repo.save(media);
    }

    public void deleteMedia(Long id) {
        repo.deleteById(id);
    }

    public List<Media> getMediasByType(MediaType type) {
        return repo.findByType(type);
    }

    public List<Media> getSeriesList() {
        return getMediasByType(MediaType.SERIES);
    }

    public List<Media> getMoviesList() {
        return getMediasByType(MediaType.MOVIE);
    }
}
