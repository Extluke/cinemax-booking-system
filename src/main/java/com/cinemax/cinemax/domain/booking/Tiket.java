package com.cinemax.cinemax.domain.booking;

import com.cinemax.cinemax.domain.schedule.Jadwal;
import jakarta.persistence.*;

@Entity
@Table(name = "tikets")
public class Tiket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "transaksi_id", nullable = false)
    private Transaksi transaksi;

    @ManyToOne
    @JoinColumn(name = "jadwal_id", nullable = false)
    private Jadwal jadwal;

    private String nomorKursi;
    private Double harga;

    @Enumerated(EnumType.STRING)
    private StatusTiket status;

    public enum StatusTiket {
        VALID,
        REFUNDED
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Transaksi getTransaksi() { return transaksi; }
    public void setTransaksi(Transaksi transaksi) { this.transaksi = transaksi; }
    public Jadwal getJadwal() { return jadwal; }
    public void setJadwal(Jadwal jadwal) { this.jadwal = jadwal; }
    public String getNomorKursi() { return nomorKursi; }
    public void setNomorKursi(String nomorKursi) { this.nomorKursi = nomorKursi; }
    public Double getHarga() { return harga; }
    public void setHarga(Double harga) { this.harga = harga; }
    public StatusTiket getStatus() { return status; }
    public void setStatus(StatusTiket status) { this.status = status; }
}
