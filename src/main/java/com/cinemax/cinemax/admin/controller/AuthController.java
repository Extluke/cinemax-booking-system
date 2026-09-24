package com.cinemax.cinemax.admin.controller;

import com.cinemax.cinemax.domain.user.PasswordResetToken;
import com.cinemax.cinemax.domain.user.User;
import com.cinemax.cinemax.domain.user.UserService;
import com.cinemax.cinemax.infrastructure.mail.MailService;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;
    private final MailService mailService;

    @Autowired
    public AuthController(UserService userService, MailService mailService) {
        this.userService = userService;
        this.mailService = mailService;
    }

    @PostMapping("/register-process")
    public String processRegistration(
            @RequestParam("namaLengkap") String namaLengkap,
            @RequestParam("email") String email,
            @RequestParam("password") String password) {
        
        if (password == null || password.trim().isEmpty() || password.length() < 6) {
             return "redirect:/register?error=weak_password";
        }
        
        if (userService.findByEmail(email).isPresent()) {
            return "redirect:/register?error=email_exists";
        }

        User newUser = new User();
        newUser.setNamaLengkap(namaLengkap);
        newUser.setEmail(email);
        newUser.setPassword(password);

        userService.registerUser(newUser);

        return "redirect:/login?success=registered";
    }

    @PostMapping("/forgot-password-process")
    public String processForgotPassword(@RequestParam("email") String email) {
        System.out.println("Mencoba mereset password untuk email: " + email);
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isPresent()) {
            System.out.println("User ditemukan di DB. Memproses token...");
            User user = userOptional.get();
            PasswordResetToken token = userService.createPasswordResetTokenForUser(user);
            mailService.sendPasswordResetEmail(user.getEmail(), token.getToken());
            System.out.println("Email reset password berhasil dikirim ke Mailtrap!");
        } else {
            System.out.println("Peringatan: Email " + email + " TIDAK DITEMUKAN di dalam database!");
        }
        // Selalu tampilkan pesan sukses walaupun email tidak ditemukan (security best practice)
        return "redirect:/lupa-password?success=true";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        if (!userService.validatePasswordResetToken(token)) {
            return "redirect:/lupa-password?error=invalid_token";
        }
        model.addAttribute("token", token);
        return "reset_password";
    }

    @PostMapping("/reset-password-process")
    public String processResetPassword(@RequestParam("token") String token, @RequestParam("password") String password) {
        if (!userService.validatePasswordResetToken(token)) {
            return "redirect:/lupa-password?error=invalid_token";
        }
        
        if (password == null || password.trim().isEmpty() || password.length() < 6) {
             return "redirect:/reset-password?token=" + token + "&error=weak_password";
        }
        
        User user = userService.getPasswordResetToken(token).getUser();
        userService.updatePassword(user, password);
        return "redirect:/login?success=password_reset";
    }
}
