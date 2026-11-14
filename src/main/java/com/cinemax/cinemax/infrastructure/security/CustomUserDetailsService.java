package com.cinemax.cinemax.infrastructure.security;

import com.cinemax.cinemax.domain.user.User;
import com.cinemax.cinemax.domain.user.UserRepository;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan dengan email: " + email));

        String password = user.getPassword();
        if (password == null || password.isBlank()) {
            // Placeholder password agar akun OAuth2 Google tidak dapat di-bruteforce via form login lokal dan tidak memicu NPE
            password = "{noop}DISABLED_GOOGLE_OAUTH_ACCOUNT_PASSWORD";
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password,
                user.isEnabled(),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}
