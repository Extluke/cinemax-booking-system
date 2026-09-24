package com.cinemax.cinemax.domain.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KelasKursiRepository extends JpaRepository<KelasKursi, Long> {

    java.util.Optional<KelasKursi> findByNamaKelasIgnoreCase(String namaKelas);
}
