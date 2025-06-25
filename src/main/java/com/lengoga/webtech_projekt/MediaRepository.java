package com.lengoga.webtech_projekt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    // Benutzerbezogene Abfragen
    List<Media> findByUserId(Long userId);
    List<Media> findByUserIdAndType(Long userId, MediaType type);
    List<Media> findByUserIdAndWatched(Long userId, boolean watched);

    // Filtern nach Typ für einen bestimmten Benutzer
    List<Media> findByUserIdAndType(Long userId, MediaType type);

    // Genre-Filterung für einen bestimmten Benutzer
    List<Media> findByUserIdAndGenreInIgnoreCase(Long userId, List<String> genres);
    List<Media> findByUserIdAndGenreInIgnoreCaseAndType(Long userId, List<String> genres, MediaType type);

    // Rating-bezogene Abfragen für einen bestimmten Benutzer
    List<Media> findByUserIdAndRatingIsNotNull(Long userId);

    @Query("SELECT m FROM Media m WHERE m.user.id = ?1 AND m.rating IS NOT NULL ORDER BY m.rating DESC")
    List<Media> findByUserIdAndRatingIsNotNullOrderByRatingDesc(Long userId);

    List<Media> findByUserIdAndRating(Long userId, Integer rating);

    @Query("SELECT m FROM Media m WHERE m.user.id = ?1 AND m.rating >= ?2 ORDER BY m.rating DESC")
    List<Media> findByUserIdAndRatingGreaterThanEqualOrderByRatingDesc(Long userId, Integer minRating);

    // Die bisherigen Methoden können als Legacy-Methoden beibehalten werden
    // oder für administrative Zwecke weiterhin genutzt werden
    List<Media> findByWatched(boolean watched);
    List<Media> findByType(MediaType type);
    List<Media> findByGenreInIgnoreCase(List<String> genres);
    List<Media> findByGenreInIgnoreCaseAndType(List<String> genres, MediaType type);
    List<Media> findByRatingIsNotNull();

    @Query("SELECT m FROM Media m WHERE m.rating IS NOT NULL ORDER BY m.rating DESC")
    List<Media> findByRatingIsNotNullOrderByRatingDesc();

    List<Media> findByRating(Integer rating);

    @Query("SELECT m FROM Media m WHERE m.rating >= ?1 ORDER BY m.rating DESC")
    List<Media> findByRatingGreaterThanEqualOrderByRatingDesc(Integer minRating);
}