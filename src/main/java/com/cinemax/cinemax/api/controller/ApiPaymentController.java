package com.cinemax.cinemax.api.controller;

import com.cinemax.cinemax.api.dto.CheckoutRequestDTO;
import com.cinemax.cinemax.domain.booking.Tiket;
import com.cinemax.cinemax.domain.booking.TiketRepository;
import com.cinemax.cinemax.domain.booking.Transaksi;
import com.cinemax.cinemax.domain.booking.TransaksiRepository;
import com.cinemax.cinemax.domain.config.BioskopConfig;
import com.cinemax.cinemax.domain.config.BioskopConfigRepository;
import com.cinemax.cinemax.domain.schedule.Jadwal;
import com.cinemax.cinemax.domain.schedule.JadwalRepository;
import com.cinemax.cinemax.domain.user.User;
import com.cinemax.cinemax.domain.user.UserRepository;
import com.cinemax.cinemax.infrastructure.mail.MailService;
import com.cinemax.cinemax.infrastructure.payment.PaymentGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
public class ApiPaymentController {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private JadwalRepository jadwalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransaksiRepository transaksiRepository;

    @Autowired
    private TiketRepository tiketRepository;

    @Autowired
    private PaymentGateway paymentGateway;

    @Autowired
    private BioskopConfigRepository configRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MailService mailService;

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequestDTO request) {
        try {
            // Validasi Jadwal
            Jadwal jadwal = jadwalRepository.findById(request.getJadwalId())
                    .orElseThrow(() -> new Exception("Jadwal tidak ditemukan"));

            // Mencegah IDOR: Ambil email user yang sedang login dari session
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new Exception("User tidak ditemukan/belum login"));

            // Validasi Kursi
            if (request.getKursiKode() == null || request.getKursiKode().isEmpty()) {
                throw new Exception("Kursi belum dipilih");
            }

            // Proteksi Race Condition & Double Booking
            java.util.List<Tiket.StatusTiket> excludedStatuses = java.util.Arrays.asList(Tiket.StatusTiket.REFUNDED);
            for (String kodeKursi : request.getKursiKode()) {
                boolean isBooked = tiketRepository.isSeatBooked(jadwal, kodeKursi, excludedStatuses);
                if (isBooked) {
                    throw new Exception("Maaf, kursi " + kodeKursi + " baru saja dipesan oleh orang lain. Silakan pilih kursi lain.");
                }
            }

            // Hitung harga
            double totalHarga = jadwal.getHarga() * request.getKursiKode().size();

            // Buat Transaksi
            Transaksi transaksi = new Transaksi();
            transaksi.setNomorPesanan("ORDER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            transaksi.setPelanggan(user);
            transaksi.setTanggalTransaksi(LocalDateTime.now());
            transaksi.setTotalHarga(totalHarga);
            transaksi.setStatus(Transaksi.StatusTransaksi.PENDING);
            transaksi.setRefundStatus(Transaksi.RefundStatus.NONE);

            // Generate OTP dengan SecureRandom
            String otpCode = String.format("%06d", RANDOM.nextInt(1_000_000));
            // Simpan HASH-nya (BCrypt) ke database, bukan teks asli
            transaksi.setOtpHash(passwordEncoder.encode(otpCode));
            transaksi.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
            transaksi.setOtpAttempts(0);
            transaksi.setOtpVerified(false);
            transaksi.setOtpLastSentAt(LocalDateTime.now());

            transaksiRepository.save(transaksi);

            // Buat Tiket untuk setiap kursi
            for (String kodeKursi : request.getKursiKode()) {
                Tiket tiket = new Tiket();
                tiket.setTransaksi(transaksi);
                tiket.setJadwal(jadwal);
                tiket.setNomorKursi(kodeKursi);
                tiket.setHarga(jadwal.getHarga());
                tiket.setStatus(Tiket.StatusTiket.VALID);
                tiketRepository.save(tiket);
            }

            // Kirim email OTP Transaksi secara async
            mailService.sendTransactionOtpEmail(
                    user.getEmail(),
                    transaksi.getNomorPesanan(),
                    otpCode,
                    jadwal.getFilm().getJudul(),
                    transaksi.getTotalHarga()
            );

            // Response (tanpa paymentUrl karena token Midtrans belum dibuat sebelum OTP terverifikasi)
            Map<String, Object> response = new HashMap<>();
            response.put("transaksiId", transaksi.getId());
            response.put("nomorPesanan", transaksi.getNomorPesanan());
            response.put("totalHarga", transaksi.getTotalHarga());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, Object> payload) {
        try {
            Object transaksiIdObj = payload.get("transaksiId");
            Object otpCodeObj = payload.get("otpCode");

            if (transaksiIdObj == null || otpCodeObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "transaksiId dan otpCode wajib diisi."));
            }

            Long transaksiId = Long.valueOf(transaksiIdObj.toString());
            String otpCode = otpCodeObj.toString().trim();

            Transaksi transaksi = transaksiRepository.findById(transaksiId)
                    .orElseThrow(() -> new Exception("Transaksi tidak ditemukan"));

            // 1. Pastikan transaksi milik user yang login, jika bukan balas 403 (cegah IDOR)
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            if (transaksi.getPelanggan() == null || !transaksi.getPelanggan().getEmail().equalsIgnoreCase(currentUserEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Akses ditolak: transaksi ini bukan milik Anda."));
            }

            // Jika OTP sudah diverifikasi sebelumnya
            if (transaksi.isOtpVerified()) {
                String snapToken = "";
                try {
                    snapToken = paymentGateway.generatePaymentUrl(transaksi);
                } catch (Exception ex) {
                    System.err.println("Peringatan generate snap token: " + ex.getMessage());
                }
                Map<String, Object> resp = new HashMap<>();
                resp.put("message", "OTP sudah terverifikasi sebelumnya.");
                resp.put("otpVerified", true);
                if (snapToken != null && !snapToken.isBlank()) {
                    resp.put("snapToken", snapToken);
                    resp.put("paymentUrl", snapToken);
                }
                return ResponseEntity.ok(resp);
            }

            // 2. Tolak jika otpAttempts >= 5 atau OTP kedaluwarsa
            if (transaksi.getOtpAttempts() >= 5) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Kode OTP telah hangus karena batas percobaan (5 kali) telah habis. Silakan kirim ulang kode OTP.",
                        "attemptsExceeded", true,
                        "remainingAttempts", 0
                ));
            }

            if (transaksi.getOtpExpiry() == null || transaksi.getOtpExpiry().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Kode OTP telah kedaluwarsa. Silakan kirim ulang kode OTP.",
                        "expired", true
                ));
            }

            // 3. Bandingkan hash, jika salah naikkan otpAttempts dan kembalikan sisa percobaan
            if (transaksi.getOtpHash() == null || !passwordEncoder.matches(otpCode, transaksi.getOtpHash())) {
                int attempts = transaksi.getOtpAttempts() + 1;
                transaksi.setOtpAttempts(attempts);
                transaksiRepository.save(transaksi);

                int remaining = Math.max(0, 5 - attempts);
                if (remaining == 0) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Kode OTP salah. Batas percobaan (5 kali) telah habis. OTP telah hangus.",
                            "remainingAttempts", 0,
                            "attemptsExceeded", true
                    ));
                }
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Kode OTP salah. Sisa percobaan: " + remaining,
                        "remainingAttempts", remaining
                ));
            }

            // 4. Jika benar set otpVerified=true dan hapus hash
            transaksi.setOtpVerified(true);
            transaksi.setOtpHash(null);
            transaksiRepository.save(transaksi);

            String snapToken = "";
            try {
                snapToken = paymentGateway.generatePaymentUrl(transaksi);
            } catch (Exception ex) {
                System.err.println("Peringatan generate snap token: " + ex.getMessage());
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("message", "Kode OTP berhasil diverifikasi.");
            resp.put("otpVerified", true);
            if (snapToken != null && !snapToken.isBlank()) {
                resp.put("snapToken", snapToken);
                resp.put("paymentUrl", snapToken);
            }

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, Object> payload) {
        try {
            Object transaksiIdObj = payload.get("transaksiId");
            if (transaksiIdObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "transaksiId wajib diisi."));
            }

            Long transaksiId = Long.valueOf(transaksiIdObj.toString());
            Transaksi transaksi = transaksiRepository.findById(transaksiId)
                    .orElseThrow(() -> new Exception("Transaksi tidak ditemukan"));

            // Cek kepemilikan transaksi (cegah IDOR)
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            if (transaksi.getPelanggan() == null || !transaksi.getPelanggan().getEmail().equalsIgnoreCase(currentUserEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Akses ditolak: transaksi ini bukan milik Anda."));
            }

            // Tolak jika belum 60 detik dari otpLastSentAt
            if (transaksi.getOtpLastSentAt() != null && transaksi.getOtpLastSentAt().plusSeconds(60).isAfter(LocalDateTime.now())) {
                long secondsLeft = Duration.between(LocalDateTime.now(), transaksi.getOtpLastSentAt().plusSeconds(60)).getSeconds();
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Harap tunggu " + (secondsLeft > 0 ? secondsLeft : 60) + " detik sebelum meminta OTP baru.",
                        "retryAfter", (secondsLeft > 0 ? secondsLeft : 60)
                ));
            }

            // Buat OTP baru, reset otpAttempts, perpanjang otpExpiry (10 menit), kirim email
            String otpCode = String.format("%06d", RANDOM.nextInt(1_000_000));
            transaksi.setOtpHash(passwordEncoder.encode(otpCode));
            transaksi.setOtpAttempts(0);
            transaksi.setOtpVerified(false);
            transaksi.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
            transaksi.setOtpLastSentAt(LocalDateTime.now());
            transaksiRepository.save(transaksi);

            String movieTitle = "Tiket Bioskop";
            List<Tiket> tiketList = tiketRepository.findByTransaksiId(transaksi.getId());
            if (!tiketList.isEmpty() && tiketList.get(0).getJadwal() != null && tiketList.get(0).getJadwal().getFilm() != null) {
                movieTitle = tiketList.get(0).getJadwal().getFilm().getJudul();
            }

            mailService.sendTransactionOtpEmail(
                    transaksi.getPelanggan().getEmail(),
                    transaksi.getNomorPesanan(),
                    otpCode,
                    movieTitle,
                    transaksi.getTotalHarga()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Kode OTP baru telah berhasil dikirim ke email Anda.",
                    "success", true
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/snap-token")
    public ResponseEntity<?> getSnapToken(@RequestBody Map<String, Object> payload) {
        try {
            Object transaksiIdObj = payload.get("transaksiId");
            if (transaksiIdObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "transaksiId wajib diisi."));
            }

            Long transaksiId = Long.valueOf(transaksiIdObj.toString());
            Transaksi transaksi = transaksiRepository.findById(transaksiId)
                    .orElseThrow(() -> new Exception("Transaksi tidak ditemukan"));

            // Cek kepemilikan transaksi (cegah IDOR)
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            if (transaksi.getPelanggan() == null || !transaksi.getPelanggan().getEmail().equalsIgnoreCase(currentUserEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Akses ditolak: transaksi ini bukan milik Anda."));
            }

            // WAJIB: periksa otpVerified == true
            if (!transaksi.isOtpVerified()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Akses ditolak: OTP transaksi belum diverifikasi."));
            }

            String snapToken = paymentGateway.generatePaymentUrl(transaksi);
            return ResponseEntity.ok(Map.of(
                    "snapToken", snapToken,
                    "paymentUrl", snapToken
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/notification")
    public ResponseEntity<?> notification(@RequestBody Map<String, Object> payload) {
        try {
            String orderIdStr = payload.get("order_id") != null ? String.valueOf(payload.get("order_id")) : null;
            String statusCode = payload.get("status_code") != null ? String.valueOf(payload.get("status_code")) : null;
            String grossAmount = payload.get("gross_amount") != null ? String.valueOf(payload.get("gross_amount")) : null;
            String signatureKey = payload.get("signature_key") != null ? String.valueOf(payload.get("signature_key")) : null;
            String transactionStatus = payload.get("transaction_status") != null ? String.valueOf(payload.get("transaction_status")) : null;

            System.out.println("[MIDTRANS WEBHOOK] Notifikasi diterima untuk order_id: " + orderIdStr
                    + ", status_code: " + statusCode + ", transaction_status: " + transactionStatus
                    + ", gross_amount: " + grossAmount);

            if (orderIdStr == null || transactionStatus == null || !orderIdStr.matches("[0-9]{1,19}")
                    || statusCode == null || !statusCode.matches("[0-9]{1,4}")
                    || grossAmount == null || !grossAmount.matches("[0-9]+(?:\\.[0-9]{1,2})?")
                    || signatureKey == null || !signatureKey.matches("[0-9a-fA-F]{128}")) {
                System.err.println("[MIDTRANS WEBHOOK] Format payload tidak valid.");
                return ResponseEntity.badRequest().body("Invalid payload");
            }

            BioskopConfig config = configRepository.findById("SINGLETON").orElse(null);
            if (config == null || config.getPaymentServerKey() == null || config.getPaymentServerKey().isBlank()) {
                System.err.println("[MIDTRANS WEBHOOK] Payment Server Key belum dikonfigurasi.");
                return ResponseEntity.status(503).body("Payment webhook is not configured");
            }

            // 1. Verifikasi Signature SHA-512 bawaan Midtrans
            String rawString = orderIdStr + statusCode + grossAmount + config.getPaymentServerKey();
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] encodedHash = digest.digest(rawString.getBytes(StandardCharsets.UTF_8));
            StringBuilder calculatedSignatureBuilder = new StringBuilder(2 * encodedHash.length);
            for (byte hashByte : encodedHash) {
                String hexByte = Integer.toHexString(0xff & hashByte);
                if (hexByte.length() == 1) {
                    calculatedSignatureBuilder.append('0');
                }
                calculatedSignatureBuilder.append(hexByte);
            }
            String calculatedSignature = calculatedSignatureBuilder.toString();
            boolean signatureValid = MessageDigest.isEqual(calculatedSignature.getBytes(StandardCharsets.US_ASCII),
                    signatureKey.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));

            // 2. Layer Verifikasi Sekunder: Jika signature SHA-512 lokal tidak cocok
            // (misal karena Midtrans Sandbox menggunakan Signature Secret Key khusus),
            // lakukan verifikasi langsung ke REST API resmi Midtrans via HTTPS backend-to-backend.
            if (!signatureValid) {
                boolean verifiedViaMidtransApi = verifyWithMidtransApi(orderIdStr, grossAmount, config);
                if (!verifiedViaMidtransApi) {
                    System.err.println("[MIDTRANS WEBHOOK] Signature lokal tidak cocok dan verifikasi langsung Midtrans API gagal untuk order: " + orderIdStr);
                    return ResponseEntity.status(403).body("Invalid signature key");
                }
                System.out.println("[MIDTRANS WEBHOOK] Signature SHA-512 lokal berbeda, namun transaksi diverifikasi VALID oleh Midtrans GET Status API.");
            } else {
                System.out.println("[MIDTRANS WEBHOOK] Signature SHA-512 lokal terverifikasi valid.");
            }

            Long transaksiId = Long.parseLong(orderIdStr);
            Transaksi transaksi = transaksiRepository.findById(transaksiId)
                    .orElseThrow(() -> new Exception("Transaksi tidak ditemukan"));

            BigDecimal amountFromGateway = new BigDecimal(grossAmount);
            BigDecimal expectedAmount = BigDecimal.valueOf(transaksi.getTotalHarga());
            if (amountFromGateway.compareTo(expectedAmount) != 0) {
                System.err.println("[MIDTRANS WEBHOOK] Jumlah pembayaran tidak sesuai order.");
                return ResponseEntity.badRequest().body("Payment amount does not match order");
            }

            String fraudStatus = (String) payload.get("fraud_status");
            boolean settlementBerhasil = "settlement".equals(transactionStatus);
            boolean captureBerhasil = "capture".equals(transactionStatus) && "accept".equalsIgnoreCase(fraudStatus);
            if ((settlementBerhasil || captureBerhasil)
                    && transaksi.getStatus() == Transaksi.StatusTransaksi.PENDING
                    && transaksi.getRefundStatus() == Transaksi.RefundStatus.NONE) {
                transaksi.setStatus(Transaksi.StatusTransaksi.SUCCESS);
                transaksiRepository.save(transaksi);
                System.out.println("[MIDTRANS WEBHOOK] Status transaksi #" + transaksiId + " BERHASIL diubah menjadi SUCCESS!");
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Webhook error: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    private boolean verifyWithMidtransApi(String orderId, String expectedGrossAmount, BioskopConfig config) {
        try {
            String baseUrl = config.isProduction()
                    ? "https://api.midtrans.com/v2/"
                    : "https://api.sandbox.midtrans.com/v2/";
            String url = baseUrl + orderId + "/status";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Accept", "application/json");
            String authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString(
                    (config.getPaymentServerKey() + ":").getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", authHeader);

            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                String statusCode = String.valueOf(body.get("status_code"));
                String transactionStatus = String.valueOf(body.get("transaction_status"));
                String fraudStatus = String.valueOf(body.get("fraud_status"));
                String grossAmount = String.valueOf(body.get("gross_amount"));

                boolean status200 = "200".equals(statusCode);
                boolean settlement = "settlement".equals(transactionStatus);
                boolean capture = "capture".equals(transactionStatus) && "accept".equalsIgnoreCase(fraudStatus);

                if (status200 && (settlement || capture)) {
                    BigDecimal amountFromApi = new BigDecimal(grossAmount);
                    BigDecimal expectedAmount = new BigDecimal(expectedGrossAmount);
                    return amountFromApi.compareTo(expectedAmount) == 0;
                }
            }
        } catch (Exception e) {
            System.err.println("[MIDTRANS WEBHOOK] Gagal verifikasi langsung ke Midtrans API: " + e.getMessage());
        }
        return false;
    }
}
