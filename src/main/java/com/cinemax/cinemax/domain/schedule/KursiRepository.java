package com.cinemax.cinemax.domain.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KursiRepository extends JpaRepository<Kursi, Long> {
    List<Kursi> findByStudioId(Long studioId);
    
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    void deleteByStudioId(Long studioId);
}
