package com.cinemax.cinemax.admin.controller;

import com.cinemax.cinemax.domain.config.BioskopConfig;
import com.cinemax.cinemax.domain.config.BioskopConfigRepository;
import com.cinemax.cinemax.domain.user.User;
import com.cinemax.cinemax.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserRepository userRepository;
    private final BioskopConfigRepository configRepository;

    @Autowired
    public GlobalControllerAdvice(UserRepository userRepository, BioskopConfigRepository configRepository) {
        this.userRepository = userRepository;
        this.configRepository = configRepository;
    }

    @InitBinder("currentUser")
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("*"); // Disallow all binding on currentUser
    }

    @ModelAttribute("currentUser")
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String email = auth.getName();
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    @ModelAttribute("userInitials")
    public String getUserInitials(@ModelAttribute("currentUser") User currentUser) {
        if (currentUser == null || currentUser.getNamaLengkap() == null || currentUser.getNamaLengkap().isEmpty()) {
            return "AD"; // Default Admin
        }
        
        String[] words = currentUser.getNamaLengkap().trim().split("\\s+");
        if (words.length == 1) {
            return String.valueOf(words[0].charAt(0)).toUpperCase();
        } else {
            return (String.valueOf(words[0].charAt(0)) + String.valueOf(words[words.length - 1].charAt(0))).toUpperCase();
        }
    }

    @ModelAttribute("appConfig")
    public BioskopConfig getAppConfig() {
        return configRepository.findById("SINGLETON").orElse(new BioskopConfig());
    }
}
