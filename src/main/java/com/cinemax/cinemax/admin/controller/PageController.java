/**
 * Tujuan program: Menyediakan halaman publik CineMax menggunakan data film,
 * banner CMS, dan jadwal yang tersimpan di database.
 *
 * Contributor: 'Aarif Rahmaan J. Faqiih
 * NIM: 103112430182
 * Role: User
 * Kelas: IF-12-07
 * Terakhir diubah: 27 September 2026, 00:00 WIB
 */
package com.cinemax.cinemax.admin.controller;

import com.cinemax.cinemax.domain.booking.Promosi;
import com.cinemax.cinemax.domain.booking.PromosiRepository;
import com.cinemax.cinemax.domain.movie.Film;
import com.cinemax.cinemax.domain.movie.FilmRepository;
import com.cinemax.cinemax.domain.movie.GenreRepository;
import com.cinemax.cinemax.domain.schedule.Jadwal;
import com.cinemax.cinemax.domain.schedule.JadwalRepository;
import com.cinemax.cinemax.domain.schedule.Studio;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@Transactional(readOnly = true)
public class PageController {

    private static final String JELAJAH_FILM_URL = "/jelajah";

    private static final int JUMLAH_HARI_JADWAL = 7;

    private final FilmRepository filmRepository;

    private final GenreRepository genreRepository;

    private final JadwalRepository jadwalRepository;

    private final PromosiRepository promosiRepository;

    private final com.cinemax.cinemax.domain.schedule.KursiRepository kursiRepository;
    private final com.cinemax.cinemax.domain.booking.TiketRepository tiketRepository;
    private final com.cinemax.cinemax.domain.booking.TransaksiRepository transaksiRepository;
    private final com.cinemax.cinemax.domain.user.UserRepository userRepository;
    private final com.cinemax.cinemax.domain.booking.RefundRepository refundRepository;

    /**
     * Membuat controller dengan seluruh dependency pembacaan konten publik.
     * Constructor injection menjaga setiap dependency wajib terlihat jelas dan
     * memudahkan controller diuji secara terpisah.
     */
    public PageController(
            FilmRepository filmRepository,
            GenreRepository genreRepository,
            JadwalRepository jadwalRepository,
            PromosiRepository promosiRepository,
            com.cinemax.cinemax.domain.schedule.KursiRepository kursiRepository,
            com.cinemax.cinemax.domain.booking.TiketRepository tiketRepository,
            com.cinemax.cinemax.domain.booking.TransaksiRepository transaksiRepository,
            com.cinemax.cinemax.domain.user.UserRepository userRepository,
            com.cinemax.cinemax.domain.booking.RefundRepository refundRepository) {
        this.filmRepository = filmRepository;
        this.genreRepository = genreRepository;
        this.jadwalRepository = jadwalRepository;
        this.promosiRepository = promosiRepository;
        this.kursiRepository = kursiRepository;
        this.tiketRepository = tiketRepository;
        this.transaksiRepository = transaksiRepository;
        this.userRepository = userRepository;
        this.refundRepository = refundRepository;
    }

