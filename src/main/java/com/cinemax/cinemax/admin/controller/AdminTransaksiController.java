package com.cinemax.cinemax.admin.controller;

import com.cinemax.cinemax.domain.booking.Transaksi;
import com.cinemax.cinemax.domain.booking.TransaksiRepository;
import com.cinemax.cinemax.domain.booking.Refund;
import com.cinemax.cinemax.domain.booking.RefundRepository;
import com.cinemax.cinemax.domain.booking.Tiket;
import com.cinemax.cinemax.domain.booking.TiketRepository;
import com.cinemax.cinemax.domain.config.AuditLog;
import com.cinemax.cinemax.domain.config.AuditService;
import com.cinemax.cinemax.domain.config.BioskopConfig;
import com.cinemax.cinemax.domain.config.BioskopConfigRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequestMapping("/admin")
@Transactional
public class AdminTransaksiController {

    private final TransaksiRepository transaksiRepository;
    private final RefundRepository refundRepository;
    private final TiketRepository tiketRepository;
    private final BioskopConfigRepository configRepository;
    private final AuditService auditService;

    public AdminTransaksiController(TransaksiRepository transaksiRepository, RefundRepository refundRepository,
            TiketRepository tiketRepository, BioskopConfigRepository configRepository, AuditService auditService) {
        this.transaksiRepository = transaksiRepository;
        this.refundRepository = refundRepository;
        this.tiketRepository = tiketRepository;
        this.configRepository = configRepository;
        this.auditService = auditService;
    }

    @GetMapping("/manajemen-transaksi")
    public String manajemenTransaksi(Model model) {
        List<Transaksi> transaksis = transaksiRepository.findAll(Sort.by(Sort.Direction.DESC, "tanggalTransaksi"));
        model.addAttribute("transaksis", transaksis);
        model.addAttribute("activePage", "transaksi");
        return "admin/manajemen_transaksi";
    }

    @PostMapping("/transaksi/ajukan-refund")
    public String ajukanRefund(@RequestParam Long transaksiId,
            @RequestParam(required = false) String alasan,
            @RequestParam(required = false) String namaPemilikRekening,
            @RequestParam(required = false) String nomorRekening) {
        Transaksi transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaksi tidak ditemukan."));
        if (transaksi.getStatus() != Transaksi.StatusTransaksi.SUCCESS
                || (transaksi.getRefundStatus() != Transaksi.RefundStatus.NONE
                        && transaksi.getRefundStatus() != Transaksi.RefundStatus.REJECTED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund hanya dapat diajukan untuk transaksi paid yang belum memiliki refund aktif.");
        }
        if (alasan == null || alasan.isBlank() || alasan.length() > 2000
                || namaPemilikRekening == null || namaPemilikRekening.isBlank() || namaPemilikRekening.length() > 120
                || nomorRekening == null || !nomorRekening.matches("[A-Za-z0-9 .-]{4,100}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alasan dan data rekening tidak valid.");
        }

        List<Tiket> tickets = tiketRepository.findByTransaksiId(transaksiId);
        if (tickets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaksi tidak memiliki tiket yang valid.");
        }
        BioskopConfig config = configRepository.findById("SINGLETON").orElseGet(BioskopConfig::new);
        LocalDateTime batasRefund = tickets.get(0).getJadwal().getWaktuMulai()
                .minusHours(Math.max(0, config.getRefundTimeLimitHours()));
        if (LocalDateTime.now().isAfter(batasRefund)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Refund hanya dapat diajukan sebelum batas H-" + config.getRefundTimeLimitHours() + " jam.");
        }

        boolean refundAktif = refundRepository.existsByTransaksiIdAndStatusIn(transaksiId,
                List.of(Refund.StatusRefund.PENDING, Refund.StatusRefund.APPROVED, Refund.StatusRefund.PROCESSED));
        if (refundAktif) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaksi sudah memiliki pengajuan refund aktif.");
        }

        String adminName = SecurityContextHolder.getContext().getAuthentication().getName();
        Refund refund = new Refund();
        refund.setTransaksi(transaksi);
        refund.setAlasan(alasan.trim());
        refund.setNamaPemilikRekening(namaPemilikRekening.trim());
        refund.setNomorRekening(nomorRekening.trim());
        refund.setStatus(Refund.StatusRefund.PENDING);
        refund.setDiajukanOleh(adminName);
        refund.setWaktuPengajuan(LocalDateTime.now());
        refundRepository.save(refund);
        transaksi.setRefundStatus(Transaksi.RefundStatus.PENDING_REFUND);
        transaksiRepository.save(transaksi);
        auditService.log(AuditLog.ActionType.CREATE, "Refund: " + transaksi.getNomorPesanan(),
                "Mengajukan pengembalian dana (Refund) atas nama pelanggan");
        return "redirect:/admin/manajemen-transaksi?refundRequested=true";
    }

}
