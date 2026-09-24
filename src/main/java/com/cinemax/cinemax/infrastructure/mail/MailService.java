package com.cinemax.cinemax.infrastructure.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Autowired
    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = "http://localhost:8081/reset-password?token=" + token;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@cineplex.com");
        message.setTo(to);
        message.setSubject("Cineplex - Reset Password");
        message.setText("Anda meminta untuk mereset password Anda.\n" +
                "Silakan klik link berikut untuk membuat password baru:\n\n" +
                resetUrl + "\n\n" +
                "Link ini hanya berlaku selama 15 menit.\n" +
                "Jika Anda tidak memintanya, abaikan email ini.");
        
        mailSender.send(message);
    }
}
