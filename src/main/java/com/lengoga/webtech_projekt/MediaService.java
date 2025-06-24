package com.lengoga.webtech_projekt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        if (media.getCreatedAt() == null) {
            media.setCreatedAt(LocalDateTime.now());
        }
        if (media.getUpdatedAt() == null) {
            media.setUpdatedAt(LocalDateTime.now());
        }
        return repo.save(media);
    }

    public void deleteMedia(Long id) {
        repo.deleteById(id);
    }

    public Media updateMedia(Long id, Media updatedMedia) {
        Optional<Media> existingMedia = repo.findById(id);
        if (existingMedia.isPresent()) {
            Media media = existingMedia.get();
            media.setTitle(updatedMedia.getTitle());
            media.setGenre(updatedMedia.getGenre());
            media.setWatched(updatedMedia.isWatched());
            media.setType(updatedMedia.getType());

            if (updatedMedia.getTmdbId() != null) {
                media.setTmdbId(updatedMedia.getTmdbId());
            }
            if (updatedMedia.getTrailerUrl() != null) {
                media.setTrailerUrl(updatedMedia.getTrailerUrl());
            }

            if (updatedMedia.getRating() != null ||
                    (updatedMedia.getComment() != null && !updatedMedia.getComment().trim().isEmpty())) {
                media.updateRatingWithDate(updatedMedia.getRating(), updatedMedia.getComment());
            }

            return repo.save(media);
        }
        throw new RuntimeException("Medium mit ID " + id + " nicht gefunden");
    }

    public Media updateRating(Long id, Integer rating, String comment) {
        Optional<Media> existingMedia = repo.findById(id);
        if (existingMedia.isPresent()) {
            Media media = existingMedia.get();
            media.updateRatingWithDate(rating, comment);
            return repo.save(media);
        }
        throw new RuntimeException("Medium mit ID " + id + " nicht gefunden");
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

    public List<Media> getRatedMedias() {
        return repo.findByRatingIsNotNull();
    }

    public List<Media> getTopRatedMedias() {
        return repo.findByRatingIsNotNullOrderByRatingDesc();
    }

    public List<Media> getMediasByRating(Integer rating) {
        return repo.findByRating(rating);
    }

    public List<Media> getMediasWithMinRating(Integer minRating) {
        return repo.findByRatingGreaterThanEqualOrderByRatingDesc(minRating);
    }

    public LocalDateTime getLatestRatingDate() {
        List<Media> ratedMedias = repo.findByRatingIsNotNull();
        return ratedMedias.stream()
                .map(Media::getRatingDate)
                .filter(date -> date != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    public List<Media> filterMedia(List<String> genres, MediaType type) {
        if (genres == null || genres.isEmpty()) {
            if (type == null) {
                return repo.findAll();
            } else {
                return repo.findByType(type);
            }
        } else {
            if (type == null) {
                return repo.findByGenreInIgnoreCase(genres);
            } else {
                return repo.findByGenreInIgnoreCaseAndType(genres, type);
            }
        }
    }

    public Media getMediaById(Long id) {
        Optional<Media> media = repo.findById(id);
        return media.orElse(null);
    }

    public Media saveMedia(Media media) {
        return repo.save(media);
    }
}