/**
 * Tujuan program: Menyediakan kueri tiket yang aman untuk transaksi dan laporan.
 * Contributor: 'Aarif Rahmaan J. Faqiih
 * NIM: 103112430182
 * Role: User
 * Kelas: IF-12-07
 * Terakhir diubah: 27 September 2026, 00:00 WIB
 */
package com.cinemax.cinemax.domain.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TiketRepository extends JpaRepository<Tiket, Long> {
    java.util.List<Tiket> findByJadwalIdAndStatus(Long jadwalId, Tiket.StatusTiket status);

    java.util.List<Tiket> findByTransaksiId(Long transaksiId);

    java.util.List<Tiket> findByTransaksiPelangganOrderByTransaksiTanggalTransaksiDesc(
            com.cinemax.cinemax.domain.user.User pelanggan);

    /**
     * Menghitung tiket sukses dalam satu rentang hari. Rentang setengah-terbuka
     * menghindari fungsi DATE database sehingga kueri tetap portabel dan indeks
     * kolom waktu masih dapat digunakan.
     */
    @Query("SELECT COUNT(t) FROM Tiket t WHERE t.transaksi.status = 'SUCCESS' " +
            "AND t.transaksi.tanggalTransaksi >= :waktuMulai " +
            "AND t.transaksi.tanggalTransaksi < :waktuSelesai")
    long countTicketsSoldInPeriod(
            @org.springframework.data.repository.query.Param("waktuMulai") java.time.LocalDateTime waktuMulai,
            @org.springframework.data.repository.query.Param("waktuSelesai") java.time.LocalDateTime waktuSelesai);

    // Menghitung total tiket berstatus VALID (Verifikasi)
    long countByStatus(Tiket.StatusTiket status);

    /**
     * Menghitung tiket sukses suatu studio dalam rentang hari yang sama dengan
     * metrik dashboard.
     */
    @Query("SELECT COUNT(t) FROM Tiket t WHERE t.jadwal.studio.id = :studioId " +
            "AND t.transaksi.status = 'SUCCESS' " +
            "AND t.transaksi.tanggalTransaksi >= :waktuMulai " +
            "AND t.transaksi.tanggalTransaksi < :waktuSelesai")
    long countTicketsSoldByStudioInPeriod(
            @org.springframework.data.repository.query.Param("studioId") Long studioId,
            @org.springframework.data.repository.query.Param("waktuMulai") java.time.LocalDateTime waktuMulai,
            @org.springframework.data.repository.query.Param("waktuSelesai") java.time.LocalDateTime waktuSelesai);

    // Rekap penjualan tiket 7 hari terakhir
    @Query(value = "SELECT DATE(tr.tanggal_transaksi), COUNT(t.id) " +
            "FROM tikets t JOIN transaksis tr ON t.transaksi_id = tr.id " +
            "WHERE tr.status = 'SUCCESS' AND tr.tanggal_transaksi >= :startDate " +
            "GROUP BY DATE(tr.tanggal_transaksi) " +
            "ORDER BY DATE(tr.tanggal_transaksi) ASC", nativeQuery = true)
    java.util.List<Object[]> findTicketSalesLast7Days(
            @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT COUNT(t) > 0 FROM Tiket t WHERE t.jadwal = :jadwal AND t.nomorKursi = :nomorKursi AND t.status NOT IN :statuses")
    boolean isSeatBooked(
            @org.springframework.data.repository.query.Param("jadwal") com.cinemax.cinemax.domain.schedule.Jadwal jadwal,
            @org.springframework.data.repository.query.Param("nomorKursi") String nomorKursi,
            @org.springframework.data.repository.query.Param("statuses") java.util.List<Tiket.StatusTiket> statuses);
}
