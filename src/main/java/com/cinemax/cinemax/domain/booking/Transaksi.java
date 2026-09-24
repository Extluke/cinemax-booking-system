package com.cinemax.cinemax.domain.booking;

import com.cinemax.cinemax.domain.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaksis")
public class Transaksi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nomorPesanan;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User pelanggan;

    private LocalDateTime tanggalTransaksi;
    private Double totalHarga;

    @Enumerated(EnumType.STRING)
    private StatusTransaksi status;

    public enum StatusTransaksi {
        PENDING,
        SUCCESS,
        REFUND
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'NONE'")
    private RefundStatus refundStatus = RefundStatus.NONE;

    public enum RefundStatus {
        NONE,
        PENDING_REFUND,
        REFUNDED,
        REJECTED
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNomorPesanan() { return nomorPesanan; }
    public void setNomorPesanan(String nomorPesanan) { this.nomorPesanan = nomorPesanan; }
    public User getPelanggan() { return pelanggan; }
    public void setPelanggan(User pelanggan) { this.pelanggan = pelanggan; }
    public LocalDateTime getTanggalTransaksi() { return tanggalTransaksi; }
    public void setTanggalTransaksi(LocalDateTime tanggalTransaksi) { this.tanggalTransaksi = tanggalTransaksi; }
    public Double getTotalHarga() { return totalHarga; }
    public void setTotalHarga(Double totalHarga) { this.totalHarga = totalHarga; }
    public StatusTransaksi getStatus() { return status; }
    public void setStatus(StatusTransaksi status) { this.status = status; }
    public RefundStatus getRefundStatus() { return refundStatus; }
    public void setRefundStatus(RefundStatus refundStatus) { this.refundStatus = refundStatus; }
}
