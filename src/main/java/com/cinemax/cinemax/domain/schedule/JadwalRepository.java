/**
 * Tujuan program: Menyediakan kueri jadwal tayang untuk pelanggan dan admin.
 * Contributor: 'Aarif Rahmaan J. Faqiih
 * NIM: 103112430182
 * Role: User
 * Kelas: IF-12-07
 * Terakhir diubah: 27 September 2026, 00:00 WIB
 */
package com.cinemax.cinemax.domain.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JadwalRepository extends JpaRepository<Jadwal, Long> {

    List<Jadwal> findByWaktuMulaiBetweenOrderByWaktuMulaiAsc(LocalDateTime start, LocalDateTime end);

    /**
     * Mengambil jadwal aktif suatu film pada satu hari kalender. Batas waktu
     * akhir bersifat eksklusif sehingga pertunjukan tepat pukul 00.00 esok hari
     * tidak ikut tertampilkan pada tanggal sebelumnya.
     */
    List<Jadwal> findByFilmIdAndWaktuMulaiGreaterThanEqualAndWaktuMulaiLessThanAndStatusOrderByWaktuMulaiAsc(
            Long filmId,
            LocalDateTime waktuMulai,
            LocalDateTime waktuSelesai,
            Jadwal.StatusJadwal status);

    @Query("SELECT j FROM Jadwal j WHERE j.studio.id = :studioId AND (:excludeJadwalId IS NULL OR j.id != :excludeJadwalId) AND j.status != :statusDibatalkan AND j.waktuMulai < :endTime AND j.waktuSelesai > :startTime")
    List<Jadwal> findOverlappingJadwals(@Param("studioId") Long studioId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeJadwalId") Long excludeJadwalId,
            @Param("statusDibatalkan") Jadwal.StatusJadwal statusDibatalkan);

    boolean existsByStudioIdAndWaktuSelesaiAfterAndStatusNot(Long studioId, LocalDateTime time,
            Jadwal.StatusJadwal status);

    /**
     * Menghitung jadwal studio pada rentang waktu yang dipilih dashboard tanpa
     * mengandalkan fungsi DATE spesifik database.
     */
    @Query("SELECT COUNT(j) FROM Jadwal j WHERE j.studio.id = :studioId " +
            "AND j.waktuMulai >= :waktuMulai " +
            "AND j.waktuMulai < :waktuSelesai " +
            "AND j.status != 'DIBATALKAN'")
    long countSchedulesByStudioInPeriod(
            @Param("studioId") Long studioId,
            @Param("waktuMulai") LocalDateTime waktuMulai,
            @Param("waktuSelesai") LocalDateTime waktuSelesai);
}
