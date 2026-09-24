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
    
    @Query("SELECT j FROM Jadwal j WHERE j.studio.id = :studioId AND (:excludeJadwalId IS NULL OR j.id != :excludeJadwalId) AND j.status != :statusDibatalkan AND j.waktuMulai < :endTime AND j.waktuSelesai > :startTime")
    List<Jadwal> findOverlappingJadwals(@Param("studioId") Long studioId, 
                                        @Param("startTime") LocalDateTime startTime, 
                                        @Param("endTime") LocalDateTime endTime, 
                                        @Param("excludeJadwalId") Long excludeJadwalId,
                                        @Param("statusDibatalkan") Jadwal.StatusJadwal statusDibatalkan);

    boolean existsByStudioIdAndWaktuSelesaiAfterAndStatusNot(Long studioId, LocalDateTime time, Jadwal.StatusJadwal status);

    // Menghitung jadwal tayang hari ini per studio
    @Query("SELECT COUNT(j) FROM Jadwal j WHERE j.studio.id = :studioId AND DATE(j.waktuMulai) = CURRENT_DATE AND j.status != 'DIBATALKAN'")
    long countSchedulesTodayByStudio(@Param("studioId") Long studioId);
}
