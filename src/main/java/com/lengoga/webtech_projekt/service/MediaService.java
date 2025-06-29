package com.lengoga.webtech_projekt.service;

import com.lengoga.webtech_projekt.MediaType;
import com.lengoga.webtech_projekt.model.entity.Media;
import com.lengoga.webtech_projekt.model.entity.User;
import com.lengoga.webtech_projekt.repository.MediaRepository;
import com.lengoga.webtech_projekt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MediaService {

    private final MediaRepository repo;
    private final UserRepository userRepository;

    @Autowired
    public MediaService(MediaRepository mediaRepository, UserRepository userRepository) {
        this.repo = mediaRepository;
        this.userRepository = userRepository;
    }

    // ===== VERBESSERTE BENUTZERSPEZIFISCHE METHODEN =====

    public List<Media> getMediasByUser(Long userId) {
        User user = validateUserWithException(userId);
        System.out.println("✅ Lade alle Medien für User: " + user.getUsername() + " (ID: " + userId + ")");
        return repo.findByUserId(userId);
    }

    public List<Media> getWatchedMediaByUser(Long userId) {
        validateUserWithException(userId);
        return repo.findByUserIdAndWatched(userId, true);
    }

    public List<Media> getUnwatchedMediaByUser(Long userId) {
        validateUserWithException(userId);
        return repo.findByUserIdAndWatched(userId, false);
    }

    public List<Media> getSeriesByUser(Long userId) {
        validateUserWithException(userId);
        return repo.findByUserIdAndType(userId, MediaType.SERIES);
    }

    public List<Media> getMoviesByUser(Long userId) {
        validateUserWithException(userId);
        return repo.findByUserIdAndType(userId, MediaType.MOVIE);
    }

    public List<Media> getRatedMediaByUser(Long userId) {
        User user = validateUserWithException(userId);
        List<Media> ratedMedias = repo.findByUserIdAndRatingIsNotNull(userId);
        System.out.println("✅ Lade bewertete Medien für User: " + user.getUsername() + " - Anzahl: " + ratedMedias.size());
        return ratedMedias;
    }

    public List<Media> getTopRatedMediaByUser(Long userId) {
        validateUserWithException(userId);
        return repo.findByUserIdAndRatingIsNotNullOrderByRatingDesc(userId);
    }

    public List<Media> getMediaByRating(Long userId, Integer rating) {
        validateUserWithException(userId);
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating muss zwischen 1 und 5 liegen");
        }
        return repo.findByUserIdAndRating(userId, rating);
    }

    public List<Media> getMediaByMinRating(Long userId, Integer minRating) {
        validateUserWithException(userId);
        if (minRating < 1 || minRating > 5) {
            throw new IllegalArgumentException("Rating muss zwischen 1 und 5 liegen");
        }
        return repo.findByUserIdAndRatingGreaterThanEqualOrderByRatingDesc(userId, minRating);
    }

    public List<Media> getMediaByGenres(Long userId, List<String> genres) {
        validateUserWithException(userId);
        if (genres == null || genres.isEmpty()) {
            return repo.findByUserId(userId);
        }
        return repo.findByUserIdAndGenreInIgnoreCase(userId, genres);
    }

    public Media addMedia(Media media, Long userId) {
        User user = validateUserWithException(userId);
        media.setUser(user);
        System.out.println("✅ Füge Medium hinzu für User: " + user.getUsername() + " - Titel: " + media.getTitle());
        return repo.save(media);
    }

    public Optional<Media> updateMedia(Long mediaId, Media updatedMedia, Long userId) {
        validateUserWithException(userId);
        Optional<Media> existingMediaOpt = repo.findById(mediaId);

        if (existingMediaOpt.isPresent()) {
            Media existingMedia = existingMediaOpt.get();

            // Sicherheitscheck: Nur der Besitzer kann das Medium bearbeiten
            if (!existingMedia.getUser().getId().equals(userId)) {
                throw new SecurityException("Sie können nur Ihre eigenen Medien bearbeiten");
            }

            // Aktualisiere die Felder
            existingMedia.setTitle(updatedMedia.getTitle());
            existingMedia.setGenre(updatedMedia.getGenre());
            existingMedia.setWatched(updatedMedia.isWatched());
            existingMedia.setType(updatedMedia.getType());

            if (updatedMedia.getTmdbId() != null) {
                existingMedia.setTmdbId(updatedMedia.getTmdbId());
            }
            if (updatedMedia.getTrailerUrl() != null) {
                existingMedia.setTrailerUrl(updatedMedia.getTrailerUrl());
            }

            // Rating und Kommentar aktualisieren
            if (updatedMedia.getRating() != null ||
                    (updatedMedia.getComment() != null && !updatedMedia.getComment().trim().isEmpty())) {
                existingMedia.updateRatingWithDate(updatedMedia.getRating(), updatedMedia.getComment());
            }

            return Optional.of(repo.save(existingMedia));
        }

        return Optional.empty();
    }

    public boolean deleteMedia(Long mediaId, Long userId) {
        validateUserWithException(userId);
        Optional<Media> mediaOpt = repo.findById(mediaId);

        if (mediaOpt.isPresent()) {
            Media media = mediaOpt.get();

            // Sicherheitscheck: Nur der Besitzer kann das Medium löschen
            if (!media.getUser().getId().equals(userId)) {
                throw new SecurityException("Sie können nur Ihre eigenen Medien löschen");
            }

            repo.deleteById(mediaId);
            return true;
        }

        return false;
    }

    // ===== VERBESSERTE HILFSMETHODEN =====

    /**
     * Validiert einen User und wirft eine Exception mit detaillierter Fehlermeldung
     */
    private User validateUserWithException(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID darf nicht null sein");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            // Sammle verfügbare User IDs für bessere Fehlermeldung
            List<User> allUsers = userRepository.findAll();
            String availableIds = allUsers.stream()
                    .map(u -> u.getId() + " (" + u.getUsername() + ")")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("keine");

            String errorMsg = String.format(
                    "❌ User mit ID %d nicht gefunden. Verfügbare User: %s",
                    userId, availableIds
            );

            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        User user = userOpt.get();
        System.out.println("✅ User validiert: " + user.getUsername() + " (ID: " + userId + ")");
        return user;
    }

    /**
     * Einfache User-Validierung (für boolean return)
     */
    public boolean validateUser(Long userId) {
        try {
            validateUserWithException(userId);
            return true;
        } catch (Exception e) {
            System.err.println("User-Validierung fehlgeschlagen: " + e.getMessage());
            return false;
        }
    }

    // ===== LEGACY METHODEN (für Abwärtskompatibilität) =====

    public List<Media> getAllMedias() {
        return repo.findAll();
    }

    public List<Media> getWatchedMedias() {
        return repo.findByWatched(true);
    }

    public List<Media> getUnwatchedMedias() {
        return repo.findByWatched(false);
    }

    public List<Media> getSeriesList() {
        return repo.findByType(MediaType.SERIES);
    }

    public List<Media> getMoviesList() {
        return repo.findByType(MediaType.MOVIE);
    }

    public Media addMedia(Media media) {
        // Diese Methode sollte eigentlich deprecated werden,
        // da sie keinen Benutzer zuweist
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