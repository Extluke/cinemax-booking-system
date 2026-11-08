package com.cinemax.cinemax;

import com.cinemax.cinemax.domain.booking.Refund;
import com.cinemax.cinemax.domain.booking.RefundRepository;
import com.cinemax.cinemax.domain.booking.Tiket;
import com.cinemax.cinemax.domain.booking.TiketRepository;
import com.cinemax.cinemax.domain.booking.Transaksi;
import com.cinemax.cinemax.domain.booking.TransaksiRepository;
import com.cinemax.cinemax.domain.config.BioskopConfig;
import com.cinemax.cinemax.domain.config.BioskopConfigRepository;
import com.cinemax.cinemax.domain.movie.Film;
import com.cinemax.cinemax.domain.movie.FilmRepository;
import com.cinemax.cinemax.domain.schedule.Jadwal;
import com.cinemax.cinemax.domain.schedule.JadwalRepository;
import com.cinemax.cinemax.domain.schedule.Studio;
import com.cinemax.cinemax.domain.schedule.StudioRepository;
import com.cinemax.cinemax.domain.user.User;
import com.cinemax.cinemax.domain.user.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RefundWorkflowIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired FilmRepository filmRepository;
    @Autowired StudioRepository studioRepository;
    @Autowired JadwalRepository jadwalRepository;
    @Autowired TransaksiRepository transaksiRepository;
    @Autowired TiketRepository tiketRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired BioskopConfigRepository configRepository;

    private Transaksi transaksi;
    private Tiket tiket;

    @BeforeEach
    void setUp() {
        tiketRepository.deleteAll();
        refundRepository.deleteAll();
        transaksiRepository.deleteAll();
        jadwalRepository.deleteAll();
        filmRepository.deleteAll();
        studioRepository.deleteAll();
        userRepository.deleteAll();
        configRepository.deleteAll();

        BioskopConfig config = new BioskopConfig();
        config.setRefundTimeLimitHours(2);
        configRepository.save(config);

        User user = new User();
        user.setNamaLengkap("Refund Customer");
        user.setEmail("refund-customer@test.local");
        user.setPassword("unused");
        user.setRole("ROLE_USER");
        userRepository.save(user);

        Film film = new Film();
        film.setJudul("Refund Test Movie");
        film.setSinopsis("Test");
        film.setDurasi(90);
        film.setBatasUsia("SU");
        film.setStatus(Film.StatusFilm.SEDANG_TAYANG);
        filmRepository.save(film);

        Studio studio = new Studio();
        studio.setNama("Refund Test Studio");
        studio.setKapasitas(20);
        studio.setStatus(Studio.StatusStudio.AKTIF);
        studioRepository.save(studio);

        Jadwal jadwal = new Jadwal();
        jadwal.setFilm(film);
        jadwal.setStudio(studio);
        jadwal.setHarga(50000.0);
        jadwal.setStatus(Jadwal.StatusJadwal.AKTIF);
        jadwal.setWaktuMulai(LocalDateTime.now().plusDays(1));
        jadwal.setWaktuSelesai(LocalDateTime.now().plusDays(1).plusHours(2));
        jadwalRepository.save(jadwal);

        transaksi = new Transaksi();
        transaksi.setNomorPesanan("900001");
        transaksi.setPelanggan(user);
        transaksi.setTanggalTransaksi(LocalDateTime.now());
        transaksi.setTotalHarga(50000.0);
        transaksi.setStatus(Transaksi.StatusTransaksi.SUCCESS);
        transaksi.setRefundStatus(Transaksi.RefundStatus.NONE);
        transaksiRepository.save(transaksi);

        tiket = new Tiket();
        tiket.setTransaksi(transaksi);
        tiket.setJadwal(jadwal);
        tiket.setNomorKursi("A1");
        tiket.setHarga(50000.0);
        tiket.setStatus(Tiket.StatusTiket.VALID);
        tiketRepository.save(tiket);
    }

    @Test
    void paidRefundCanBeRequestedApprovedAndCompletedAndSeatReleased() throws Exception {
        mockMvc.perform(post("/ajukan-refund").with(user("refund-customer@test.local").roles("USER")).with(csrf())
                        .param("transaksiId", transaksi.getId().toString())
                        .param("alasan", "Jadwal berubah")
                        .param("namaPemilikRekening", "Ayu")
                        .param("nomorRekening", "12345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/detail-tiket?transaksiId=" + transaksi.getId()));

        Refund refund = refundRepository.findFirstByTransaksiIdOrderByWaktuPengajuanDesc(transaksi.getId()).orElseThrow();
        assertEquals(Refund.StatusRefund.PENDING, refund.getStatus());
        assertEquals("12345678", refund.getNomorRekening());
        assertEquals(Transaksi.RefundStatus.PENDING_REFUND,
                transaksiRepository.findById(transaksi.getId()).orElseThrow().getRefundStatus());

        mockMvc.perform(get("/detail-tiket").with(user("refund-customer@test.local").roles("USER")).param("transaksiId", transaksi.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Menunggu persetujuan admin")));

        mockMvc.perform(post("/admin/refund/approve/" + refund.getId()).with(csrf())
                        .with(user("admin@test.local").roles("ADMIN")))
                .andExpect(status().is3xxRedirection());
        assertEquals(Tiket.StatusTiket.REFUNDED, tiketRepository.findById(tiket.getId()).orElseThrow().getStatus());
        assertTrue(!tiketRepository.isSeatBooked(tiket.getJadwal(), "A1", java.util.List.of(Tiket.StatusTiket.REFUNDED)));

        mockMvc.perform(post("/admin/refund/process/" + refund.getId()).with(csrf())
                        .with(user("admin@test.local").roles("ADMIN")))
                .andExpect(status().is3xxRedirection());
        assertEquals(Refund.StatusRefund.PROCESSED, refundRepository.findById(refund.getId()).orElseThrow().getStatus());
        assertEquals(Transaksi.RefundStatus.REFUNDED,
                transaksiRepository.findById(transaksi.getId()).orElseThrow().getRefundStatus());
    }

    @Test
    void cannotRequestRefundInsideConfiguredWindow() throws Exception {
        Jadwal jadwal = tiket.getJadwal();
        jadwal.setWaktuMulai(LocalDateTime.now().plusMinutes(30));
        jadwal.setWaktuSelesai(LocalDateTime.now().plusHours(2));
        jadwalRepository.save(jadwal);

        mockMvc.perform(post("/ajukan-refund").with(user("refund-customer@test.local").roles("USER")).with(csrf())
                        .param("transaksiId", transaksi.getId().toString())
                        .param("alasan", "Jadwal berubah")
                        .param("namaPemilikRekening", "Ayu")
                        .param("nomorRekening", "12345678"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("batas H-2 jam")));
        assertTrue(refundRepository.findAll().isEmpty());
    }

    @Test
    void pendingPaymentCannotRequestRefund() throws Exception {
        transaksi.setStatus(Transaksi.StatusTransaksi.PENDING);
        transaksiRepository.save(transaksi);

        mockMvc.perform(post("/ajukan-refund").with(user("refund-customer@test.local").roles("USER")).with(csrf())
                        .param("transaksiId", transaksi.getId().toString())
                        .param("alasan", "Belum dibayar")
                        .param("namaPemilikRekening", "Ayu")
                        .param("nomorRekening", "12345678"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Refund tidak dapat diajukan")));
        assertTrue(refundRepository.findAll().isEmpty());
    }

    @Test
    void adminTransactionPageRetainsRefundActionAndRequiresPayoutDetails() throws Exception {
        mockMvc.perform(get("/admin/manajemen-transaksi").with(user("admin@test.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ajukan Refund")))
                .andExpect(content().string(containsString("namaPemilikRekening")))
                .andExpect(content().string(containsString("nomorRekening")));

        mockMvc.perform(post("/admin/transaksi/ajukan-refund")
                        .with(user("admin@test.local").roles("ADMIN")).with(csrf())
                        .param("transaksiId", transaksi.getId().toString())
                        .param("alasan", "Permintaan pelanggan"))
                .andExpect(status().isBadRequest());
        assertTrue(refundRepository.findAll().isEmpty());
    }

    @Test
    void adminCanStillSubmitValidRefundWithPayoutData() throws Exception {
        mockMvc.perform(post("/admin/transaksi/ajukan-refund")
                        .with(user("admin@test.local").roles("ADMIN")).with(csrf())
                        .param("transaksiId", transaksi.getId().toString())
                        .param("alasan", "Permintaan pelanggan")
                        .param("namaPemilikRekening", "Ayu")
                        .param("nomorRekening", "12345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/manajemen-transaksi?refundRequested=true"));
        Refund refund = refundRepository.findFirstByTransaksiIdOrderByWaktuPengajuanDesc(transaksi.getId()).orElseThrow();
        assertEquals("12345678", refund.getNomorRekening());
        assertEquals(Transaksi.RefundStatus.PENDING_REFUND,
                transaksiRepository.findById(transaksi.getId()).orElseThrow().getRefundStatus());
    }

    @Test
    void rejectedRefundReturnsPaidStatusAndShowsAdminReasonToOwner() throws Exception {
        mockMvc.perform(post("/ajukan-refund").with(user("refund-customer@test.local").roles("USER")).with(csrf())
                        .param("transaksiId", transaksi.getId().toString())
                        .param("alasan", "Saya tidak dapat hadir")
                        .param("namaPemilikRekening", "Ayu")
                        .param("nomorRekening", "12345678"))
                .andExpect(status().is3xxRedirection());
        Refund refund = refundRepository.findFirstByTransaksiIdOrderByWaktuPengajuanDesc(transaksi.getId()).orElseThrow();

        mockMvc.perform(post("/admin/refund/reject/" + refund.getId()).with(user("admin@test.local").roles("ADMIN")).with(csrf())
                        .param("alasanTolak", "Jadwal sudah terlalu dekat"))
                .andExpect(status().is3xxRedirection());

        assertEquals(Refund.StatusRefund.REJECTED, refundRepository.findById(refund.getId()).orElseThrow().getStatus());
        assertEquals(Transaksi.StatusTransaksi.SUCCESS,
                transaksiRepository.findById(transaksi.getId()).orElseThrow().getStatus());
        assertEquals(Tiket.StatusTiket.VALID, tiketRepository.findById(tiket.getId()).orElseThrow().getStatus());
        mockMvc.perform(get("/detail-tiket").with(user("refund-customer@test.local").roles("USER"))
                        .param("transaksiId", transaksi.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ditolak (transaksi kembali paid)")))
                .andExpect(content().string(containsString("Jadwal sudah terlalu dekat")));
    }
}
