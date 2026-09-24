package com.cinemax.cinemax.api.controller;

import com.cinemax.cinemax.api.dto.CheckoutRequestDTO;
import com.cinemax.cinemax.domain.booking.Tiket;
import com.cinemax.cinemax.domain.booking.TiketRepository;
import com.cinemax.cinemax.domain.booking.Transaksi;
import com.cinemax.cinemax.domain.booking.TransaksiRepository;
import com.cinemax.cinemax.domain.schedule.Jadwal;
import com.cinemax.cinemax.domain.schedule.JadwalRepository;
import com.cinemax.cinemax.domain.schedule.Kursi;
import com.cinemax.cinemax.domain.user.User;
import com.cinemax.cinemax.domain.user.UserRepository;
import com.cinemax.cinemax.infrastructure.payment.PaymentGateway;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
}
