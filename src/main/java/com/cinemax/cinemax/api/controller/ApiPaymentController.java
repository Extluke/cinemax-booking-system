/**
 * Tujuan program: Menangani checkout dan notifikasi pembayaran dari gateway.
 * Terakhir diubah: 2 Oktober 2026, 22:36 WIB.
 */
package com.cinemax.cinemax.api.controller;

import com.cinemax.cinemax.api.dto.CheckoutRequestDTO;
import com.cinemax.cinemax.domain.booking.Tiket;
import com.cinemax.cinemax.domain.booking.TiketRepository;
import com.cinemax.cinemax.domain.booking.Transaksi;
import com.cinemax.cinemax.domain.booking.TransaksiRepository;
import com.cinemax.cinemax.domain.schedule.Jadwal;
import com.cinemax.cinemax.domain.schedule.JadwalRepository;

import com.cinemax.cinemax.domain.user.User;
import com.cinemax.cinemax.domain.user.UserRepository;
import com.cinemax.cinemax.infrastructure.payment.PaymentGateway;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import com.cinemax.cinemax.domain.config.BioskopConfig;
import com.cinemax.cinemax.domain.config.BioskopConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class ApiPaymentController {

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

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequestDTO request) {
        try {
            // Validasi Jadwal
            Jadwal jadwal = jadwalRepository.findById(request.getJadwalId())
                    .orElseThrow(() -> new Exception("Jadwal tidak ditemukan"));

            // Mencegah IDOR: Ambil email user yang sedang login dari token/session
            String currentUserEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
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
            transaksiRepository.save(transaksi);

            // Buat Tiket untuk setiap kursi
            for (String kodeKursi : request.getKursiKode()) {
                Tiket tiket = new Tiket();
                tiket.setTransaksi(transaksi);
                tiket.setJadwal(jadwal);
                tiket.setNomorKursi(kodeKursi);
                tiket.setHarga(jadwal.getHarga());
                tiket.setStatus(Tiket.StatusTiket.VALID); // Atau PENDING
                tiketRepository.save(tiket);
            }

            // Request Payment URL dari Gateway
            String paymentUrl = paymentGateway.generatePaymentUrl(transaksi);

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("transaksiId", transaksi.getId());
            response.put("nomorPesanan", transaksi.getNomorPesanan());
            response.put("totalHarga", transaksi.getTotalHarga());
            response.put("paymentUrl", paymentUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/notification")
    public ResponseEntity<?> notification(@RequestBody Map<String, Object> payload) {
        try {
            String orderIdStr = (String) payload.get("order_id");
            String statusCode = (String) payload.get("status_code");
            String grossAmount = (String) payload.get("gross_amount");
            String signatureKey = (String) payload.get("signature_key");
            String transactionStatus = (String) payload.get("transaction_status");

            if (orderIdStr == null || transactionStatus == null || !orderIdStr.matches("[0-9]{1,19}")
                    || statusCode == null || !statusCode.matches("[0-9]{1,4}")
                    || grossAmount == null || !grossAmount.matches("[0-9]+(?:\\.[0-9]{1,2})?")
                    || signatureKey == null || !signatureKey.matches("[0-9a-fA-F]{128}")) {
                return ResponseEntity.badRequest().body("Invalid payload");
            }

            BioskopConfig config = configRepository.findById("SINGLETON").orElse(null);
            if (config == null || config.getPaymentServerKey() == null || config.getPaymentServerKey().isBlank()) {
                return ResponseEntity.status(503).body("Payment webhook is not configured");
            }

            // Hanya webhook bertanda tangan valid boleh mengubah transaksi menjadi lunas.
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
            if (!signatureValid) {
                return ResponseEntity.status(403).body("Invalid signature key");
            }

            Long transaksiId = Long.parseLong(orderIdStr);
            Transaksi transaksi = transaksiRepository.findById(transaksiId)
                    .orElseThrow(() -> new Exception("Transaksi tidak ditemukan"));

            BigDecimal amountFromGateway = new BigDecimal(grossAmount);
            BigDecimal expectedAmount = BigDecimal.valueOf(transaksi.getTotalHarga());
            if (amountFromGateway.compareTo(expectedAmount) != 0) {
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
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Webhook error: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}
