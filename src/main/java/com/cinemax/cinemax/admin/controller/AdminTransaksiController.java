package com.cinemax.cinemax.admin.controller;

import com.cinemax.cinemax.domain.booking.Refund;
import com.cinemax.cinemax.domain.booking.RefundRepository;
import com.cinemax.cinemax.domain.booking.Transaksi;
import com.cinemax.cinemax.domain.booking.TransaksiRepository;
import com.cinemax.cinemax.domain.config.AuditLog;
import com.cinemax.cinemax.domain.config.AuditService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequestMapping("/admin")
@Transactional
public class AdminTransaksiController {

    @Autowired
    private TransaksiRepository transaksiRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private AuditService auditService;

    @GetMapping("/manajemen-transaksi")
    public String manajemenTransaksi(Model model) {
        List<Transaksi> transaksis = transaksiRepository.findAll(Sort.by(Sort.Direction.DESC, "tanggalTransaksi"));
        model.addAttribute("transaksis", transaksis);
        model.addAttribute("activePage", "transaksi");
        return "admin/manajemen_transaksi";
    }

    @PostMapping("/transaksi/ajukan-refund")
    public String ajukanRefund(@RequestParam Long transaksiId, @RequestParam String alasan) {
        Transaksi transaksi = transaksiRepository.findById(transaksiId).orElse(null);
        if (transaksi != null && transaksi.getStatus() == Transaksi.StatusTransaksi.SUCCESS) {
            
            // Cek apakah sudah ada refund pending untuk transaksi ini
            boolean hasPendingRefund = refundRepository.existsByTransaksiIdAndStatusIn(transaksiId, List.of(Refund.StatusRefund.PENDING, Refund.StatusRefund.APPROVED, Refund.StatusRefund.PROCESSED));
            
            if (!hasPendingRefund) {
                String adminName = SecurityContextHolder.getContext().getAuthentication().getName();
                
                Refund refund = new Refund();
                refund.setTransaksi(transaksi);
                refund.setAlasan(alasan);
                refund.setStatus(Refund.StatusRefund.PENDING);
                refund.setDiajukanOleh(adminName);
                refund.setWaktuPengajuan(LocalDateTime.now());
                refundRepository.save(refund);
                
                transaksi.setRefundStatus(Transaksi.RefundStatus.PENDING_REFUND);
                transaksiRepository.save(transaksi);
                
                auditService.log(AuditLog.ActionType.CREATE, "Refund: " + transaksi.getNomorPesanan(), "Mengajukan pengembalian dana (Refund)");
                return "redirect:/admin/manajemen-transaksi?refundRequested=true";
            } else {
                return "redirect:/admin/manajemen-transaksi?error=duplicateRefund";
            }
        }
        return "redirect:/admin/manajemen-transaksi?error=invalidTransaction";
    }
}
