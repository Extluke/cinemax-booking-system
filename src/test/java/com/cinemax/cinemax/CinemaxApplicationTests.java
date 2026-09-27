/**
 * Tujuan program: Memverifikasi halaman publik CineMax menggunakan data database uji.
 * Contributor: 'Aarif Rahmaan J. Faqiih
 * NIM: 103112430182
 * Role: User
 * Kelas: IF-12-07
 * Terakhir diubah: 27 September 2026, 00:00 WIB
 */
package com.cinemax.cinemax;

import com.cinemax.cinemax.domain.booking.Promosi;
import com.cinemax.cinemax.domain.booking.PromosiRepository;
import com.cinemax.cinemax.domain.config.BioskopConfig;
import com.cinemax.cinemax.domain.config.BioskopConfigRepository;
import com.cinemax.cinemax.domain.movie.Film;
import com.cinemax.cinemax.domain.movie.FilmRepository;
import com.cinemax.cinemax.domain.movie.Genre;
import com.cinemax.cinemax.domain.movie.GenreRepository;
import com.cinemax.cinemax.domain.movie.TipeStudio;
import com.cinemax.cinemax.domain.movie.TipeStudioRepository;
import com.cinemax.cinemax.domain.schedule.Jadwal;
import com.cinemax.cinemax.domain.schedule.JadwalRepository;
import com.cinemax.cinemax.domain.schedule.Studio;
import com.cinemax.cinemax.domain.schedule.StudioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CinemaxApplicationTests {

    private static final String ALAMAT_DATABASE = "Jl. Database No. 27, Bandung";

    private static final String JUDUL_FILM = "Film Database";

    private static final String NAMA_TIPE_STUDIO = "IMAX Database";

    private static final String JUDUL_PROMOSI = "Promo Database";

    @Autowired
    private BioskopConfigRepository bioskopConfigRepository;

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private JadwalRepository jadwalRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PromosiRepository promosiRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private TipeStudioRepository tipeStudioRepository;

    private Film filmDatabase;

    /**
     * Menyiapkan satu rangkaian data database yang mencakup seluruh konten yang
     * tampil di halaman publik. Setiap objek disimpan sebelum direlasikan untuk
     * meniru foreign key pada skema produksi.
     */
    @BeforeEach
    void setUpDataPublik() {
        // Urutan hapus mengikuti relasi foreign key agar data antar-test terisolasi.
        jadwalRepository.deleteAll();
        promosiRepository.deleteAll();
        studioRepository.deleteAll();
        tipeStudioRepository.deleteAll();
        filmRepository.deleteAll();
        genreRepository.deleteAll();
        bioskopConfigRepository.deleteAll();

        BioskopConfig bioskopConfig = new BioskopConfig();
        bioskopConfig.setNamaBioskop("CineMax Database");
        bioskopConfig.setAlamatBioskop(ALAMAT_DATABASE);
        bioskopConfig.setKontakCs("081234567890");
        bioskopConfigRepository.save(bioskopConfig);

        Genre genre = new Genre("Aksi Database");
        genreRepository.save(genre);

        filmDatabase = new Film();
        filmDatabase.setJudul(JUDUL_FILM);
        filmDatabase.setSinopsis("Sinopsis dari database.");
        filmDatabase.setDurasi(120);
        filmDatabase.setBatasUsia("13+");
        filmDatabase.setStatus(Film.StatusFilm.SEDANG_TAYANG);
        filmDatabase.setGenres(new HashSet<>());
        filmDatabase.getGenres().add(genre);
        filmRepository.save(filmDatabase);

        TipeStudio tipeStudio = new TipeStudio(NAMA_TIPE_STUDIO);
        tipeStudioRepository.save(tipeStudio);

        Studio studio = new Studio();
        studio.setNama("Studio Database");
        studio.setTipe(tipeStudio);
        studio.setKapasitas(100);
        studio.setStatus(Studio.StatusStudio.AKTIF);
        studioRepository.save(studio);

        Jadwal jadwal = new Jadwal();
        jadwal.setFilm(filmDatabase);
        jadwal.setStudio(studio);
        jadwal.setHarga(50000.0);
        jadwal.setStatus(Jadwal.StatusJadwal.AKTIF);
        jadwal.setWaktuMulai(LocalDateTime.now().toLocalDate().atTime(14, 0));
        jadwal.setWaktuSelesai(LocalDateTime.now().toLocalDate().atTime(16, 0));
        jadwalRepository.save(jadwal);

        Promosi promosi = new Promosi();
        promosi.setJudul(JUDUL_PROMOSI);
        promosi.setPosterUrl("/img/placeholder.jpg");
        promosi.setTargetUrl("/film/" + filmDatabase.getId());
        promosi.setTanggalMulai(LocalDate.now().minusDays(1));
        promosi.setTanggalAkhir(LocalDate.now().plusDays(1));
        promosi.setIsAktif(true);
        promosiRepository.save(promosi);
    }

    /**
     * Memastikan halaman beranda memakai banner, film, alamat, dan target link
     * yang sama dengan data database.
     */
    @Test
    void berandaMenampilkanKontenDatabaseYangDapatDiklik() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(JUDUL_PROMOSI)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(JUDUL_FILM)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(ALAMAT_DATABASE)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/film/" + filmDatabase.getId())));
    }

    /**
     * Memastikan katalog, detail, dan jadwal menggunakan rute film yang sama
     * serta menampilkan tipe studio dari relasi database.
     */
    @Test
    void katalogDetailDanJadwalMenggunakanDataDatabase() throws Exception {
        String detailFilmUrl = "/film/" + filmDatabase.getId();
        String jadwalFilmUrl = "/pilih-jadwal?filmId=" + filmDatabase.getId();

        mockMvc.perform(get("/jelajah"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(detailFilmUrl)));

        mockMvc.perform(get(detailFilmUrl))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sinopsis dari database.")));

        mockMvc.perform(get(jadwalFilmUrl))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(NAMA_TIPE_STUDIO)));
    }
}
