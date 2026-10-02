/**
 * Tujuan program: Menyimpan alasan, status, dan tujuan rekening pengajuan refund.
 * Terakhir diubah: 2 Oktober 2026, 22:36 WIB.
 */
package com.cinemax.cinemax.domain.booking;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "transaksi_id", nullable = false)
    private Transaksi transaksi;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String alasan;

    @Column(length = 100)
    private String nomorRekening;

    @Column(length = 120)
    private String namaPemilikRekening;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusRefund status;

    private String diajukanOleh;
    private LocalDateTime waktuPengajuan;

    private String disetujuiOleh;
    private LocalDateTime waktuPersetujuan;

    @Column(columnDefinition = "TEXT")
    private String catatanAdmin;

    /** Tahapan persetujuan dan penyelesaian pengembalian dana. */
    public enum StatusRefund {
        PENDING,
        APPROVED,
        REJECTED,
        PROCESSED
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Transaksi getTransaksi() { return transaksi; }
    public void setTransaksi(Transaksi transaksi) { this.transaksi = transaksi; }
    public String getAlasan() { return alasan; }
    public void setAlasan(String alasan) { this.alasan = alasan; }
    public String getNomorRekening() { return nomorRekening; }
    public void setNomorRekening(String nomorRekening) { this.nomorRekening = nomorRekening; }
    public String getNamaPemilikRekening() { return namaPemilikRekening; }
    public void setNamaPemilikRekening(String namaPemilikRekening) { this.namaPemilikRekening = namaPemilikRekening; }
    public StatusRefund getStatus() { return status; }
    public void setStatus(StatusRefund status) { this.status = status; }
    public String getDiajukanOleh() { return diajukanOleh; }
    public void setDiajukanOleh(String diajukanOleh) { this.diajukanOleh = diajukanOleh; }
    public LocalDateTime getWaktuPengajuan() { return waktuPengajuan; }
    public void setWaktuPengajuan(LocalDateTime waktuPengajuan) { this.waktuPengajuan = waktuPengajuan; }
    public String getDisetujuiOleh() { return disetujuiOleh; }
    public void setDisetujuiOleh(String disetujuiOleh) { this.disetujuiOleh = disetujuiOleh; }
    public LocalDateTime getWaktuPersetujuan() { return waktuPersetujuan; }
    public void setWaktuPersetujuan(LocalDateTime waktuPersetujuan) { this.waktuPersetujuan = waktuPersetujuan; }
    public String getCatatanAdmin() { return catatanAdmin; }
    public void setCatatanAdmin(String catatanAdmin) { this.catatanAdmin = catatanAdmin; }
}
