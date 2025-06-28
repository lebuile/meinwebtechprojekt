package com.lengoga.webtech_projekt.controller;

import com.lengoga.webtech_projekt.model.entity.Media;
import com.lengoga.webtech_projekt.service.MediaService;
import com.lengoga.webtech_projekt.service.TmdbService;
import com.lengoga.webtech_projekt.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/watchlist")
@CrossOrigin
public class MediaController {

    private final MediaService mediaService;
    private final UserService userService;
    private final TmdbService tmdbService;

    @Autowired
    public MediaController(MediaService mediaService, UserService userService, TmdbService tmdbService) {
        this.mediaService = mediaService;
        this.userService = userService;
        this.tmdbService = tmdbService;
    }

    // NEU: WHERE TO WATCH ENDPUNKT
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
                return ResponseEntity.ok(Map.of(
                        "flatrate", new ArrayList<>(),
                        "rent", new ArrayList<>(),
                        "buy", new ArrayList<>(),
                        "link", null,
                        "region", "DE",
                        "message", "Keine Streaming-Informationen verfügbar"
                ));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Laden der Streaming-Informationen: " + e.getMessage()));
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

            return ResponseEntity.ok(Map.of("trailerUrl", trailerUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Laden des Trailers: " + e.getMessage()));
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

            return ResponseEntity.ok(Map.of("similarMedia", similarMedia));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Laden ähnlicher Medien: " + e.getMessage()));
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Abrufen der TMDB-Details: " + e.getMessage()));
        }
    }

    // 4. LATEST RATING DATE ENDPUNKT
    @GetMapping("/{userId}/latest-rating-date")
    public ResponseEntity<?> getLatestRatingDate(@PathVariable Long userId) {
        try {
            // Validiere User
            if (!userService.validateUser(userId)) {
                return ResponseEntity.notFound().build();
            }

            // Hole das neueste Rating-Datum für den User
            List<Media> ratedMedias = mediaService.getRatedMediaByUser(userId);
            String latestRatingDate = ratedMedias.stream()
                    .map(Media::getRatingDate)
                    .filter(date -> date != null)
                    .max((date1, date2) -> date1.compareTo(date2))
                    .map(date -> date.toString())
                    .orElse(null);

            return ResponseEntity.ok(Map.of("latestRatingDate", latestRatingDate));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Laden des letzten Bewertungsdatums: " + e.getMessage()));
        }
    }

    // 5. RATING UPDATE ENDPUNKT
    @PatchMapping("/{userId}/update/{mediaId}/rating")
    public ResponseEntity<?> updateRating(@PathVariable Long userId,
                                          @PathVariable Long mediaId,
                                          @RequestBody Map<String, Object> ratingData) {
        try {
            // Validiere User
            if (!userService.validateUser(userId)) {
                return ResponseEntity.notFound().build();
            }

            Media media = mediaService.getMediaById(mediaId);
            if (media == null) {
                return ResponseEntity.notFound().build();
            }

            // Sicherheitscheck: Nur der Besitzer kann das Medium bewerten
            if (!media.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Sie können nur Ihre eigenen Medien bewerten"));
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Aktualisieren der Bewertung: " + e.getMessage()));
        }
    }

    // ===== BESTEHENDE BENUTZERBEZOGENE ENDPUNKTE =====

    @GetMapping("/{userId}/all")
    public ResponseEntity<List<Media>> getAllMediaByUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(mediaService.getMediasByUser(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{userId}/series")
    public ResponseEntity<List<Media>> getSeriesByUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(mediaService.getSeriesByUser(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{userId}/movies")
    public ResponseEntity<List<Media>> getMoviesByUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(mediaService.getMoviesByUser(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/{userId}/add")
    public ResponseEntity<Media> addMedia(@PathVariable Long userId, @RequestBody Media media) {
        try {
            Media savedMedia = mediaService.addMedia(media, userId);
            return ResponseEntity.ok(savedMedia);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{userId}/update/{mediaId}")
    public ResponseEntity<Media> updateMedia(@PathVariable Long userId,
                                             @PathVariable Long mediaId,
                                             @RequestBody Media updatedMedia) {
        try {
            Optional<Media> result = mediaService.updateMedia(mediaId, updatedMedia, userId);
            return result.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{userId}/delete/{mediaId}")
    public ResponseEntity<Void> deleteMedia(@PathVariable Long userId, @PathVariable Long mediaId) {
        try {
            boolean deleted = mediaService.deleteMedia(mediaId, userId);
            return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{userId}/watched")
    public ResponseEntity<List<Media>> getWatchedMediaByUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(mediaService.getWatchedMediaByUser(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{userId}/unwatched")
    public ResponseEntity<List<Media>> getUnwatchedMediaByUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(mediaService.getUnwatchedMediaByUser(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{userId}/genres")
    public ResponseEntity<List<Media>> getMediaByGenres(@PathVariable Long userId,
                                                        @RequestParam List<String> genres) {
        try {
            return ResponseEntity.ok(mediaService.getMediaByGenres(userId, genres));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{userId}/rated")
    public ResponseEntity<List<Media>> getRatedMediaByUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(mediaService.getRatedMediaByUser(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{userId}/top-rated")
    public ResponseEntity<List<Media>> getTopRatedMediaByUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(mediaService.getTopRatedMediaByUser(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{userId}/rating/{rating}")
    public ResponseEntity<List<Media>> getMediaByRating(@PathVariable Long userId,
                                                        @PathVariable Integer rating) {
        try {
            return ResponseEntity.ok(mediaService.getMediaByRating(userId, rating));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{userId}/rating-min/{minRating}")
    public ResponseEntity<List<Media>> getMediaByMinRating(@PathVariable Long userId,
                                                           @PathVariable Integer minRating) {
        try {
            return ResponseEntity.ok(mediaService.getMediaByMinRating(userId, minRating));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
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