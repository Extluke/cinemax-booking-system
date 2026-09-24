package com.cinemax.cinemax.domain.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(AuditLog.ActionType aksi, String objekSasaran, String keterangan) {
        String adminName = "Sistem / Unknown";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            adminName = auth.getName();
        }

        String ip = "Unknown";
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        saveLogAsync(aksi, objekSasaran, keterangan, adminName, ip);
    }

    @org.springframework.scheduling.annotation.Async
    public void saveLogAsync(AuditLog.ActionType aksi, String objekSasaran, String keterangan, String adminPelaksana, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setWaktuKejadian(LocalDateTime.now());
        auditLog.setAksi(aksi);
        auditLog.setObjekSasaran(objekSasaran);
        auditLog.setKeterangan(keterangan);
        auditLog.setAdminPelaksana(adminPelaksana);
        auditLog.setIpAddress(ipAddress);
        auditLog.setWaktuKejadian(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }
}
