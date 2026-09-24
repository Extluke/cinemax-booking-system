package com.cinemax.cinemax.admin.controller;

import com.cinemax.cinemax.domain.config.AuditLog;
import com.cinemax.cinemax.domain.config.AuditLogRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminAuditController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping("/audit-log")
    public String viewAuditLog(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String aksi,
            @RequestParam(required = false) String tanggal,
            Model model) {

        Specification<AuditLog> spec = (root, query, cb) -> cb.conjunction();

        // Filter: Teks (Nama Admin / Keterangan)
        if (search != null && !search.isEmpty()) {
            Specification<AuditLog> searchSpec = (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("adminPelaksana")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("keterangan")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("objekSasaran")), "%" + search.toLowerCase() + "%")
            );
            spec = spec.and(searchSpec);
        }

        // Filter: Aksi
        if (aksi != null && !aksi.isEmpty() && !aksi.equals("Semua Aksi")) {
            // Mapping dari UI ke Enum
            String enumValue = aksi;
            if (aksi.contains("CREATE")) enumValue = "CREATE";
            else if (aksi.contains("UPDATE")) enumValue = "UPDATE";
            else if (aksi.contains("DELETE")) enumValue = "DELETE";
            else if (aksi.contains("LOGIN")) enumValue = "LOGIN";
            
            try {
                AuditLog.ActionType actionType = AuditLog.ActionType.valueOf(enumValue);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("aksi"), actionType));
            } catch (Exception e) {}
        }

        // Filter: Tanggal
        if (tanggal != null && !tanggal.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(tanggal);
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
                spec = spec.and((root, query, cb) -> cb.between(root.get("waktuKejadian"), startOfDay, endOfDay));
            } catch (Exception e) {}
        }

        // Apply Order By WaktuKejadian Descending manually since Specification clears default ordering if not handled
        // Spring Data JPA requires Sort parameter for Specifications to be ordered
        List<AuditLog> logs = auditLogRepository.findAll(spec, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "waktuKejadian"));

        model.addAttribute("logs", logs);
        model.addAttribute("activePage", "audit");
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentAksi", aksi);
        model.addAttribute("currentTanggal", tanggal);

        return "admin/audit_log";
    }

    @GetMapping("/audit-log/export")
    public void exportAuditLog(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"audit_log.csv\"");

        List<AuditLog> logs = auditLogRepository.findAllByOrderByWaktuKejadianDesc();

        PrintWriter writer = response.getWriter();
        writer.println("Waktu Kejadian,Admin Pelaksana,Aksi,Objek Sasaran,IP Address,Keterangan");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

        for (AuditLog log : logs) {
            writer.printf("%s,%s,%s,%s,%s,\"%s\"\n",
                    log.getWaktuKejadian().format(formatter),
                    escapeCsv(log.getAdminPelaksana()),
                    log.getAksi().name(),
                    escapeCsv(log.getObjekSasaran()),
                    escapeCsv(log.getIpAddress()),
                    escapeCsv(log.getKeterangan())
            );
        }
        writer.flush();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",")) {
            return "\"" + value + "\"";
        }
        return value;
    }
}
