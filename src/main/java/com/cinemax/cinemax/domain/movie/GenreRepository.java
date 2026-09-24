package com.cinemax.cinemax.domain.movie;

import com.cinemax.cinemax.domain.movie.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    boolean existsByNama(String nama);
}
