package com.lengoga.webtech_projekt.controller;

import com.lengoga.webtech_projekt.model.entity.Media;
import com.lengoga.webtech_projekt.model.entity.User;
import com.lengoga.webtech_projekt.repository.UserRepository;
import com.lengoga.webtech_projekt.service.MediaService;
import com.lengoga.webtech_projekt.service.TmdbService;
import com.lengoga.webtech_projekt.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/watchlist")
@CrossOrigin
public class MediaController {

    private final MediaService mediaService;
    private final UserService userService;
    private final TmdbService tmdbService;
    private final UserRepository userRepository;

    @Autowired
    public MediaController(MediaService mediaService, UserService userService, TmdbService tmdbService, UserRepository userRepository) {
        this.mediaService = mediaService;
        this.userService = userService;
        this.tmdbService = tmdbService;
        this.userRepository = userRepository;
    }

    @GetMapping("/debug/users")
    public ResponseEntity<?> debugUsers() {
        try {
            List<User> users = userRepository.findAll();
            List<Map<String, Object>> userInfo = new ArrayList<>();

            for (User user : users) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("email", user.getEmail());
                userMap.put("mediaCount", user.getMediaList().size());
                userInfo.add(userMap);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("users", userInfo);
            response.put("totalCount", users.size());
            response.put("message", "Verfügbare User IDs");
            response.put("currentTime", java.time.LocalDateTime.now().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/debug/validate-user/{userId}")
    public ResponseEntity<?> validateUser(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            Map<String, Object> response = new HashMap<>();

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("email", user.getEmail());
                userMap.put("mediaCount", user.getMediaList().size());

                response.put("exists", true);
                response.put("user", userMap);
                response.put("message", "User existiert");
            } else {
                List<Map<String, Object>> availableUsers = new ArrayList<>();
                for (User u : userRepository.findAll()) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", u.getId());
                    userMap.put("username", u.getUsername());
                    availableUsers.add(userMap);
                }

                response.put("exists", false);
                response.put("requestedUserId", userId);
                response.put("message", "User existiert nicht");
                response.put("availableUsers", availableUsers);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @GetMapping("/{userId}/latest-rating-date")
    public ResponseEntity<?> getLatestRatingDate(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                List<Map<String, Object>> availableUsers = new ArrayList<>();
                for (User u : userRepository.findAll()) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", u.getId());
                    userMap.put("username", u.getUsername());
                    availableUsers.add(userMap);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("latestRatingDate", null);
                response.put("error", "User mit ID " + userId + " nicht gefunden");
                response.put("availableUsers", availableUsers);

                return ResponseEntity.ok(response);
            }

            // Hole das neueste Rating-Datum für den User
            List<Media> ratedMedias = mediaService.getRatedMediaByUser(userId);
            String latestRatingDate = ratedMedias.stream()
                    .map(Media::getRatingDate)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .map(Object::toString)
                    .orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("latestRatingDate", latestRatingDate);
            response.put("userId", userId);
            response.put("ratedMediaCount", ratedMedias.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Fehler in getLatestRatingDate für User " + userId + ": " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Interner Serverfehler: " + e.getMessage());
            errorResponse.put("latestRatingDate", null);
            errorResponse.put("userId", userId);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ===== WHERE TO WATCH ENDPUNKT =====
    @GetMapping("/{mediaId}/where-to-watch")
    public ResponseEntity<?> getWhereToWatch(@PathVariable Long mediaId) {
        try {
            Media media = mediaService.getMediaById(mediaId);
            if (media == null) {
                return ResponseEntity.notFound().build();
            }

            TmdbService.WhereToWatchResult whereToWatch = null;

            // Wenn Medium bereits eine TMDB ID hat, verwende diese
            if (media.getTmdbId() != null) {
                whereToWatch = tmdbService.getWhereToWatch(media.getTmdbId(), media.getType().toString());
            } else {
                // Ansonsten erst nach Medium suchen
                var searchResult = tmdbService.searchMedia(media.getTitle(), media.getType().toString());
                if (searchResult != null && searchResult.getTmdbId() != null) {
                    // TMDB ID speichern für zukünftige Aufrufe
                    media.setTmdbId(searchResult.getTmdbId());
                    mediaService.saveMedia(media);

                    whereToWatch = tmdbService.getWhereToWatch(searchResult.getTmdbId(), media.getType().toString());
                }
            }

            if (whereToWatch != null) {
                return ResponseEntity.ok(whereToWatch);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("flatrate", new ArrayList<>());
                response.put("rent", new ArrayList<>());
                response.put("buy", new ArrayList<>());
                response.put("link", null);
                response.put("region", "DE");
                response.put("message", "Keine Streaming-Informationen verfügbar");
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Fehler beim Laden der Streaming-Informationen: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ===== BESTEHENDE ENDPUNKTE =====

    // 1. TRAILER ENDPUNKT
    @GetMapping("/{mediaId}/trailer")
    public ResponseEntity<?> getTrailer(@PathVariable Long mediaId) {
        try {
            Media media = mediaService.getMediaById(mediaId);
            if (media == null) {
                return ResponseEntity.notFound().build();
            }

            String trailerUrl = null;

            // Wenn Medium bereits eine Trailer-URL hat, verwende diese
            if (media.getTrailerUrl() != null && !media.getTrailerUrl().trim().isEmpty()) {
                trailerUrl = media.getTrailerUrl();
            } else {
                // Ansonsten von TMDB abrufen
                if (media.getTmdbId() != null) {
                    trailerUrl = tmdbService.getTrailerUrl(media.getTmdbId(), media.getType().toString());

                    // Trailer-URL in Datenbank speichern für zukünftige Aufrufe
                    if (trailerUrl != null) {
                        media.setTrailerUrl(trailerUrl);
                        mediaService.saveMedia(media);
                    }
                } else {
                    // Fallback: Suche nach Titel
                    var searchResult = tmdbService.searchMedia(media.getTitle(), media.getType().toString());
                    if (searchResult != null) {
                        trailerUrl = searchResult.getTrailerUrl();
                        // Optional: TMDB ID auch speichern
                        media.setTmdbId(searchResult.getTmdbId());
                        media.setTrailerUrl(trailerUrl);
                        mediaService.saveMedia(media);
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("trailerUrl", trailerUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Fehler beim Laden des Trailers: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 2. SIMILAR MEDIA ENDPUNKT
    @GetMapping("/{mediaId}/similar")
    public ResponseEntity<?> getSimilarMedia(@PathVariable Long mediaId) {
        try {
            Media media = mediaService.getMediaById(mediaId);
            if (media == null) {
                return ResponseEntity.notFound().build();
            }

            List<TmdbService.SimilarMediaResult> similarMedia = new ArrayList<>();

            if (media.getTmdbId() != null) {
                // Verwende TMDB ID wenn vorhanden
                similarMedia = tmdbService.getSimilarMedia(media.getTmdbId(), media.getType().toString());
            } else {
                // Fallback: Erst nach Medium suchen, dann ähnliche finden
                var searchResult = tmdbService.searchMedia(media.getTitle(), media.getType().toString());
                if (searchResult != null && searchResult.getTmdbId() != null) {
                    // TMDB ID speichern für zukünftige Aufrufe
                    media.setTmdbId(searchResult.getTmdbId());
                    mediaService.saveMedia(media);

                    similarMedia = tmdbService.getSimilarMedia(searchResult.getTmdbId(), media.getType().toString());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("similarMedia", similarMedia);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Fehler beim Laden ähnlicher Medien: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 3. TMDB DETAILS ENDPUNKT
    @GetMapping("/tmdb/{tmdbId}/details")
    public ResponseEntity<?> getTmdbDetails(@PathVariable Integer tmdbId, @RequestParam String type) {
        try {
            TmdbService.TmdbDetailsResult details = tmdbService.getMediaDetails(tmdbId, type);

            if (details == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(details);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Fehler beim Abrufen der TMDB-Details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 4. RATING UPDATE ENDPUNKT
    @PatchMapping("/{userId}/update/{mediaId}/rating")
    public ResponseEntity<?> updateRating(@PathVariable Long userId,
                                          @PathVariable Long mediaId,
                                          @RequestBody Map<String, Object> ratingData) {
        try {
            // Validiere User
            if (!userService.validateUser(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User mit ID " + userId + " nicht gefunden");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            Media media = mediaService.getMediaById(mediaId);
            if (media == null) {
                return ResponseEntity.notFound().build();
            }

            // Sicherheitscheck: Nur der Besitzer kann das Medium bewerten
            if (!media.getUser().getId().equals(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Sie können nur Ihre eigenen Medien bewerten");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            // Rating und Kommentar aus Request extrahieren
            Integer rating = null;
            if (ratingData.get("rating") != null) {
                rating = (Integer) ratingData.get("rating");
            }
            String comment = (String) ratingData.get("comment");

            // Rating aktualisieren
            media.updateRatingWithDate(rating, comment);
            Media updatedMedia = mediaService.saveMedia(media);

            return ResponseEntity.ok(updatedMedia);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Fehler beim Aktualisieren der Bewertung: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ===== BESTEHENDE BENUTZERBEZOGENE ENDPUNKTE =====

    @GetMapping("/{userId}/all")
    public ResponseEntity<?> getAllMediaByUser(@PathVariable Long userId) {
        try {
            if (!userService.validateUser(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User mit ID " + userId + " nicht gefunden");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            return ResponseEntity.ok(mediaService.getMediasByUser(userId));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{userId}/series")
    public ResponseEntity<?> getSeriesByUser(@PathVariable Long userId) {
        try {
            if (!userService.validateUser(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User mit ID " + userId + " nicht gefunden");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            return ResponseEntity.ok(mediaService.getSeriesByUser(userId));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{userId}/movies")
    public ResponseEntity<?> getMoviesByUser(@PathVariable Long userId) {
        try {
            if (!userService.validateUser(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User mit ID " + userId + " nicht gefunden");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            return ResponseEntity.ok(mediaService.getMoviesByUser(userId));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{userId}/add")
    public ResponseEntity<?> addMedia(@PathVariable Long userId, @RequestBody Media media) {
        try {
            if (!userService.validateUser(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User mit ID " + userId + " nicht gefunden");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            Media savedMedia = mediaService.addMedia(media, userId);
            return ResponseEntity.ok(savedMedia);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PutMapping("/{userId}/update/{mediaId}")
    public ResponseEntity<?> updateMedia(@PathVariable Long userId,
                                         @PathVariable Long mediaId,
                                         @RequestBody Media updatedMedia) {
        try {
            if (!userService.validateUser(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User mit ID " + userId + " nicht gefunden");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            Optional<Media> result = mediaService.updateMedia(mediaId, updatedMedia, userId);
            return result.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @DeleteMapping("/{userId}/delete/{mediaId}")
    public ResponseEntity<?> deleteMedia(@PathVariable Long userId, @PathVariable Long mediaId) {
        try {
            if (!userService.validateUser(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User mit ID " + userId + " nicht gefunden");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            boolean deleted = mediaService.deleteMedia(mediaId, userId);
            return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/{userId}/watched")
    public ResponseEntity<?> getWatchedMediaByUser(@PathVariable Long userId) {
        try {
            if (!userService.validateUser(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User mit ID " + userId + " nicht gefunden");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            return ResponseEntity.ok(mediaService.getWatchedMediaByUser(userId));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{userId}/unwatched")
    public ResponseEntity<?> getUnwatchedMediaByUser(@PathVariable Long userId) {
        try {
            if (!userService.validateUser(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User mit ID " + userId + " nicht gefunden");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            return ResponseEntity.ok(mediaService.getUnwatchedMediaByUser(userId));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{userId}/genres")
    public ResponseEntity<?> getMediaByGenres(@PathVariable Long userId,
                                              @RequestParam List<String> genres) {
        try {
            if (!userService.validateUser(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User mit ID " + userId + " nicht gefunden");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            return ResponseEntity.ok(mediaService.getMediaByGenres(userId, genres));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/{userId}/rated")
    public ResponseEntity<?> getRatedMediaByUser(@PathVariable Long userId) {
        try {
            if (!userService.validateUser(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User mit ID " + userId + " nicht gefunden");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            return ResponseEntity.ok(mediaService.getRatedMediaByUser(userId));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{userId}/top-rated")
    public ResponseEntity<?> getTopRatedMediaByUser(@PathVariable Long userId) {
        try {
            if (!userService.validateUser(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User mit ID " + userId + " nicht gefunden");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            return ResponseEntity.ok(mediaService.getTopRatedMediaByUser(userId));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{userId}/rating/{rating}")
    public ResponseEntity<?> getMediaByRating(@PathVariable Long userId,
                                              @PathVariable Integer rating) {
        try {
            if (!userService.validateUser(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User mit ID " + userId + " nicht gefunden");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            return ResponseEntity.ok(mediaService.getMediaByRating(userId, rating));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/{userId}/rating-min/{minRating}")
    public ResponseEntity<?> getMediaByMinRating(@PathVariable Long userId,
                                                 @PathVariable Integer minRating) {
        try {
            if (!userService.validateUser(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User mit ID " + userId + " nicht gefunden");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            return ResponseEntity.ok(mediaService.getMediaByMinRating(userId, minRating));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // ===== LEGACY-ENDPUNKTE FÜR ABWÄRTSKOMPATIBILITÄT =====
    @GetMapping("/all")
    public List<Media> getAllMedia() {
        return mediaService.getAllMedias();
    }

    @GetMapping("/series")
    public List<Media> getSeries() {
        return mediaService.getSeriesList();
    }

    @GetMapping("/movies")
    public List<Media> getMovies() {
        return mediaService.getMoviesList();
    }

    @PostMapping("/add")
    public Media addMedia(@RequestBody Media media) {
        return mediaService.addMedia(media);
    }
}