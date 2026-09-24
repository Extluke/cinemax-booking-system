package com.cinemax.cinemax.domain.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromosiRepository extends JpaRepository<Promosi, Long> {
    List<Promosi> findAllByOrderByTanggalMulaiDesc();
    List<Promosi> findByIsAktifOrderByTanggalMulaiDesc(boolean isAktif);
}
