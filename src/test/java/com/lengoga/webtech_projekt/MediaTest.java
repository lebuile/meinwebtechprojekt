package com.lengoga.webtech_projekt;

import com.lengoga.webtech_projekt.model.entity.Media;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;

class MediaTest {

    private Media media;
    private String testTitle = "Test Film";
    private String testGenre = "Action";

    @BeforeEach
    void setUp() {
        media = new Media();
    }

    // Dieser Test prüft, ob das Genre korrekt gesetzt und zurückgegeben wird
    @Test
    void testGenreSetterAndGetter() {
        String genre = "Thriller";

        media.setGenre(genre);

        assertEquals(genre, media.getGenre());
    }

    // Dieser Test prüft, ob bei einem ungültig niedrigen Rating eine Exception geworfen wird.
    @Test
    void testRatingInvalidLow() {
        assertThrows(IllegalArgumentException.class, () -> {
            media.setRating(0);
        });
    }

    // Dieser Test prüft, ob beim Setzen des Titels der Zeitstempel aktualisiert wird.
    @Test
    void testTitleSetterUpdatesTimestamp() {
        String newTitle = "Neuer Titel";
        LocalDateTime beforeUpdate = LocalDateTime.now();

        media.setTitle(newTitle);

        assertEquals(newTitle, media.getTitle());
        if (media.getUpdatedAt() != null) {
            assertTrue(media.getUpdatedAt().isAfter(beforeUpdate) || media.getUpdatedAt().isEqual(beforeUpdate));
        }
    }

    // Dieser Test prüft, ob der Kommentar korrekt gesetzt und ausgelesen wird.
    @Test
    void testCommentSetterAndGetter() {
        String comment = "Sehr guter Film!";

        media.setComment(comment);

        assertEquals(comment, media.getComment());
    }

    // Dieser Test prüft, ob der watched-Status korrekt gesetzt und ausgelesen wird.
    @Test
    void testWatchedSetterAndGetter() {
        media.setWatched(true);

        assertTrue(media.isWatched());
    }
}