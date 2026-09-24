package com.cinemax.cinemax.domain.config;

import com.cinemax.cinemax.domain.config.BioskopConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BioskopConfigRepository extends JpaRepository<BioskopConfig, String> {
}
