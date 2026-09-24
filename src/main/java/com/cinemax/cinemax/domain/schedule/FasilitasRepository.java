package com.cinemax.cinemax.domain.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FasilitasRepository extends JpaRepository<Fasilitas, Long> {

    java.util.Optional<Fasilitas> findByNamaIgnoreCase(String nama);
}
