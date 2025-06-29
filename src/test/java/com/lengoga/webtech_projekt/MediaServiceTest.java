package com.lengoga.webtech_projekt;

import com.lengoga.webtech_projekt.model.entity.Media;
import com.lengoga.webtech_projekt.model.entity.User;
import com.lengoga.webtech_projekt.repository.MediaRepository;
import com.lengoga.webtech_projekt.repository.UserRepository;
import com.lengoga.webtech_projekt.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private UserRepository userRepository;

    private MediaService mediaService;
    private User testUser;
    private Media testMovie;

    @BeforeEach
    void setUp() {
        mediaService = new MediaService(mediaRepository, userRepository);

        testUser = new User();
        testUser.setUsername("testuser");
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testMovie = new Media();
        testMovie.setTitle("Test Movie");
        testMovie.setGenre("Action");
        testMovie.setType(MediaType.MOVIE);
        testMovie.setWatched(false);
        testMovie.setUser(testUser);
        ReflectionTestUtils.setField(testMovie, "id", 1L);
    }

    // Test prüft, ob validateUser bei gültiger ID true zurückgibt
    @Test
    void testValidateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        boolean result = mediaService.validateUser(1L);

        assertTrue(result);
        verify(userRepository).findById(1L);
    }

    // Test prüft, ob alle Medien eines Benutzers korrekt geladen werden
    @Test
    void testGetMediasByUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(mediaRepository.findByUserId(1L)).thenReturn(Arrays.asList(testMovie));

        List<Media> result = mediaService.getMediasByUser(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMovie, result.get(0));
        verify(mediaRepository).findByUserId(1L);
    }

    // Test prüft, ob ein neues Medium einem Benutzer zugeordnet und gespeichert wird.
    @Test
    void testAddMedia_Success() {
        Media newMedia = new Media();
        newMedia.setTitle("New Movie");
        newMedia.setType(MediaType.MOVIE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(mediaRepository.save(any(Media.class))).thenReturn(newMedia);

        Media result = mediaService.addMedia(newMedia, 1L);

        assertNotNull(result);
        assertEquals("New Movie", result.getTitle());
        assertEquals(testUser, newMedia.getUser());
        verify(mediaRepository).save(newMedia);
    }

    // Test prüft, ob ein Medium erfolgreich aktualisiert wird
    @Test
    void testUpdateMedia_Success() {
        Media updatedMedia = new Media();
        updatedMedia.setTitle("Updated Title");
        updatedMedia.setGenre("Updated Genre");
        updatedMedia.setWatched(true);
        updatedMedia.setType(MediaType.SERIES);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(testMovie));
        when(mediaRepository.save(any(Media.class))).thenReturn(testMovie);

        Optional<Media> result = mediaService.updateMedia(1L, updatedMedia, 1L);

        assertTrue(result.isPresent());
        assertEquals("Updated Title", testMovie.getTitle());
        assertEquals("Updated Genre", testMovie.getGenre());
        assertTrue(testMovie.isWatched());
        assertEquals(MediaType.SERIES, testMovie.getType());
        verify(mediaRepository).save(testMovie);
    }

    // Test prüft, ob bei Bewertung <1 oder >5 eine IllegalArgumentException kommt.
    @Test
    void testGetMediaByRating_InvalidRating() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> {
            mediaService.getMediaByRating(1L, 0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            mediaService.getMediaByRating(1L, 6);
        });
    }
}