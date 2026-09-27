/**
 * Tujuan program: Menyediakan kueri film publik untuk katalog pelanggan.
 * Contributor: 'Aarif Rahmaan J. Faqiih
 * NIM: 103112430182
 * Role: User
 * Kelas: IF-12-07
 * Terakhir diubah: 27 September 2026, 00:00 WIB
 */
package com.cinemax.cinemax.domain.movie;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FilmRepository extends JpaRepository<Film, Long>, JpaSpecificationExecutor<Film> {

    /**
     * Mengambil film berdasarkan status publik agar halaman pelanggan tidak
     * menampilkan film yang sudah tidak tayang.
     *
     * @param status status publikasi film yang akan ditampilkan
     * @return film yang mempunyai status tersebut, diurutkan berdasarkan judul
     */
    @EntityGraph(attributePaths = "genres")
    List<Film> findByStatusOrderByJudulAsc(Film.StatusFilm status);

    /**
     * Mengambil film bersama genre agar tampilan detail tidak bergantung pada
     * lazy loading setelah controller menyelesaikan transaksi baca.
     */
    @Override
    @EntityGraph(attributePaths = "genres")
    Optional<Film> findById(Long id);

    boolean existsByGenresId(Long genreId);
}
