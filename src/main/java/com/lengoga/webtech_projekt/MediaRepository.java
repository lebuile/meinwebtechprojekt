package com.lengoga.webtech_projekt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByWatched(boolean watched);
    List<Media> findByType(MediaType type);
    List<Media> findByGenreInIgnoreCase(List<String> genres);
    List<Media> findByGenreInIgnoreCaseAndType(List<String> genres, MediaType type);
}
