package com.cinemax.cinemax.domain.movie;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipeStudioRepository extends JpaRepository<TipeStudio, Long> {

    java.util.Optional<TipeStudio> findByNamaIgnoreCase(String nama);
}
