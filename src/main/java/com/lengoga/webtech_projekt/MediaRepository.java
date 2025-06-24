package com.lengoga.webtech_projekt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
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