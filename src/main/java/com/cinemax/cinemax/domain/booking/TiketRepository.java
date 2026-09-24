package com.cinemax.cinemax.domain.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TiketRepository extends JpaRepository<Tiket, Long> {
    
    // Menghitung tiket terjual hari ini
    @Query("SELECT COUNT(t) FROM Tiket t WHERE t.transaksi.status = 'SUCCESS' AND DATE(t.transaksi.tanggalTransaksi) = CURRENT_DATE")
    long countTicketsSoldToday();

    // Menghitung total tiket berstatus VALID (Verifikasi)
    long countByStatus(Tiket.StatusTiket status);

    // Menghitung tiket terjual hari ini per studio
    @Query("SELECT COUNT(t) FROM Tiket t WHERE t.jadwal.studio.id = :studioId AND t.transaksi.status = 'SUCCESS' AND DATE(t.transaksi.tanggalTransaksi) = CURRENT_DATE")
    long countTicketsSoldTodayByStudio(@org.springframework.data.repository.query.Param("studioId") Long studioId);

    // Rekap penjualan tiket 7 hari terakhir
    @Query(value = "SELECT DATE(tr.tanggal_transaksi), COUNT(t.id) " +
                   "FROM tikets t JOIN transaksis tr ON t.transaksi_id = tr.id " +
                   "WHERE tr.status = 'SUCCESS' AND tr.tanggal_transaksi >= :startDate " +
                   "GROUP BY DATE(tr.tanggal_transaksi) " +
                   "ORDER BY DATE(tr.tanggal_transaksi) ASC", nativeQuery = true)
    java.util.List<Object[]> findTicketSalesLast7Days(@org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT COUNT(t) > 0 FROM Tiket t WHERE t.jadwal = :jadwal AND t.nomorKursi = :nomorKursi AND t.status NOT IN :statuses")
    boolean isSeatBooked(@org.springframework.data.repository.query.Param("jadwal") com.cinemax.cinemax.domain.schedule.Jadwal jadwal, @org.springframework.data.repository.query.Param("nomorKursi") String nomorKursi, @org.springframework.data.repository.query.Param("statuses") java.util.List<Tiket.StatusTiket> statuses);
}
