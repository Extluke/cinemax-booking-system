package com.cinemax.cinemax.infrastructure.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    @Autowired
    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = baseUrl + "/reset-password?token=" + token;
        
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

    @Async
    public void sendAccountActivationEmail(String toEmail, String namaLengkap, String token) {
        String activationUrl = baseUrl + "/aktivasi-akun?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@cineplex.com");
        message.setTo(toEmail);
        message.setSubject("Aktivasi Akun Cineplex Anda");
        message.setText("Halo " + namaLengkap + ",\n\n" +
                "Terima kasih telah mendaftar di Cineplex Booking System.\n" +
                "Untuk mengaktifkan akun Anda, silakan klik tautan di bawah ini:\n\n" +
                activationUrl + "\n\n" +
                "Tautan aktivasi ini berlaku selama 24 jam.\n" +
                "Jika Anda tidak merasa mendaftar di Cineplex, silakan abaikan email ini.\n\n" +
                "Salam hangat,\nTim Cineplex");

        mailSender.send(message);
    }

    @Async
    public void sendTransactionOtpEmail(String toEmail, String orderNumber, String otpCode, String movieTitle, double totalHarga) {
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        rupiahFormat.setMaximumFractionDigits(0);
        String formattedHarga = rupiahFormat.format(totalHarga);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@cineplex.com");
        message.setTo(toEmail);
        message.setSubject("Kode OTP Transaksi Cineplex - " + orderNumber);
        message.setText("Halo Pelanggan Cineplex,\n\n" +
                "Berikut adalah rincian pesanan dan kode OTP untuk otorisasi transaksi Anda:\n\n" +
                "Nomor Pesanan : " + orderNumber + "\n" +
                "Film          : " + movieTitle + "\n" +
                "Total Tagihan : " + formattedHarga + "\n\n" +
                "-----------------------------------------\n" +
                "KODE OTP TRANSAKSI: " + otpCode + "\n" +
                "-----------------------------------------\n\n" +
                "PENTING: Kode ini hanya berlaku selama 10 menit.\n" +
                "PERINGATAN KEAMANAN: Jangan pernah membagikan kode OTP ini kepada siapa pun, termasuk pihak yang mengaku sebagai staf atau admin Cineplex.\n\n" +
                "Salam hangat,\nTim Cineplex");

        mailSender.send(message);
    }
}