    /**
     * Menampilkan beranda dengan banner CMS aktif dan film yang sedang tayang.
     */
    @GetMapping("/")
    public String beranda(Model model) {
        List<Film> filmSedangTayang = getFilmSedangTayang();
        List<Promosi> promosiAktif = getPromosiAktif();

        model.addAttribute("featuredFilm", getFilmUnggulan(filmSedangTayang));
        model.addAttribute("filmSedangTayang", filmSedangTayang);
        model.addAttribute("promosiAktif", promosiAktif);
        model.addAttribute("promotionTargetUrls", getPromotionTargetUrls(promosiAktif));

        return "beranda";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    /**
     * Menampilkan seluruh film yang boleh ditelusuri pelanggan beserta genre
     * dari database untuk filter di sisi peramban.
     */
    @GetMapping(JELAJAH_FILM_URL)
    public String jelajah(Model model) {
        model.addAttribute("films", getFilmPublik());
        model.addAttribute("genres", genreRepository.findAll());

        return "jelajah";
    }

    /**
     * Menampilkan detail promosi berdasarkan ID.
     */
    @GetMapping("/promo/detail-{id}")
    public String detailPromosi(@PathVariable Long id, Model model) {
        Promosi promosi = promosiRepository.findById(id).orElse(null);
        if (promosi == null || !promosi.getIsAktif()) {
            return "redirect:/";
        }
        model.addAttribute("promosi", promosi);
        return "detail_promosi";
    }

    /**
     * Menampilkan detail satu film berdasarkan ID, bukan file HTML statis.
     *
     * @param filmId ID film dari URL
     * @param model  model Thymeleaf untuk halaman detail
     * @return nama template detail film
     */
    @GetMapping("/film/{filmId}")
    public String detailFilm(
            @PathVariable Long filmId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggal,
            Model model) {
        Film film = findFilmPublik(filmId);
        List<Film> filmSerupa = getFilmSedangTayang()
                .stream()
                .filter(candidateFilm -> !Objects.equals(candidateFilm.getId(), film.getId()))
                .limit(4)
                .toList();

        LocalDate tanggalHariIni = LocalDate.now();
        LocalDate tanggalTerpilih = getTanggalJadwalValid(tanggal, tanggalHariIni);
        LocalDateTime waktuMulaiHari = tanggalTerpilih.atStartOfDay();
        LocalDateTime waktuSelesaiHari = waktuMulaiHari.plusDays(1);
        List<Jadwal> jadwalAktif = jadwalRepository
                .findByFilmIdAndWaktuMulaiGreaterThanEqualAndWaktuMulaiLessThanAndStatusOrderByWaktuMulaiAsc(
                        film.getId(),
                        waktuMulaiHari,
                        waktuSelesaiHari,
                        Jadwal.StatusJadwal.AKTIF);

        model.addAttribute("film", film);
        model.addAttribute("filmSerupa", filmSerupa);
        model.addAttribute("selectedDate", tanggalTerpilih);
        model.addAttribute("scheduleDays", getHariJadwal(tanggalHariIni));
        model.addAttribute("schedulesByStudio", groupJadwalByStudio(jadwalAktif));

        return "detail_film";
    }

    @GetMapping("/profil")
    public String profil(Model model) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
        com.cinemax.cinemax.domain.user.User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            model.addAttribute("currentUser", user);
            String initials = java.util.Arrays.stream(user.getNamaLengkap().split(" "))
                    .map(s -> s.substring(0, 1))
                    .limit(2)
                    .collect(java.util.stream.Collectors.joining())
                    .toUpperCase();
            model.addAttribute("userInitials", initials);
        }
        return "profil";
    }

    @org.springframework.web.bind.annotation.PostMapping("/edit-profil")
    @Transactional(readOnly = false)
    public String editProfil(@RequestParam String namaLengkap, @RequestParam(required = false) String password) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
        com.cinemax.cinemax.domain.user.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pengguna tidak ditemukan."));

        user.setNamaLengkap(namaLengkap);

        if (password != null && !password.isBlank()) {
            org.springframework.security.crypto.password.PasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            user.setPassword(encoder.encode(password));
        }

        userRepository.save(user);

        return "redirect:/profil?success=true";
    }

    @GetMapping("/pilih-kursi")
    public String pilihKursi(@RequestParam Long jadwalId, Model model) {
        Jadwal jadwal = jadwalRepository.findById(jadwalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jadwal tidak ditemukan."));

        List<com.cinemax.cinemax.domain.schedule.Kursi> semuaKursi = kursiRepository
                .findByStudioId(jadwal.getStudio().getId());
        List<com.cinemax.cinemax.domain.booking.Tiket> tiketTerjual = tiketRepository.findByJadwalIdAndStatus(jadwalId,
                com.cinemax.cinemax.domain.booking.Tiket.StatusTiket.VALID);
        java.util.Set<String> kursiTerisi = tiketTerjual.stream()
                .map(com.cinemax.cinemax.domain.booking.Tiket::getNomorKursi)
                .collect(java.util.stream.Collectors.toSet());

        semuaKursi.sort(java.util.Comparator.comparing(com.cinemax.cinemax.domain.schedule.Kursi::getBaris)
                .thenComparing(com.cinemax.cinemax.domain.schedule.Kursi::getKolom));

        model.addAttribute("jadwal", jadwal);
        model.addAttribute("semuaKursi", semuaKursi);
        model.addAttribute("kursiTerisi", kursiTerisi);

        return "pilih_kursi";
    }

    @GetMapping("/lupa-password")
    public String lupaPassword() {
        return "lupa_password";
    }

    /**
     * Menampilkan jadwal aktif film pilihan. Tanggal dibatasi ke tujuh hari
     * berikutnya agar URL yang dimanipulasi tidak menjadi jalur akses data
     * jadwal yang tidak dimaksudkan untuk pelanggan.
     */
    @GetMapping("/pilih-jadwal")
    public String pilihJadwal(
            @RequestParam Long filmId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggal,
            Model model) {
        Film film = findFilmPublik(filmId);
        LocalDate tanggalHariIni = LocalDate.now();
        LocalDate tanggalTerpilih = getTanggalJadwalValid(tanggal, tanggalHariIni);
        LocalDateTime waktuMulaiHari = tanggalTerpilih.atStartOfDay();
        LocalDateTime waktuSelesaiHari = waktuMulaiHari.plusDays(1);
        List<Jadwal> jadwalAktif = jadwalRepository
                .findByFilmIdAndWaktuMulaiGreaterThanEqualAndWaktuMulaiLessThanAndStatusOrderByWaktuMulaiAsc(
                        film.getId(),
                        waktuMulaiHari,
                        waktuSelesaiHari,
                        Jadwal.StatusJadwal.AKTIF);

        model.addAttribute("film", film);
        model.addAttribute("selectedDate", tanggalTerpilih);
        model.addAttribute("scheduleDays", getHariJadwal(tanggalHariIni));
        model.addAttribute("schedulesByStudio", groupJadwalByStudio(jadwalAktif));

        return "pilih_jadwal";
    }

    @GetMapping("/daftar-tiket")
    public String daftarTiket(@RequestParam(required = false, defaultValue = "semua") String tab, Model model) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
        com.cinemax.cinemax.domain.user.User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            java.util.List<com.cinemax.cinemax.domain.booking.Tiket> allTikets = tiketRepository
                    .findByTransaksiPelangganOrderByTransaksiTanggalTransaksiDesc(user);
            
            LocalDateTime now = LocalDateTime.now();
            Map<com.cinemax.cinemax.domain.booking.Transaksi, java.util.List<com.cinemax.cinemax.domain.booking.Tiket>> grouped = new LinkedHashMap<>();
            
            for (com.cinemax.cinemax.domain.booking.Tiket tiket : allTikets) {
                com.cinemax.cinemax.domain.booking.Transaksi tx = tiket.getTransaksi();
                boolean isPast = tiket.getJadwal().getWaktuMulai().isBefore(now);
                
                boolean include = false;
                if ("semua".equals(tab)) {
                    include = true;
                } else if ("aktif".equals(tab)) {
                    if (tx.getStatus() == com.cinemax.cinemax.domain.booking.Transaksi.StatusTransaksi.SUCCESS && !isPast) {
                        include = true;
                    }
                } else if ("pending".equals(tab)) {
                    if (tx.getStatus() == com.cinemax.cinemax.domain.booking.Transaksi.StatusTransaksi.PENDING) {
                        include = true;
                    }
                } else if ("selesai".equals(tab)) {
                    if (tx.getStatus() == com.cinemax.cinemax.domain.booking.Transaksi.StatusTransaksi.REFUND || 
                        (tx.getStatus() == com.cinemax.cinemax.domain.booking.Transaksi.StatusTransaksi.SUCCESS && isPast)) {
                        include = true;
                    }
                }
                
                if (include) {
                    grouped.computeIfAbsent(tx, k -> new ArrayList<>()).add(tiket);
                }
            }
            model.addAttribute("transaksiTiketMap", grouped);
            model.addAttribute("activeTab", tab);
        }
        return "daftar_tiket";
    }

    @GetMapping("/detail-tiket")
    public String detailTiket(@RequestParam Long transaksiId, Model model) {
        com.cinemax.cinemax.domain.booking.Transaksi transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaksi tidak ditemukan."));

        java.util.List<com.cinemax.cinemax.domain.booking.Tiket> tiketList = tiketRepository
                .findByTransaksiId(transaksiId);
        if (tiketList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tiket tidak ditemukan.");
        }

        model.addAttribute("transaksi", transaksi);
        model.addAttribute("tiketList", tiketList);
        model.addAttribute("jadwal", tiketList.get(0).getJadwal());

        return "detail_tiket";
    }

    @org.springframework.web.bind.annotation.PostMapping("/ajukan-refund")
    @Transactional(readOnly = false)
    public String ajukanRefund(@RequestParam Long transaksiId, @RequestParam String alasan) {
        com.cinemax.cinemax.domain.booking.Transaksi transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaksi tidak ditemukan."));

        String currentUserEmail = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();

        if (!transaksi.getPelanggan().getEmail().equals(currentUserEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Anda tidak berhak mengajukan refund untuk transaksi ini.");
        }

        if (transaksi.getStatus() != com.cinemax.cinemax.domain.booking.Transaksi.StatusTransaksi.SUCCESS ||
                transaksi.getRefundStatus() != com.cinemax.cinemax.domain.booking.Transaksi.RefundStatus.NONE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund tidak dapat diajukan.");
        }

        com.cinemax.cinemax.domain.booking.Refund refund = new com.cinemax.cinemax.domain.booking.Refund();
        refund.setTransaksi(transaksi);
        refund.setAlasan(alasan);
        refund.setStatus(com.cinemax.cinemax.domain.booking.Refund.StatusRefund.PENDING);
        refund.setDiajukanOleh(currentUserEmail);
        refund.setWaktuPengajuan(LocalDateTime.now());

        refundRepository.save(refund);

        transaksi.setRefundStatus(com.cinemax.cinemax.domain.booking.Transaksi.RefundStatus.PENDING_REFUND);
        transaksiRepository.save(transaksi);

        return "redirect:/detail-tiket?transaksiId=" + transaksiId;
    }

    @GetMapping("/detail-transaksi")
    public String detailTransaksi() {
        return "detail_transaksi";
    }

    @GetMapping("/faktur")
    public String fakturPesanan(
            @RequestParam Long transaksiId,
            @RequestParam(required = false) String token,
            Model model) {
        com.cinemax.cinemax.domain.booking.Transaksi transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaksi tidak ditemukan."));

        java.util.List<com.cinemax.cinemax.domain.booking.Tiket> tiketList = tiketRepository
                .findByTransaksiId(transaksiId);

        if (tiketList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tiket tidak ditemukan.");
        }

        Jadwal jadwal = tiketList.get(0).getJadwal();

        model.addAttribute("transaksi", transaksi);
        model.addAttribute("tiketList", tiketList);
        model.addAttribute("jadwal", jadwal);
        model.addAttribute("token", token);

        return "faktur_pesanan";
    }

    /**
     * Mengembalikan film yang sudah boleh dibeli/ditelusuri pelanggan.
     */
    private List<Film> getFilmPublik() {
        List<Film> filmPublik = new ArrayList<>();

        filmPublik.addAll(getFilmSedangTayang());
        filmPublik.addAll(filmRepository.findByStatusOrderByJudulAsc(Film.StatusFilm.SEGERA));

        return filmPublik;
    }

    private List<Film> getFilmSedangTayang() {
        return filmRepository.findByStatusOrderByJudulAsc(Film.StatusFilm.SEDANG_TAYANG);
    }

    private Film getFilmUnggulan(List<Film> filmSedangTayang) {
        if (filmSedangTayang.isEmpty()) {
            return null;
        }

        return filmSedangTayang.get(0);
    }

    /**
     * Menolak ID film yang tidak ada atau statusnya tidak publik agar film yang
     * ditarik dari katalog tidak dapat tetap diakses lewat URL lama.
     */
    private Film findFilmPublik(Long filmId) {
        Film film = filmRepository
                .findById(filmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Film tidak ditemukan."));

        if (film.getStatus() == Film.StatusFilm.TIDAK_TAYANG || film.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Film tidak tersedia.");
        }

        return film;
    }

    /**
     * Mengambil hanya banner aktif yang tanggalnya meliputi hari ini.
     */
    private List<Promosi> getPromosiAktif() {
        LocalDate tanggalHariIni = LocalDate.now();

        return promosiRepository
                .findByIsAktifTrueAndTanggalMulaiLessThanEqualAndTanggalAkhirGreaterThanEqualOrderByTanggalMulaiDesc(
                        tanggalHariIni,
                        tanggalHariIni);
    }

    /**
     * Menyiapkan tautan banner yang aman untuk digunakan pada atribut href.
     * Tautan tanpa target, URL rusak, dan skema selain HTTP(S) kembali ke
     * katalog film sehingga banner selalu dapat diklik tanpa membuka skema
     * berbahaya seperti javascript:.
     */
    private Map<Long, String> getPromotionTargetUrls(List<Promosi> promosiAktif) {
        Map<Long, String> targetUrls = new LinkedHashMap<>();

        for (Promosi promosi : promosiAktif) {
            targetUrls.put(promosi.getId(), getSafePromotionTargetUrl(promosi.getTargetUrl()));
        }

        return targetUrls;
    }

    private String getSafePromotionTargetUrl(String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            return JELAJAH_FILM_URL;
        }

        String cleanTargetUrl = targetUrl.trim();

        if (cleanTargetUrl.startsWith("/") && !cleanTargetUrl.startsWith("//")) {
            return cleanTargetUrl;
        }

        try {
            URI targetUri = new URI(cleanTargetUrl);
            String scheme = targetUri.getScheme();

            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                return targetUri.toASCIIString();
            }
        } catch (URISyntaxException uriSyntaxException) {
            // URL CMS tidak valid; gunakan halaman aman sebagai fallback.
        }

        return JELAJAH_FILM_URL;
    }

    private LocalDate getTanggalJadwalValid(LocalDate tanggal, LocalDate tanggalHariIni) {
        LocalDate tanggalMaksimum = tanggalHariIni.plusDays(JUMLAH_HARI_JADWAL - 1);

        if (tanggal == null || tanggal.isBefore(tanggalHariIni) || tanggal.isAfter(tanggalMaksimum)) {
            return tanggalHariIni;
        }

        return tanggal;
    }

    private List<LocalDate> getHariJadwal(LocalDate tanggalHariIni) {
        List<LocalDate> hariJadwal = new ArrayList<>();

        for (int offsetHari = 0; offsetHari < JUMLAH_HARI_JADWAL; offsetHari++) {
            hariJadwal.add(tanggalHariIni.plusDays(offsetHari));
        }

        return hariJadwal;
    }

    /**
     * Mengelompokkan jadwal berdasarkan studio dengan LinkedHashMap untuk
     * mempertahankan urutan mulai pertunjukan dari hasil kueri database.
     */
    private Map<Studio, List<Jadwal>> groupJadwalByStudio(List<Jadwal> jadwalAktif) {
        Map<Studio, List<Jadwal>> jadwalByStudio = new LinkedHashMap<>();

        for (Jadwal jadwal : jadwalAktif) {
            Studio studio = jadwal.getStudio();

            jadwalByStudio.computeIfAbsent(studio, unusedStudio -> new ArrayList<>()).add(jadwal);
        }

        return jadwalByStudio;
    }
}
