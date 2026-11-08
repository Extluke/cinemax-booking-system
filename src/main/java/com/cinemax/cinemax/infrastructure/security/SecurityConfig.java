/**
 * Tujuan program: Mengatur autentikasi serta akses rute publik dan privat CineMax, termasuk webhook pembayaran Midtrans via Cloudflare.
 *
 *
 *
 * Kelas: IF-12-07
 * Terakhir diubah: 20 Oktober 2026, 15:35 WIB
 */
package com.cinemax.cinemax.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

        private final CustomAuthenticationSuccessHandler successHandler;

        public SecurityConfig(CustomAuthenticationSuccessHandler successHandler) {
                this.successHandler = successHandler;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // Pengecualian CSRF khusus untuk webhook notifikasi Midtrans (machine-to-machine)
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/payment/notification"))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/css/**", "/js/**", "/img/**", "/uploads/**")
                                                .permitAll() // Izinkan file statis
                                                .requestMatchers("/", "/login", "/register", "/register-process",
                                                                "/lupa-password",
                                                                "/forgot-password-process", "/reset-password",
                                                                "/reset-password-process", "/jelajah",
                                                                "/film/**", "/pilih-jadwal", "/promo/**")
                                                .permitAll() // Halaman publik
                                                .requestMatchers("/api/payment/notification")
                                                .permitAll() // Webhook notifikasi pembayaran Midtrans via Cloudflare
                                                .requestMatchers("/admin/pengaturan", "/admin/pengaturan/**",
                                                                "/admin/staf", "/admin/staf/**",
                                                                "/admin/audit-log", "/admin/audit-log/**",
                                                                "/admin/laporan", "/admin/laporan/**")
                                                .hasRole("SUPER_ADMIN")
                                                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN") // Khusus admin dan super admin
                                                .anyRequest().authenticated() // Sisanya harus login (Admin tetap bisa akses halaman user)
                                )
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login-process") // Endpoint untuk memproses form (dibuat otomatis oleh Spring Security)
                                                .usernameParameter("email") // Kita login menggunakan email
                                                .passwordParameter("password")
                                                .successHandler(successHandler) // Arahkan sesuai role (Admin ke Dashboard, User ke Beranda)
                                                .failureUrl("/login?error=true")
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/")
                                                .permitAll())
                                .exceptionHandling(ex -> ex
                                                .accessDeniedPage("/error/403") // Kalau user biasa buka halaman admin
                                );

                return http.build();
        }
}
