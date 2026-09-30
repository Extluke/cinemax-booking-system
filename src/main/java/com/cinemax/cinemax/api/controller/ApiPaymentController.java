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
import com.cinemax.cinemax.domain.config.BioskopConfig;
import com.cinemax.cinemax.domain.config.BioskopConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
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

            if (orderIdStr == null || transactionStatus == null) {
                return ResponseEntity.badRequest().body("Invalid payload");
            }

            BioskopConfig config = configRepository.findById("SINGLETON").orElse(null);
            if (config != null && config.getPaymentServerKey() != null && !config.getPaymentServerKey().isEmpty()) {
                // Validasi Signature Key jika Server Key tersedia (Opsional tapi disarankan)
                String serverKey = config.getPaymentServerKey();
                String rawString = orderIdStr + statusCode + grossAmount + serverKey;
                MessageDigest digest = MessageDigest.getInstance("SHA-512");
                byte[] encodedhash = digest.digest(rawString.getBytes(StandardCharsets.UTF_8));
                StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
                for (int i = 0; i < encodedhash.length; i++) {
                    String hex = Integer.toHexString(0xff & encodedhash[i]);
                    if(hex.length() == 1) {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }
                String calculatedSignature = hexString.toString();
                if (signatureKey != null && !calculatedSignature.equalsIgnoreCase(signatureKey)) {
                    // Invalid signature
                    return ResponseEntity.status(403).body("Invalid signature key");
                }
            }

            Long transaksiId = Long.parseLong(orderIdStr);
            Transaksi transaksi = transaksiRepository.findById(transaksiId)
                    .orElseThrow(() -> new Exception("Transaksi tidak ditemukan"));

            if (transactionStatus.equals("settlement") || transactionStatus.equals("capture")) {
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
