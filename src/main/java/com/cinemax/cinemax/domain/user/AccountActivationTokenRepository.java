package com.cinemax.cinemax.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, Long> {

    Optional<AccountActivationToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM AccountActivationToken a WHERE a.user = :user")
    void deleteByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM AccountActivationToken a WHERE a.expiryDate < :now")
    void deleteByExpiryDateBefore(@Param("now") LocalDateTime now);
}

