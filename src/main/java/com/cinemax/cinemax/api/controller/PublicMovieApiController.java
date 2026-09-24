package com.cinemax.cinemax.api.controller;

import com.cinemax.cinemax.domain.movie.Film;
import com.cinemax.cinemax.domain.movie.FilmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint publik (tanpa autentikasi) untuk digunakan oleh Frontend Developer (Teman User).
 * Blueprint ini dapat dijadikan acuan.
 */
@RestController
@RequestMapping("/api/v1/movies")
public class PublicMovieApiController {

    @Autowired
    private FilmRepository filmRepository;

    @GetMapping
    public ResponseEntity<List<Film>> getAllMovies() {
        // Mengembalikan daftar film beserta HTTP 200 OK
        List<Film> films = filmRepository.findAll();
        return ResponseEntity.ok(films);
    }
}
