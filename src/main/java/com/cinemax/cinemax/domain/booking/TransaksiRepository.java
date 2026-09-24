package com.cinemax.cinemax.domain.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransaksiRepository extends JpaRepository<Transaksi, Long> {
    
    // Mengambil 5 transaksi terbaru
    List<Transaksi> findTop5ByOrderByTanggalTransaksiDesc();

    // Menghitung total pendapatan (hanya status SUCCESS)
    @Query("SELECT SUM(t.totalHarga) FROM Transaksi t WHERE t.status = 'SUCCESS'")
    Double calculateTotalRevenue();

    // Menghitung jumlah transaksi refund
    long countByStatus(Transaksi.StatusTransaksi status);

    // Revenue per film
    @Query("SELECT f.judul AS judul, SUM(t.harga) AS revenue " +
           "FROM Tiket t " +
           "JOIN t.jadwal j " +
           "JOIN j.film f " +
           "JOIN t.transaksi tr " +
           "WHERE tr.status = 'SUCCESS' " +
           "GROUP BY f.id, f.judul " +
           "ORDER BY SUM(t.harga) DESC")
    List<Object[]> calculateRevenueByFilm();

    // Revenue by Date
    @Query("SELECT DATE(tr.tanggalTransaksi) AS tanggal, SUM(tr.totalHarga) AS revenue " +
           "FROM Transaksi tr " +
           "WHERE tr.status = 'SUCCESS' " +
           "GROUP BY DATE(tr.tanggalTransaksi) " +
           "ORDER BY DATE(tr.tanggalTransaksi) ASC")
    List<Object[]> calculateRevenueByDate();
}
