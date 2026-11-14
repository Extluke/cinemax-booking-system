package com.cinemax.cinemax.admin.controller;

import com.cinemax.cinemax.domain.user.AccountActivationToken;
import com.cinemax.cinemax.domain.user.PasswordResetToken;
import com.cinemax.cinemax.domain.user.User;
import com.cinemax.cinemax.domain.user.UserService;
import com.cinemax.cinemax.infrastructure.mail.MailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        newUser.setEmail(email.trim());
        newUser.setPassword(password);

        userService.registerUser(newUser);

        AccountActivationToken token = userService.createAccountActivationTokenForUser(newUser);
        mailService.sendAccountActivationEmail(newUser.getEmail(), newUser.getNamaLengkap(), token.getToken());

        return "redirect:/login?success=need_activation";
    }

    @GetMapping("/aktivasi-akun")
    public String activateAccount(
            @RequestParam(name = "token", required = false) String token,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        // Bersihkan sesi error login sebelumnya jika ada (aman, tanpa session.invalidate)
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            session.removeAttribute("LAST_LOGIN_ATTEMPT_DISABLED");
        }

        if (token == null || token.trim().isEmpty()) {
            return "redirect:/login?error=invalid_activation_token";
        }

        Optional<User> userOpt = userService.activateAccount(token.trim());
        if (userOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("namaLengkap", userOpt.get().getNamaLengkap());
            return "redirect:/aktivasi-berhasil";
        } else {
            return "redirect:/login?error=invalid_activation_token";
        }
    }

    @GetMapping("/aktivasi-berhasil")
    public String showAktivasiBerhasil(HttpServletRequest request, Model model) {
        // Pembersihan sesi lapis kedua
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            session.removeAttribute("LAST_LOGIN_ATTEMPT_DISABLED");
        }
        return "aktivasi_berhasil";
    }

    @PostMapping("/kirim-ulang-aktivasi")
    public String resendActivation(@RequestParam("email") String email) {
        userService.resendActivationEmail(email);
        return "redirect:/login?success=resend_sent";
    }

    @PostMapping("/forgot-password-process")
    public String processForgotPassword(@RequestParam("email") String email) {
        System.out.println("Mencoba mereset password untuk email: " + email);
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getAuthProvider() == User.AuthProvider.GOOGLE) {
                return "redirect:/lupa-password?error=google_account";
            }
            System.out.println("User ditemukan di DB. Memproses token...");
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
