package com.cinemax.cinemax.domain.config;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime waktuKejadian;

    @Column(nullable = false)
    private String adminPelaksana;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType aksi;

    @Column(nullable = false)
    private String objekSasaran;

    @Column(columnDefinition = "TEXT")
    private String keterangan;

    private String ipAddress;

    public enum ActionType {
        CREATE, UPDATE, DELETE, LOGIN
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getWaktuKejadian() { return waktuKejadian; }
    public void setWaktuKejadian(LocalDateTime waktuKejadian) { this.waktuKejadian = waktuKejadian; }

    public String getAdminPelaksana() { return adminPelaksana; }
    public void setAdminPelaksana(String adminPelaksana) { this.adminPelaksana = adminPelaksana; }

    public ActionType getAksi() { return aksi; }
    public void setAksi(ActionType aksi) { this.aksi = aksi; }

    public String getObjekSasaran() { return objekSasaran; }
    public void setObjekSasaran(String objekSasaran) { this.objekSasaran = objekSasaran; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
