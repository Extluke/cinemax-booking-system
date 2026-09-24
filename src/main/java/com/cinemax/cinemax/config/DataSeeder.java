package com.cinemax.cinemax.config;

import com.cinemax.cinemax.domain.booking.TiketRepository;
import com.cinemax.cinemax.domain.booking.TransaksiRepository;
import com.cinemax.cinemax.domain.movie.FilmRepository;
import com.cinemax.cinemax.domain.schedule.JadwalRepository;
import com.cinemax.cinemax.domain.schedule.StudioRepository;
import com.cinemax.cinemax.domain.user.User;
import com.cinemax.cinemax.domain.user.UserRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            FilmRepository filmRepository,
            StudioRepository studioRepository,
            JadwalRepository jadwalRepository,
            TransaksiRepository transaksiRepository,
            TiketRepository tiketRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Seed Users
            User superAdmin = userRepository.findByEmail("superadmin@cineplex.com").orElseGet(() -> {
                User u = new User();
                u.setNamaLengkap("Super Administrator");
                u.setEmail("superadmin@cineplex.com");
                u.setPassword(passwordEncoder.encode("superadmin123"));
                u.setRole("ROLE_SUPER_ADMIN");
                return userRepository.save(u);
            });
            // Update role if already exists but not super admin
            if (!"ROLE_SUPER_ADMIN".equals(superAdmin.getRole())) {
                superAdmin.setRole("ROLE_SUPER_ADMIN");
                userRepository.save(superAdmin);
            }

            User kasir = userRepository.findByEmail("admin@cineplex.com").orElseGet(() -> {
                User u = new User();
                u.setNamaLengkap("Administrator");
                u.setEmail("admin@cineplex.com");
                u.setPassword(passwordEncoder.encode("admin123"));
                u.setRole("ROLE_ADMIN");
                return userRepository.save(u);
            });        };
    }
}
