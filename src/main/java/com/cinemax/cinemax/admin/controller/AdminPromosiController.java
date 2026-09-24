package com.cinemax.cinemax.admin.controller;

import com.cinemax.cinemax.domain.booking.Promosi;
import com.cinemax.cinemax.domain.booking.PromosiRepository;
import com.cinemax.cinemax.domain.config.AuditLog;
import com.cinemax.cinemax.domain.config.AuditService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;

@Controller
@RequestMapping("/admin")
@Transactional
public class AdminPromosiController {

    @Autowired
    private PromosiRepository promosiRepository;

    @Autowired
    private AuditService auditService;

    @GetMapping("/manajemen-promosi")
    public String manajemenPromosi(Model model, 
                                   @RequestParam(required = false) String search,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggalMulai,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggalAkhir) {
        
        List<Promosi> listPromosi = promosiRepository.findAllByOrderByTanggalMulaiDesc();
        
        // Filtering
        if (search != null && !search.isEmpty()) {
            listPromosi = listPromosi.stream()
                .filter(p -> p.getJudul().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (status != null && !status.isEmpty() && !status.equals("SEMUA")) {
            boolean isActiveFilter = status.equals("AKTIF");
            listPromosi = listPromosi.stream()
                .filter(p -> p.getIsAktif() == isActiveFilter)
                .collect(Collectors.toList());
        }
        
        if (tanggalMulai != null) {
            listPromosi = listPromosi.stream()
                .filter(p -> !p.getTanggalMulai().isBefore(tanggalMulai))
                .collect(Collectors.toList());
        }
        
        if (tanggalAkhir != null) {
            listPromosi = listPromosi.stream()
                .filter(p -> !p.getTanggalAkhir().isAfter(tanggalAkhir))
                .collect(Collectors.toList());
        }

        // Active promos for preview carousel
        List<Promosi> activePromos = listPromosi.stream().filter(Promosi::getIsAktif).collect(Collectors.toList());
        if (activePromos.isEmpty()) {
            activePromos = promosiRepository.findByIsAktifOrderByTanggalMulaiDesc(true);
        }
        
        model.addAttribute("listPromosi", listPromosi);
        model.addAttribute("activePromos", activePromos);
        model.addAttribute("activePage", "promosi");
        
        return "admin/manajemen_promosi";
    }

    @GetMapping("/tambah-promosi")
    public String tambahPromosiForm(Model model) {
        model.addAttribute("activePage", "promosi");
        return "admin/tambah_promosi";
    }

    @PostMapping("/tambah-promosi")
    public String tambahPromosiSubmit(@RequestParam String judul,
                                      @RequestParam(required = false) String targetUrl,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggalMulai,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggalAkhir,
                                      @RequestParam(required = false) String isAktif,
                                      @RequestParam(value = "posterFile", required = false) MultipartFile posterFile) {
        
        Promosi promosi = new Promosi();
        promosi.setJudul(judul);
        promosi.setTargetUrl(targetUrl != null && !targetUrl.isEmpty() ? targetUrl : null);
        promosi.setTanggalMulai(tanggalMulai);
        promosi.setTanggalAkhir(tanggalAkhir);
        promosi.setIsAktif(isAktif != null && isAktif.equals("on"));
        
        if (posterFile != null && !posterFile.isEmpty()) {
            try {
                String uploadDir = "uploads/promosi/";
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                String fileName = UUID.randomUUID().toString() + "_" + 
                        posterFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(posterFile.getInputStream(), filePath);
                promosi.setPosterUrl("/uploads/promosi/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
                promosi.setPosterUrl("/img/poster_placeholder.jpg"); // Fallback
            }
        } else {
            promosi.setPosterUrl("/img/poster_placeholder.jpg");
        }
        
        promosiRepository.save(promosi);
        auditService.log(AuditLog.ActionType.CREATE, "Promosi: " + promosi.getJudul(), "Menambahkan banner promosi baru");
        
        return "redirect:/admin/manajemen-promosi?success=true";
    }
    
    @GetMapping("/edit-promosi")
    public String editPromosiForm(@RequestParam Long id, Model model) {
        Promosi promosi = promosiRepository.findById(id).orElse(null);
        if (promosi == null) return "redirect:/admin/manajemen-promosi?error=notfound";
        
        model.addAttribute("promosi", promosi);
        model.addAttribute("activePage", "promosi");
        return "admin/tambah_promosi"; // Re-use the form for edit
    }
    
    @PostMapping("/edit-promosi")
    public String editPromosiSubmit(@RequestParam Long id,
                                    @RequestParam String judul,
                                    @RequestParam(required = false) String targetUrl,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggalMulai,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggalAkhir,
                                    @RequestParam(required = false) String isAktif,
                                    @RequestParam(value = "posterFile", required = false) MultipartFile posterFile) {
        
        Promosi promosi = promosiRepository.findById(id).orElse(null);
        if (promosi == null) return "redirect:/admin/manajemen-promosi?error=notfound";
        
        promosi.setJudul(judul);
        promosi.setTargetUrl(targetUrl != null && !targetUrl.isEmpty() ? targetUrl : null);
        promosi.setTanggalMulai(tanggalMulai);
        promosi.setTanggalAkhir(tanggalAkhir);
        promosi.setIsAktif(isAktif != null && isAktif.equals("on"));
        
        if (posterFile != null && !posterFile.isEmpty()) {
            try {
                // Hapus poster lama
                if (promosi.getPosterUrl() != null && promosi.getPosterUrl().startsWith("/uploads/promosi/")) {
                    String oldFileName = promosi.getPosterUrl().substring("/uploads/promosi/".length());
                    Path oldFilePath = Paths.get("uploads/promosi/").resolve(oldFileName);
                    Files.deleteIfExists(oldFilePath);
                }
                
                String uploadDir = "uploads/promosi/";
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
                
                String fileName = UUID.randomUUID().toString() + "_" + 
                        posterFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(posterFile.getInputStream(), filePath);
                promosi.setPosterUrl("/uploads/promosi/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        promosiRepository.save(promosi);
        auditService.log(AuditLog.ActionType.UPDATE, "Promosi: " + promosi.getJudul(), "Mengubah banner promosi ID " + promosi.getId());
        return "redirect:/admin/manajemen-promosi?updated=true";
    }

    @PostMapping("/hapus-promosi/{id}")
    public String hapusPromosi(@PathVariable Long id) {
        Promosi promosi = promosiRepository.findById(id).orElse(null);
        if (promosi != null) {
            if (promosi.getPosterUrl() != null && promosi.getPosterUrl().startsWith("/uploads/promosi/")) {
                String fileName = promosi.getPosterUrl().substring("/uploads/promosi/".length());
                Path filePath = Paths.get("uploads/promosi/").resolve(fileName);
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            String namaPromosi = promosi.getJudul();
            promosiRepository.delete(promosi);
            auditService.log(AuditLog.ActionType.DELETE, "Promosi: " + namaPromosi, "Menghapus banner promosi");
        }
        return "redirect:/admin/manajemen-promosi?deleted=true";
    }
    
    @PostMapping("/toggle-promosi/{id}")
    public String togglePromosi(@PathVariable Long id) {
        Promosi promosi = promosiRepository.findById(id).orElse(null);
        if (promosi != null) {
            String namaPromosi = promosi.getJudul();
            promosi.setIsAktif(!promosi.getIsAktif());
            promosiRepository.save(promosi);
            auditService.log(AuditLog.ActionType.UPDATE, "Promosi: " + namaPromosi, "Mengubah status aktif menjadi " + promosi.getIsAktif());
        }
        return "redirect:/admin/manajemen-promosi?updated=true";
    }
}
