/**
 * Tujuan program: Menyediakan kueri aman untuk data banner promosi CMS.
 * Contributor: 'Aarif Rahmaan J. Faqiih
 * NIM: 103112430182
 * Role: User
 * Kelas: IF-12-07
 * Terakhir diubah: 27 September 2026, 00:00 WIB
 */
package com.cinemax.cinemax.domain.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PromosiRepository extends JpaRepository<Promosi, Long> {

    List<Promosi> findAllByOrderByTanggalMulaiDesc();

    List<Promosi> findByIsAktifOrderByTanggalMulaiDesc(boolean isAktif);

    /**
     * Mengambil banner yang aktif dan masih berada dalam masa promosi.
     * Parameter tanggal dipisahkan agar Spring Data membangun kueri
     * berparameter, bukan menyusun SQL dari masukan pengguna.
     *
     * @param tanggalMulaiBatas tanggal yang harus berada setelah awal promosi
     * @param tanggalAkhirBatas tanggal yang harus berada sebelum akhir promosi
     * @return banner CMS yang layak ditampilkan kepada pelanggan
     */
    List<Promosi> findByIsAktifTrueAndTanggalMulaiLessThanEqualAndTanggalAkhirGreaterThanEqualOrderByTanggalMulaiDesc(
            LocalDate tanggalMulaiBatas,
            LocalDate tanggalAkhirBatas);
}
