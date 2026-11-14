/**
 * Tujuan program: Mewakili transaksi pembayaran dan status refund pelanggan.
 * Terakhir diubah: 2 Oktober 2026, 22:36 WIB.
 */
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

    @Column(nullable = true)
    private String otpHash;

    @Column(nullable = true)
    private LocalDateTime otpExpiry;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int otpAttempts = 0;

    @Column(nullable = true)
    private LocalDateTime otpLastSentAt;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean otpVerified = false;

    public enum RefundStatus {
        NONE,
        PENDING_REFUND,
        APPROVED_REFUND,
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

    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }

    public LocalDateTime getOtpExpiry() { return otpExpiry; }
    public void setOtpExpiry(LocalDateTime otpExpiry) { this.otpExpiry = otpExpiry; }

    public int getOtpAttempts() { return otpAttempts; }
    public void setOtpAttempts(int otpAttempts) { this.otpAttempts = otpAttempts; }

    public LocalDateTime getOtpLastSentAt() { return otpLastSentAt; }
    public void setOtpLastSentAt(LocalDateTime otpLastSentAt) { this.otpLastSentAt = otpLastSentAt; }

    public boolean isOtpVerified() { return otpVerified; }
    public boolean getOtpVerified() { return otpVerified; }
    public void setOtpVerified(boolean otpVerified) { this.otpVerified = otpVerified; }
}
