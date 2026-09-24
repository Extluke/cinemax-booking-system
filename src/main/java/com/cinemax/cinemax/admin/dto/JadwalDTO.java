package com.cinemax.cinemax.admin.dto;

import com.cinemax.cinemax.domain.schedule.Jadwal;
import java.time.format.DateTimeFormatter;

public class JadwalDTO {
    private Long id;
    private String filmJudul;
    private String waktuMulaiStr;
    private String waktuSelesaiStr;
    private double leftPercentage;
    private double widthPercentage;
    private boolean isConflict;
    private String conflictReason;
    
    private Long filmId;
    private Long studioId;
    private Double harga;
    private String status;

    public JadwalDTO(Jadwal jadwal, double leftPercentage, double widthPercentage, boolean isConflict, String conflictReason) {
        this.id = jadwal.getId();
        this.filmJudul = jadwal.getFilm().getJudul();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        this.waktuMulaiStr = jadwal.getWaktuMulai() != null ? jadwal.getWaktuMulai().format(timeFormatter) : "";
        this.waktuSelesaiStr = jadwal.getWaktuSelesai() != null ? jadwal.getWaktuSelesai().format(timeFormatter) : "";
        this.leftPercentage = leftPercentage;
        this.widthPercentage = widthPercentage;
        this.isConflict = isConflict;
        this.conflictReason = conflictReason;
        
        this.filmId = jadwal.getFilm() != null ? jadwal.getFilm().getId() : null;
        this.studioId = jadwal.getStudio() != null ? jadwal.getStudio().getId() : null;
        this.harga = jadwal.getHarga();
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (jadwal.getStatus() != null && jadwal.getStatus().name().equals("AKTIF")) {
            if (jadwal.getWaktuSelesai() != null && now.isAfter(jadwal.getWaktuSelesai())) {
                this.status = "SELESAI";
            } else if (jadwal.getWaktuMulai() != null && jadwal.getWaktuSelesai() != null && 
                       !now.isBefore(jadwal.getWaktuMulai()) && !now.isAfter(jadwal.getWaktuSelesai())) {
                this.status = "SEDANG_TAYANG";
            } else {
                this.status = "AKTIF";
            }
        } else {
            this.status = jadwal.getStatus() != null ? jadwal.getStatus().name() : "AKTIF";
        }
    }

    public Long getId() { return id; }
    public String getFilmJudul() { return filmJudul; }
    public String getWaktuMulaiStr() { return waktuMulaiStr; }
    public String getWaktuSelesaiStr() { return waktuSelesaiStr; }
    public double getLeftPercentage() { return leftPercentage; }
    public double getWidthPercentage() { return widthPercentage; }
    public boolean getIsConflict() { return isConflict; }
    public String getConflictReason() { return conflictReason; }
    public Long getFilmId() { return filmId; }
    public Long getStudioId() { return studioId; }
    public Double getHarga() { return harga; }
    public String getStatus() { return status; }
}
