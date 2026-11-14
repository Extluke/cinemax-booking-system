package com.cinemax.cinemax.domain.user;

import com.cinemax.cinemax.infrastructure.mail.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final AccountActivationTokenRepository activationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private final ConcurrentMap<String, LocalDateTime> activationResendCooldown = new ConcurrentHashMap<>();

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordResetTokenRepository tokenRepository,
                       AccountActivationTokenRepository activationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    public void registerUser(User user) {
        // Enkripsi password sebelum disimpan
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Set role default jika belum ada
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("ROLE_USER");
        }
        // Registrasi manual akun dibuat dengan status belum aktif (enabled = false)
        user.setEnabled(false);
        user.setAuthProvider(User.AuthProvider.LOCAL);
        userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public AccountActivationToken createAccountActivationTokenForUser(User user) {
        // Hapus token lama jika ada untuk user yang sama
        activationTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        AccountActivationToken activationToken = new AccountActivationToken();
        activationToken.setUser(user);
        activationToken.setToken(token);
        // Masa berlaku 24 jam
        activationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        return activationTokenRepository.save(activationToken);
    }

    @Transactional
    public Optional<User> activateAccount(String token) {
        Optional<AccountActivationToken> optToken = activationTokenRepository.findByToken(token);
        if (optToken.isEmpty()) {
            return Optional.empty();
        }

        AccountActivationToken activationToken = optToken.get();
        if (activationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            activationTokenRepository.delete(activationToken);
            return Optional.empty();
        }

        User user = activationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        activationTokenRepository.delete(activationToken);
        return Optional.of(user);
    }

    @Transactional
    public void resendActivationEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }

        String cleanEmail = email.trim().toLowerCase();
        LocalDateTime lastSent = activationResendCooldown.get(cleanEmail);
        if (lastSent != null && lastSent.plusSeconds(60).isAfter(LocalDateTime.now())) {
            // Jeda minimal 60 detik per email belum terpenuhi
            return;
        }
        activationResendCooldown.put(cleanEmail, LocalDateTime.now());

        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isEnabled()) {
                AccountActivationToken token = createAccountActivationTokenForUser(user);
                mailService.sendAccountActivationEmail(user.getEmail(), user.getNamaLengkap(), token.getToken());
            }
        }
    }

    @Scheduled(cron = "0 0 * * * *") // Bersihkan token kedaluwarsa setiap jam
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        activationTokenRepository.deleteByExpiryDateBefore(now);
        tokenRepository.deleteByExpiryDateBefore(now);
    }

    public PasswordResetToken createPasswordResetTokenForUser(User user) {
        String token = UUID.randomUUID().toString();
        PasswordResetToken myToken = new PasswordResetToken();
        myToken.setUser(user);
        myToken.setToken(token);
        myToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        return tokenRepository.save(myToken);
    }
    
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public PasswordResetToken getPasswordResetToken(String token) {
        return tokenRepository.findByToken(token).orElse(null);
    }

    public boolean validatePasswordResetToken(String token) {
        PasswordResetToken passToken = getPasswordResetToken(token);
        if (passToken == null) {
            return false; // Token tidak ditemukan
        }
        if (passToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(passToken); // Hapus jika kedaluwarsa
            return false;
        }
        return true;
    }
}
