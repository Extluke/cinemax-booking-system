package com.cinemax.cinemax.domain.schedule;

import com.cinemax.cinemax.domain.movie.Film;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "jadwals")
public class Jadwal {
    
    public enum StatusJadwal {
        AKTIF, DRAFT, SELESAI, DIBATALKAN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "film_id", nullable = false)
    private Film film;

    @ManyToOne
    @JoinColumn(name = "studio_id", nullable = false)
    private Studio studio;

    private LocalDateTime waktuMulai;
    private LocalDateTime waktuSelesai;
    private Double harga;
    
    @Enumerated(EnumType.STRING)
    private StatusJadwal status = StatusJadwal.DRAFT;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Film getFilm() { return film; }
    public void setFilm(Film film) { this.film = film; }
    public Studio getStudio() { return studio; }
    public void setStudio(Studio studio) { this.studio = studio; }
    public LocalDateTime getWaktuMulai() { return waktuMulai; }
    public void setWaktuMulai(LocalDateTime waktuMulai) { this.waktuMulai = waktuMulai; }
    public LocalDateTime getWaktuSelesai() { return waktuSelesai; }
    public void setWaktuSelesai(LocalDateTime waktuSelesai) { this.waktuSelesai = waktuSelesai; }
    public Double getHarga() { return harga; }
    public void setHarga(Double harga) { this.harga = harga; }
    public StatusJadwal getStatus() { return status; }
    public void setStatus(StatusJadwal status) { this.status = status; }
}
