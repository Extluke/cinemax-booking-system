package com.cinemax.cinemax.domain.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long>, JpaSpecificationExecutor<Refund> {
    
    // Untuk mengecek apakah transaksi sudah memiliki refund yang sedang aktif
    boolean existsByTransaksiIdAndStatusIn(Long transaksiId, List<Refund.StatusRefund> statuses);
    
    // Opsional, kalau mau cari berdasarkan id transaksi
    Optional<Refund> findByTransaksiIdAndStatus(Long transaksiId, Refund.StatusRefund status);
    
    // Menghitung jumlah refund berdasarkan status
    long countByStatus(Refund.StatusRefund status);
}
