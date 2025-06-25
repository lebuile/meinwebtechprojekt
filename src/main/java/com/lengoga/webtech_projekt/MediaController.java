package com.lengoga.webtech_projekt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/watchlist")
@CrossOrigin
public class MediaController {

    private final MediaService mediaService;
    private final UserService userService;

    @Autowired
    public MediaController(MediaService mediaService, UserService userService) {
        this.mediaService = mediaService;
        this.userService = userService;
    }

    // Benutzerbezogene Endpunkte
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

    // Legacy-Endpunkte für Abwärtskompatibilität oder Admin-Zwecke
    // Diese könnten später entfernt oder mit Berechtigungsprüfungen versehen werden
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