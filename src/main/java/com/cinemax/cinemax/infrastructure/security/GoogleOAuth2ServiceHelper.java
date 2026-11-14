package com.cinemax.cinemax.infrastructure.security;

import com.cinemax.cinemax.domain.user.User;
import com.cinemax.cinemax.domain.user.UserRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Component
public class GoogleOAuth2ServiceHelper {

    private final UserRepository userRepository;

    public GoogleOAuth2ServiceHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User processUser(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        if (email == null || email.trim().isEmpty()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_email"), "Email tidak ditemukan dari akun Google.");
        }
        email = email.trim().toLowerCase();

        String name = (String) attributes.get("name");
        String sub = (String) attributes.get("sub");
        String picture = (String) attributes.get("picture");

        Object emailVerifiedObj = attributes.get("email_verified");
        boolean emailVerified = false;
        if (emailVerifiedObj instanceof Boolean) {
            emailVerified = (Boolean) emailVerifiedObj;
        } else if (emailVerifiedObj instanceof String) {
            emailVerified = Boolean.parseBoolean((String) emailVerifiedObj);
        }

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            // a) Email belum ada: buat user baru (enabled = true, authProvider = GOOGLE, role ROLE_USER, simpan googleId dan foto)
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setNamaLengkap(name != null && !name.isBlank() ? name : "Pengguna Google");
            newUser.setPassword(null); // password dibuat null untuk akun Google
            newUser.setRole("ROLE_USER");
            newUser.setEnabled(true);
            newUser.setAuthProvider(User.AuthProvider.GOOGLE);
            newUser.setGoogleId(sub);
            newUser.setProfilePicture(picture);
            return userRepository.save(newUser);
        } else {
            User existingUser = userOptional.get();
            if (existingUser.getAuthProvider() == User.AuthProvider.LOCAL) {
                // b) Email sudah ada sebagai akun LOCAL: tautkan ke akun itu HANYA jika klaim email_verified = true, set enabled = true. Jangan buat duplikat.
                if (!emailVerified) {
                    throw new OAuth2AuthenticationException(new OAuth2Error("email_not_verified"), "Email Google Anda belum diverifikasi.");
                }
                existingUser.setEnabled(true);
                existingUser.setGoogleId(sub);
                if (picture != null && !picture.isBlank()) {
                    existingUser.setProfilePicture(picture);
                }
                return userRepository.save(existingUser);
            } else {
                // Akun GOOGLE yang sudah ada: update googleId dan foto jika ada pembaruan
                existingUser.setEnabled(true);
                existingUser.setGoogleId(sub);
                if (picture != null && !picture.isBlank()) {
                    existingUser.setProfilePicture(picture);
                }
                return userRepository.save(existingUser);
            }
        }
    }
}

