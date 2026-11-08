/**
 * Tujuan program: Mengelola peninjauan, penolakan, persetujuan, dan penyelesaian refund.
 * Terakhir diubah: 2 Oktober 2026, 22:36 WIB.
 */
package com.cinemax.cinemax.admin.controller;

import com.cinemax.cinemax.domain.booking.Refund;
import com.cinemax.cinemax.domain.booking.RefundRepository;
import com.cinemax.cinemax.domain.booking.Transaksi;
import com.cinemax.cinemax.domain.booking.TransaksiRepository;
import com.cinemax.cinemax.domain.booking.Tiket;
import com.cinemax.cinemax.domain.booking.TiketRepository;
import com.cinemax.cinemax.domain.config.AuditLog;
import com.cinemax.cinemax.domain.config.AuditService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequestMapping("/admin")
@Transactional
public class AdminRefundController {

    private final RefundRepository refundRepository;

    private final TransaksiRepository transaksiRepository;

    private final TiketRepository tiketRepository;

    private final AuditService auditService;

    /** Membuat controller dengan dependency wajib melalui constructor injection. */
    public AdminRefundController(
            RefundRepository refundRepository,
            TransaksiRepository transaksiRepository,
            TiketRepository tiketRepository,
            AuditService auditService) {
        this.refundRepository = refundRepository;
        this.transaksiRepository = transaksiRepository;
        this.tiketRepository = tiketRepository;
        this.auditService = auditService;
    }

    @GetMapping("/refunds")
    public String viewRefunds(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Model model) {

        Specification<Refund> spec = (root, query, cb) -> cb.conjunction();

        if (status != null && !status.isEmpty() && !status.equals("Semua Status")) {
            try {
                Refund.StatusRefund statusEnum = Refund.StatusRefund.valueOf(status.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), statusEnum));
            } catch (Exception e) {}
        }

        if (search != null && !search.isEmpty()) {
            Specification<Refund> searchSpec = (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.join("transaksi").get("nomorPesanan")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.join("transaksi").join("pelanggan").get("namaLengkap")), "%" + search.toLowerCase() + "%")
            );
            spec = spec.and(searchSpec);
        }

        List<Refund> refunds = refundRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "waktuPengajuan"));

        model.addAttribute("refunds", refunds);
        model.addAttribute("activePage", "refund");
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentStatus", status);

        return "admin/manajemen_refund";
    }

    /**
     * Menerima refund dan melepaskan kursi tiket agar tersedia untuk pemesanan lain.
     * Pra-kondisi: pengajuan masih menunggu persetujuan.
     * Pasca-kondisi: status refund diterima dan seluruh tiket transaksi dilepas.
     */
    @PostMapping("/refund/approve/{id}")
    public String approveRefund(@PathVariable Long id) {
        Refund refund = refundRepository.findById(id).orElse(null);
        if (refund != null && refund.getStatus() == Refund.StatusRefund.PENDING) {
            String adminName = SecurityContextHolder.getContext().getAuthentication().getName();
            
            refund.setStatus(Refund.StatusRefund.APPROVED);
            refund.setDisetujuiOleh(adminName);
            refund.setWaktuPersetujuan(LocalDateTime.now());
            
            Transaksi transaksi = refund.getTransaksi();
            transaksi.setRefundStatus(Transaksi.RefundStatus.APPROVED_REFUND);
            // Kursi dilepas hanya setelah admin menerima pengajuan refund.
            for (Tiket tiket : tiketRepository.findByTransaksiId(transaksi.getId())) {
                tiket.setStatus(Tiket.StatusTiket.REFUNDED);
            }
            transaksiRepository.save(transaksi);
            
            refundRepository.save(refund);
            
            auditService.log(AuditLog.ActionType.UPDATE, "Refund: " + transaksi.getNomorPesanan(), "Menyetujui pengajuan refund");
        }
        return "redirect:/admin/refunds?approved=true";
    }

    @PostMapping("/refund/reject/{id}")
    public String rejectRefund(@PathVariable Long id, @RequestParam String alasanTolak) {
        Refund refund = refundRepository.findById(id).orElse(null);
        if (refund != null && refund.getStatus() == Refund.StatusRefund.PENDING
                && alasanTolak != null && !alasanTolak.isBlank() && alasanTolak.length() <= 2000) {
            String adminName = SecurityContextHolder.getContext().getAuthentication().getName();
            
            refund.setStatus(Refund.StatusRefund.REJECTED);
            refund.setDisetujuiOleh(adminName);
            refund.setWaktuPersetujuan(LocalDateTime.now());
            refund.setCatatanAdmin(alasanTolak);
            
            Transaksi transaksi = refund.getTransaksi();
            transaksi.setRefundStatus(Transaksi.RefundStatus.REJECTED);
            transaksi.setStatus(Transaksi.StatusTransaksi.SUCCESS);
            transaksiRepository.save(transaksi);
            
            refundRepository.save(refund);
            
            auditService.log(AuditLog.ActionType.UPDATE, "Refund: " + transaksi.getNomorPesanan(), "Menolak pengajuan refund: " + alasanTolak);
        }
        return "redirect:/admin/refunds?rejected=true";
    }

    /**
     * Menandai dana sudah dikembalikan setelah tahap persetujuan selesai.
     * Pra-kondisi: refund telah disetujui admin.
     * Pasca-kondisi: status refund dan transaksi tercatat selesai dikembalikan.
     */
    @PostMapping("/refund/process/{id}")
    public String processRefund(@PathVariable Long id) {
        Refund refund = refundRepository.findById(id).orElse(null);
        if (refund != null && refund.getStatus() == Refund.StatusRefund.APPROVED) {
            String adminName = SecurityContextHolder.getContext().getAuthentication().getName();
            refund.setStatus(Refund.StatusRefund.PROCESSED);
            refund.setDisetujuiOleh(adminName);
            refund.setWaktuPersetujuan(LocalDateTime.now());

            Transaksi transaksi = refund.getTransaksi();
            transaksi.setRefundStatus(Transaksi.RefundStatus.REFUNDED);
            transaksi.setStatus(Transaksi.StatusTransaksi.REFUND);
            transaksiRepository.save(transaksi);
            refundRepository.save(refund);
            auditService.log(AuditLog.ActionType.UPDATE, "Refund: " + transaksi.getNomorPesanan(),
                    "Pengembalian dana ditandai selesai");
        }
        return "redirect:/admin/refunds?processed=true";
    }
}
