package com.cinemax.cinemax.admin.controller;

import com.cinemax.cinemax.domain.movie.FilmRepository;
import com.cinemax.cinemax.domain.movie.Genre;
import com.cinemax.cinemax.domain.movie.GenreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class GenreController {

    @Autowired
    private GenreRepository genreRepository;
    
    @Autowired
    private FilmRepository filmRepository;

    @GetMapping("/manajemen-genre")
    public String manajemenGenre(Model model) {
        model.addAttribute("genres", genreRepository.findAll());
        return "admin/manajemen_genre";
    }

    @GetMapping("/tambah-genre")
    public String tambahGenre() {
        return "admin/tambah_genre";
    }

    @PostMapping("/tambah-genre")
    public String prosesTambahGenre(@RequestParam String nama) {
        String trimmed = (nama != null) ? nama.trim() : "";

        // 1. Guard Whitespace / Kosong (Skenario 3)
        if (trimmed.isEmpty()) {
            return "redirect:/admin/tambah-genre?error=empty";
        }

        // 2. Guard Panjang Karakter (Skenario 4)
        if (trimmed.length() > 50) {
            return "redirect:/admin/tambah-genre?error=toolong";
        }

        // 3. Guard SQL Injection & Simbol Berbahaya (Skenario 5)
        String upper = trimmed.toUpperCase();
        if (trimmed.contains(";") || trimmed.contains("'") || trimmed.contains("\"")
                || trimmed.contains("--") || trimmed.contains("/*") || trimmed.contains("*/")
                || trimmed.contains("<") || trimmed.contains(">") || trimmed.contains("\\")
                || upper.contains("DROP TABLE") || upper.contains("SELECT ") || upper.contains("INSERT ")
                || upper.contains("DELETE ") || upper.contains("UPDATE ") || upper.contains("UNION ")
                || upper.contains("OR '1'='1")) {
            return "redirect:/admin/tambah-genre?error=security";
        }

        // 4. Guard Duplikasi Unik (Skenario 2)
        if (genreRepository.existsByNama(trimmed)) {
            return "redirect:/admin/tambah-genre?error=duplicate";
        }

        Genre genre = new Genre(trimmed);
        genreRepository.save(genre);
        return "redirect:/admin/manajemen-genre?success=true";
    }

    @PostMapping("/hapus-genre/{id}")
    public String hapusGenre(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            if (filmRepository.existsByGenresId(id)) {
                return "redirect:/admin/manajemen-genre?error=in_use";
            }
            genreRepository.deleteById(id);
            return "redirect:/admin/manajemen-genre?deleted=true";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage() + " | Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "null"));
            return "redirect:/admin/manajemen-genre?error=unknown";
        }
    }
}
