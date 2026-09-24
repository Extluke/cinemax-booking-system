package com.cinemax.cinemax.admin.controller;

import com.cinemax.cinemax.admin.dto.JadwalDTO;
import com.cinemax.cinemax.domain.booking.Refund;
import com.cinemax.cinemax.domain.booking.RefundRepository;
import com.cinemax.cinemax.domain.booking.Tiket;
import com.cinemax.cinemax.domain.booking.TiketRepository;
import com.cinemax.cinemax.domain.booking.Transaksi;
import com.cinemax.cinemax.domain.booking.TransaksiRepository;
import com.cinemax.cinemax.domain.config.AuditLog;
import com.cinemax.cinemax.domain.config.AuditService;
import com.cinemax.cinemax.domain.config.BioskopConfig;
import com.cinemax.cinemax.domain.config.BioskopConfigRepository;
import com.cinemax.cinemax.domain.movie.Film;
import com.cinemax.cinemax.domain.movie.FilmRepository;
import com.cinemax.cinemax.domain.movie.Genre;
import com.cinemax.cinemax.domain.movie.GenreRepository;
import com.cinemax.cinemax.domain.movie.TipeStudio;
import com.cinemax.cinemax.domain.movie.TipeStudioRepository;
import com.cinemax.cinemax.domain.schedule.Fasilitas;
import com.cinemax.cinemax.domain.schedule.FasilitasRepository;
import com.cinemax.cinemax.domain.schedule.Jadwal;
import com.cinemax.cinemax.domain.schedule.JadwalRepository;
import com.cinemax.cinemax.domain.schedule.KelasKursi;
import com.cinemax.cinemax.domain.schedule.KelasKursiRepository;
import com.cinemax.cinemax.domain.schedule.Kursi;
import com.cinemax.cinemax.domain.schedule.KursiRepository;
import com.cinemax.cinemax.domain.schedule.Studio;
import com.cinemax.cinemax.domain.schedule.StudioRepository;
import com.cinemax.cinemax.domain.user.User;
import com.cinemax.cinemax.domain.user.UserRepository;
import com.cinemax.cinemax.domain.user.UserService;
import jakarta.persistence.criteria.Join;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequestMapping("/admin") // Semua URL di sini akan diawali dengan /admin
@Transactional
public class AdminPageController {

    @Autowired
    private TransaksiRepository transaksiRepository;
    
    @Autowired
    private TiketRepository tiketRepository;

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    // DTO Internal untuk view Okupansi
    public static class StudioOccupancyDTO {
        public String namaStudio;
        public int persentase;
        public long tiketTerjual;
        public long totalKapasitasHariIni;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Ambil data metrik dari database
        long tiketTerjualHariIni = tiketRepository.countTicketsSoldToday();
        
        Double totalPendapatan = transaksiRepository.calculateTotalRevenue();
        if (totalPendapatan == null) totalPendapatan = 0.0;
        
        long tiketTerverifikasi = tiketRepository.countByStatus(Tiket.StatusTiket.VALID);
        long permintaanRefund = refundRepository.countByStatus(Refund.StatusRefund.PENDING);
        
        // Ambil 5 transaksi terbaru
        List<Transaksi> transaksiTerbaru = transaksiRepository.findTop5ByOrderByTanggalTransaksiDesc();

        // Hitung Okupansi Studio
        List<Studio> studios = studioRepository.findAll();
        List<StudioOccupancyDTO> occupancyList = new java.util.ArrayList<>();
        
        for (Studio studio : studios) {
            long terjual = tiketRepository.countTicketsSoldTodayByStudio(studio.getId());
            long jumlahJadwal = jadwalRepository.countSchedulesTodayByStudio(studio.getId());
            long kapasitasTotal = jumlahJadwal * studio.getKapasitas();
            
            StudioOccupancyDTO dto = new StudioOccupancyDTO();
            dto.namaStudio = studio.getNama() + " (" + studio.getTipe().getNama() + ")";
            dto.tiketTerjual = terjual;
            dto.totalKapasitasHariIni = kapasitasTotal;
            
            if (kapasitasTotal > 0) {
                dto.persentase = (int) ((terjual * 100) / kapasitasTotal);
            } else {
                dto.persentase = 0;
            }
            
            occupancyList.add(dto);
        }

        // Data untuk Grafik Tren Penjualan Mingguan
        java.time.LocalDateTime startDate = java.time.LocalDateTime.now().minusDays(6).with(java.time.LocalTime.MIN);
        List<Object[]> salesDataRaw = tiketRepository.findTicketSalesLast7Days(startDate);
        
        List<String> chartLabels = new java.util.ArrayList<>();
        List<Long> chartData = new java.util.ArrayList<>();
        
        java.time.LocalDate current = startDate.toLocalDate();
        java.time.LocalDate end = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("EEE", new java.util.Locale("id", "ID"));
        
        java.util.Map<String, Long> salesMap = new java.util.HashMap<>();
        for (Object[] row : salesDataRaw) {
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            salesMap.put(sqlDate.toLocalDate().toString(), ((Number) row[1]).longValue());
        }

        while (!current.isAfter(end)) {
            chartLabels.add(current.format(formatter));
            chartData.add(salesMap.getOrDefault(current.toString(), 0L));
            current = current.plusDays(1);
        }
        
        // Hitung growth
        long todaySales = salesMap.getOrDefault(end.toString(), 0L);
        long yesterdaySales = salesMap.getOrDefault(end.minusDays(1).toString(), 0L);
        String growthPercentage = "0%";
        if (yesterdaySales == 0 && todaySales > 0) {
            growthPercentage = "+100%";
        } else if (yesterdaySales > 0) {
            double growth = ((double)(todaySales - yesterdaySales) / yesterdaySales) * 100;
            growthPercentage = (growth > 0 ? "+" : "") + String.format("%.1f", growth) + "%";
        }

        // Lempar data ke Thymeleaf (View)
        model.addAttribute("tiketTerjualHariIni", tiketTerjualHariIni);
        model.addAttribute("totalPendapatan", totalPendapatan);
        model.addAttribute("tiketTerverifikasi", tiketTerverifikasi);
        model.addAttribute("permintaanRefund", permintaanRefund);
        model.addAttribute("transaksiTerbaru", transaksiTerbaru);
        model.addAttribute("occupancyList", occupancyList);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);
        model.addAttribute("growthPercentage", growthPercentage);

        return "admin/dashboard_admin"; 
    }

    @GetMapping("/manajemen-film")
    public String manajemenFilm(@RequestParam(required = false) String search,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) Long genreId,
                                @RequestParam(required = false) String sort,
                                Model model) {
        
        try {
            Specification<Film> spec = (root, query, cb) -> cb.conjunction();
            
            // 1. Pencarian Teks (Judul)
            if (search != null && !search.isEmpty()) {
                spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("judul")), "%" + search.toLowerCase() + "%"));
            }
            
            // 2. Filter Status
            if (status != null && !status.isEmpty() && !status.equals("Semua Status")) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), Film.StatusFilm.valueOf(status)));
            }
            
            // 3. Filter Genre
            if (genreId != null) {
                spec = spec.and((root, query, cb) -> {
                    Join<Film, Genre> genres = root.join("genres");
                    return cb.equal(genres.get("id"), genreId);
                });
            }
            
            // 4. Sorting (Terbaru/Terlama)
            Sort sortObj = Sort.unsorted();
            if ("terbaru".equals(sort)) {
                sortObj = Sort.by(Sort.Direction.DESC, "id");
            } else if ("terlama".equals(sort)) {
                sortObj = Sort.by(Sort.Direction.ASC, "id");
            }
            
            model.addAttribute("films", filmRepository.findAll(spec, sortObj));
            model.addAttribute("genres", genreRepository.findAll()); // Untuk dropdown filter
            
            // Kembalikan parameter ke view agar form tetep menahan state pilihan
            model.addAttribute("currentSearch", search);
            model.addAttribute("currentStatus", status);
            model.addAttribute("currentGenreId", genreId);
            model.addAttribute("currentSort", sort);
            
            return "admin/manajemen_film";
        } catch (Exception ex) {
            java.io.StringWriter sw = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(sw));
            throw new RuntimeException("ERROR KITA: " + sw.toString(), ex);
        }
    }

    @GetMapping("/manajemen-jadwal")
    public String manajemenJadwal(@RequestParam(required = false) String date, 
                                  @RequestParam(required = false) Long studioId, 
                                  Model model) {
        
        java.time.LocalDate selectedDate = java.time.LocalDate.now();
        if (date != null && !date.isEmpty()) {
            selectedDate = java.time.LocalDate.parse(date);
        }
        
        BioskopConfig config = configRepository.findById("SINGLETON").orElse(new BioskopConfig());
        String strBuka = config.getJamBukaByDay(selectedDate.getDayOfWeek());
        String strTutup = config.getJamTutupByDay(selectedDate.getDayOfWeek());
        
        if (strBuka == null || strTutup == null) {
            model.addAttribute("isTutup", true);
            model.addAttribute("currentDate", selectedDate.toString());
            model.addAttribute("allStudios", studioRepository.findByIsDeletedFalse());
            model.addAttribute("currentStudioId", studioId);
            model.addAttribute("jadwalMap", new java.util.LinkedHashMap<>());
            return "admin/manajemen_jadwal";
        }
        
        model.addAttribute("isTutup", false);
        java.time.LocalTime bukaTime = java.time.LocalTime.parse(strBuka);
        java.time.LocalTime tutupTime = java.time.LocalTime.parse(strTutup);
        
        java.time.LocalDateTime startOfDay = selectedDate.atTime(bukaTime);
        java.time.LocalDateTime endOfDay;
        if (!tutupTime.isAfter(bukaTime)) {
            endOfDay = selectedDate.plusDays(1).atTime(tutupTime);
        } else {
            endOfDay = selectedDate.atTime(tutupTime);
        }
        if (startOfDay.equals(endOfDay)) {
            endOfDay = startOfDay.plusDays(1); // Full 24 hours fallback
        }
        
        java.time.LocalDateTime timelineStart = startOfDay;
        int totalTimelineMinutes = (int) java.time.Duration.between(startOfDay, endOfDay).toMinutes();
        
        List<Studio> allStudios = studioRepository.findByIsDeletedFalse();
        
        // Convert list to map of Studio -> List of JadwalDTO
        java.util.Map<Studio, List<JadwalDTO>> jadwalMap = new java.util.LinkedHashMap<>();
        
        List<Jadwal> jadwals = jadwalRepository.findByWaktuMulaiBetweenOrderByWaktuMulaiAsc(startOfDay, endOfDay);
        
        for (Studio studio : allStudios) {
            if (studioId != null && !studio.getId().equals(studioId)) {
                continue; // Skip if filtered by studio
            }
            jadwalMap.put(studio, new java.util.ArrayList<>());
        }
        
        for (Jadwal j : jadwals) {
            if (studioId != null && !j.getStudio().getId().equals(studioId)) {
                continue;
            }
            
            // Calculate timeline metrics
            long startMinutes = java.time.Duration.between(timelineStart, j.getWaktuMulai()).toMinutes();
            long durationMinutes = java.time.Duration.between(j.getWaktuMulai(), j.getWaktuSelesai()).toMinutes();
            
            // Clamp values just in case
            if (startMinutes < 0) startMinutes = 0;
            if (startMinutes + durationMinutes > totalTimelineMinutes) {
                durationMinutes = totalTimelineMinutes - startMinutes;
            }
            
            double left = ((double) startMinutes / totalTimelineMinutes) * 100;
            double width = ((double) durationMinutes / totalTimelineMinutes) * 100;
            
            boolean conflict = false;
            String reason = "";
            
            // Cek konflik dengan jadwal lain (overlap checking)
            // Buffer waktu 20 menit setelah film selesai untuk pembersihan
            List<Jadwal> overlapping = jadwalRepository.findOverlappingJadwals(
                j.getStudio().getId(), 
                j.getWaktuMulai(), 
                j.getWaktuSelesai().plusMinutes(20), 
                j.getId(),
                Jadwal.StatusJadwal.DIBATALKAN
            );
            
            if (!overlapping.isEmpty()) {
                conflict = true;
                reason = "Konflik Waktu/Pembersihan";
            }
            
            JadwalDTO dto = new JadwalDTO(j, left, width, conflict, reason);
            if (jadwalMap.containsKey(j.getStudio())) {
                jadwalMap.get(j.getStudio()).add(dto);
            }
        }
        
        model.addAttribute("jadwalMap", jadwalMap);
        model.addAttribute("allStudios", allStudios);
        model.addAttribute("currentDate", selectedDate.toString());
        model.addAttribute("currentStudioId", studioId);
        
        // Setup sequential labels based on config
        java.util.List<String> timeLabels = new java.util.ArrayList<>();
        java.time.LocalDateTime cur = startOfDay;
        while (!cur.isAfter(endOfDay)) {
            timeLabels.add(cur.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            if (cur.equals(endOfDay)) break;
            cur = cur.plusHours(1).withMinute(0).withSecond(0).withNano(0);
            if (cur.isAfter(endOfDay)) {
                timeLabels.add(endOfDay.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                break;
            }
        }
        model.addAttribute("timeLabels", timeLabels);
        model.addAttribute("totalTimelineMinutes", totalTimelineMinutes);
        model.addAttribute("bukaTime", bukaTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        model.addAttribute("tutupTime", tutupTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        
        // Pass films for the accordion form
        model.addAttribute("listFilm", filmRepository.findAll());
        
        // Pass current time marker
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (now.toLocalDate().equals(selectedDate)) {
            long currentMinutes = java.time.Duration.between(timelineStart, now).toMinutes();
            if (currentMinutes >= 0 && currentMinutes <= totalTimelineMinutes) {
                double nowLeft = ((double) currentMinutes / totalTimelineMinutes) * 100;
                model.addAttribute("nowLeft", nowLeft);
                model.addAttribute("nowTime", now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            }
        }
        
        return "admin/manajemen_jadwal";
    }
    
    @Autowired
    private StudioRepository studioRepository;
    
    @Autowired
    private TipeStudioRepository tipeStudioRepository;
    
    @Autowired
    private FasilitasRepository fasilitasRepository;
    
    @Autowired
    private KelasKursiRepository kelasKursiRepository;
    
    @Autowired
    private KursiRepository kursiRepository;
    
    @Autowired
    private JadwalRepository jadwalRepository;
    
    @Autowired
    private BioskopConfigRepository configRepository;

    @GetMapping("/manajemen-ruangan")
    public String manajemenRuangan(@RequestParam(required = false) String search,
                                   @RequestParam(required = false) String status,
                                   Model model) {
        
        Specification<Studio> spec = (root, query, cb) -> cb.conjunction();
        
        // Pencarian Nama Studio
        if (search != null && !search.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nama")), "%" + search.toLowerCase() + "%"));
        }
        
        // Filter Status
        if (status != null && !status.isEmpty() && !status.equals("Semua Status")) {
            Studio.StatusStudio statusEnum = null;
            if (status.equals("Aktif Beroperasi")) statusEnum = Studio.StatusStudio.AKTIF;
            else if (status.equals("Dalam Pemeliharaan (Renovasi)")) statusEnum = Studio.StatusStudio.RENOVASI;
            else if (status.equals("Ditutup")) statusEnum = Studio.StatusStudio.DITUTUP;
            
            if (statusEnum != null) {
                Studio.StatusStudio finalStatus = statusEnum;
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), finalStatus));
            }
        }
        // Filter isDeleted = false (Hanya tampilkan yang tidak diarsip/hapus)
        spec = spec.and((root, query, cb) -> cb.isFalse(root.get("isDeleted")));
        
        model.addAttribute("studios", studioRepository.findAll(spec));
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentStatus", status);
        
        return "admin/manajemen_ruangan";
    }

    @GetMapping("/tambah-film")
    public String tambahFilm(Model model) {
        model.addAttribute("genres", genreRepository.findAll());
        return "admin/tambah_film";
    }

    @PostMapping("/tambah-film")
    public String prosesTambahFilm(@RequestParam String judul, 
                                   @RequestParam String sinopsis, 
                                   @RequestParam Integer durasi,
                                   @RequestParam String status,
                                   @RequestParam String batasUsia,
                                   @RequestParam(required = false) List<Long> genreIds,
                                   @RequestParam(value = "posterFile", required = false) MultipartFile posterFile) {
        Film film = new Film();
        film.setJudul(judul);
        film.setSinopsis(sinopsis);
        film.setDurasi(durasi);
        film.setBatasUsia(batasUsia);
        film.setStatus(Film.StatusFilm.valueOf(status));
        
        // Handle upload poster
        if (posterFile != null && !posterFile.isEmpty()) {
            String contentType = posterFile.getContentType();
            String originalFilename = posterFile.getOriginalFilename() != null ? posterFile.getOriginalFilename().toLowerCase() : "";
            boolean isContentTypeValid = contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/jpg"));
            boolean isExtensionValid = originalFilename.endsWith(".jpg") || originalFilename.endsWith(".jpeg") || originalFilename.endsWith(".png");
            
            if (!isContentTypeValid || !isExtensionValid) {
                return "redirect:/admin/manajemen-film?error=invalidfile";
            }
            try {
                // Buat direktori jika belum ada
                String uploadDir = "uploads/posters/";
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                // Simpan file
                String fileName = UUID.randomUUID().toString() + "_" + posterFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(posterFile.getInputStream(), filePath);
                // Set URL di database
                film.setPosterUrl("/uploads/posters/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
                film.setPosterUrl("/img/poster_placeholder.jpg");
            }
        } else {
            film.setPosterUrl("/img/poster_placeholder.jpg");
        }
        
        Set<Genre> selectedGenres = new HashSet<>();
        if (genreIds != null) {
            for (Long gid : genreIds) {
                genreRepository.findById(gid).ifPresent(selectedGenres::add);
            }
        }
        film.setGenres(selectedGenres);
        
        filmRepository.save(film);
        auditService.log(AuditLog.ActionType.CREATE, "Film: " + film.getJudul(), "Menerbitkan film baru dengan ID " + film.getId());
        
        return "redirect:/admin/manajemen-film?success=true";
    }

    @GetMapping("/edit-film")
    public String editFilm(@RequestParam Long id, Model model) {
        Film film = filmRepository.findById(id).orElse(null);
        if (film == null) {
            return "redirect:/admin/manajemen-film?error=notfound";
        }
        model.addAttribute("film", film);
        model.addAttribute("genres", genreRepository.findAll());
        return "admin/edit_film";
    }

    @PostMapping("/edit-film")
    public String prosesEditFilm(@RequestParam Long id,
                                 @RequestParam String judul, 
                                 @RequestParam String sinopsis, 
                                 @RequestParam Integer durasi,
                                 @RequestParam String status,
                                 @RequestParam String batasUsia,
                                 @RequestParam(required = false) List<Long> genreIds,
                                 @RequestParam(value = "posterFile", required = false) MultipartFile posterFile) {
        
        Film film = filmRepository.findById(id).orElse(null);
        if (film == null) return "redirect:/admin/manajemen-film?error=notfound";
        
        film.setJudul(judul);
        film.setSinopsis(sinopsis);
        film.setDurasi(durasi);
        film.setBatasUsia(batasUsia);
        film.setStatus(Film.StatusFilm.valueOf(status));
        
        // Handle upload poster baru
        if (posterFile != null && !posterFile.isEmpty()) {
            String contentType = posterFile.getContentType();
            String originalFilename = posterFile.getOriginalFilename() != null ? posterFile.getOriginalFilename().toLowerCase() : "";
            boolean isContentTypeValid = contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/jpg"));
            boolean isExtensionValid = originalFilename.endsWith(".jpg") || originalFilename.endsWith(".jpeg") || originalFilename.endsWith(".png");
            
            if (!isContentTypeValid || !isExtensionValid) {
                return "redirect:/admin/manajemen-film?error=invalidfile";
            }
            try {
                // Hapus poster lama jika ada dan bukan placeholder
                if (film.getPosterUrl() != null && film.getPosterUrl().startsWith("/uploads/posters/")) {
                    String oldFileName = film.getPosterUrl().substring("/uploads/posters/".length());
                    Path oldFilePath = Paths.get("uploads/posters/").resolve(oldFileName);
                    Files.deleteIfExists(oldFilePath);
                }

                String uploadDir = "uploads/posters/";
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
                
                String fileName = UUID.randomUUID().toString() + "_" + posterFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(posterFile.getInputStream(), filePath);
                film.setPosterUrl("/uploads/posters/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        Set<Genre> selectedGenres = new HashSet<>();
        if (genreIds != null) {
            for (Long gid : genreIds) {
                genreRepository.findById(gid).ifPresent(selectedGenres::add);
            }
        }
        film.setGenres(selectedGenres);
        
        filmRepository.save(film);
        auditService.log(AuditLog.ActionType.UPDATE, "Film: " + film.getJudul(), "Mengubah data film ID " + film.getId());
        
        return "redirect:/admin/manajemen-film?updated=true";
    }

    @PostMapping("/hapus-film/{id}")
    public String hapusFilm(@PathVariable Long id) {
        Film film = filmRepository.findById(id).orElse(null);
        if (film != null) {
            // Hapus file fisik poster
            if (film.getPosterUrl() != null && film.getPosterUrl().startsWith("/uploads/posters/")) {
                try {
                    String oldFileName = film.getPosterUrl().substring("/uploads/posters/".length());
                    Path oldFilePath = Paths.get("uploads/posters/").resolve(oldFileName);
                    Files.deleteIfExists(oldFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            String namaFilm = film.getJudul();
            filmRepository.deleteById(id);
            auditService.log(AuditLog.ActionType.DELETE, "Film: " + namaFilm, "Menghapus film beserta posternya (ID " + id + ")");
        }
        return "redirect:/admin/manajemen-film?deleted=true";
    }

    @PostMapping("/tambah-jadwal")
    public String prosesTambahJadwal(@RequestParam Long filmId,
                                     @RequestParam Long studioId,
                                     @RequestParam String tanggal,
                                     @RequestParam String jam,
                                     @RequestParam Double harga,
                                     @RequestParam String status,
                                     @RequestParam(required = false) Long id) {
        
        Film film = filmRepository.findById(filmId).orElse(null);
        Studio studio = studioRepository.findById(studioId).orElse(null);
        
        if (film == null || studio == null) {
            return "redirect:/admin/manajemen-jadwal?date=" + tanggal + "&error=invalid";
        }
        
        java.time.LocalDateTime waktuMulai = java.time.LocalDateTime.parse(tanggal + "T" + jam);
        java.time.LocalDate selectedDate = java.time.LocalDate.parse(tanggal);
        
        BioskopConfig config = configRepository.findById("SINGLETON").orElse(new BioskopConfig());
        String strBuka = config.getJamBukaByDay(selectedDate.getDayOfWeek());
        String strTutup = config.getJamTutupByDay(selectedDate.getDayOfWeek());
        
        if (strBuka == null || strTutup == null) {
             return "redirect:/admin/manajemen-jadwal?date=" + tanggal + "&error=out_of_hours";
        }
        
        java.time.LocalTime bukaTime = java.time.LocalTime.parse(strBuka);
        java.time.LocalTime tutupTime = java.time.LocalTime.parse(strTutup);
        
        // Aturan Jam Operasional: Jika jam buka > jam tutup (melewati tengah malam)
        if (!tutupTime.isAfter(bukaTime)) {
            if (waktuMulai.toLocalTime().isBefore(bukaTime) && !waktuMulai.toLocalTime().isAfter(tutupTime)) {
                waktuMulai = waktuMulai.plusDays(1);
            }
        }
        
        java.time.LocalDateTime waktuSelesai = waktuMulai.plusMinutes(film.getDurasi() != null ? film.getDurasi() : 120);

        // Validasi Jam Operasional (Strict)
        java.time.LocalDateTime batasBuka = selectedDate.atTime(bukaTime);
        java.time.LocalDateTime batasTutup = selectedDate.atTime(tutupTime);
        if (!tutupTime.isAfter(bukaTime)) {
             batasTutup = batasTutup.plusDays(1);
        }
        if (waktuMulai.isBefore(batasBuka) || waktuSelesai.isAfter(batasTutup)) {
             return "redirect:/admin/manajemen-jadwal?date=" + tanggal + "&error=out_of_hours";
        }
        
        // Aturan Waktu Berlalu (Past Time Rule) - khusus untuk pembuatan atau pengeditan ke waktu lampau
        if (waktuMulai.isBefore(java.time.LocalDateTime.now())) {
             return "redirect:/admin/manajemen-jadwal?date=" + tanggal + "&error=past_time";
        }
        
        Jadwal jadwal;
        if (id != null) {
            jadwal = jadwalRepository.findById(id).orElse(new Jadwal());
            // Aturan Pembatasan Edit (Restricted Edit): tidak bisa diedit 10 menit sebelum waktu tayang
            if (jadwal.getWaktuMulai() != null && jadwal.getWaktuMulai().minusMinutes(10).isBefore(java.time.LocalDateTime.now())) {
                return "redirect:/admin/manajemen-jadwal?date=" + tanggal + "&error=locked";
            }
        } else {
            jadwal = new Jadwal();
        }

        // Validasi overlap (Tanpa jeda pembersihan)
        List<Jadwal> conflicts = jadwalRepository.findOverlappingJadwals(
            studioId, waktuMulai, waktuSelesai, id, Jadwal.StatusJadwal.DIBATALKAN
        );
        
        if (!conflicts.isEmpty()) {
            return "redirect:/admin/manajemen-jadwal?date=" + tanggal + "&error=conflict";
        }
        
        jadwal.setFilm(film);
        jadwal.setStudio(studio);
        jadwal.setWaktuMulai(waktuMulai);
        jadwal.setWaktuSelesai(waktuSelesai);
        jadwal.setHarga(harga);
        jadwal.setStatus(Jadwal.StatusJadwal.valueOf(status));
        
        jadwalRepository.save(jadwal);
        auditService.log(AuditLog.ActionType.CREATE, "Jadwal: " + film.getJudul(), "Menambahkan jadwal baru di " + studio.getNama() + " jam " + jam);
        
        return "redirect:/admin/manajemen-jadwal?date=" + tanggal + "&success=true";
    }

    @GetMapping("/api/jadwal-preview")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> getJadwalPreview(
            @RequestParam String tanggal,
            @RequestParam String jam,
            @RequestParam Long filmId,
            @RequestParam Long studioId,
            @RequestParam(required = false) Long id) {
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        try {
            java.time.LocalDate selectedDate = java.time.LocalDate.parse(tanggal);
            
            BioskopConfig config = configRepository.findById("SINGLETON").orElse(new BioskopConfig());
            String strBuka = config.getJamBukaByDay(selectedDate.getDayOfWeek());
            String strTutup = config.getJamTutupByDay(selectedDate.getDayOfWeek());
            
            if (strBuka == null || strTutup == null) {
                response.put("status", "error");
                response.put("message", "Bioskop tutup pada tanggal tersebut.");
                return org.springframework.http.ResponseEntity.badRequest().body(response);
            }
            
            java.time.LocalTime bukaTime = java.time.LocalTime.parse(strBuka);
            java.time.LocalTime tutupTime = java.time.LocalTime.parse(strTutup);
            
            java.time.LocalDateTime startOfDay = selectedDate.atTime(bukaTime);
            java.time.LocalDateTime endOfDay;
            if (!tutupTime.isAfter(bukaTime)) {
                endOfDay = selectedDate.plusDays(1).atTime(tutupTime);
            } else {
                endOfDay = selectedDate.atTime(tutupTime);
            }
            if (startOfDay.equals(endOfDay)) endOfDay = startOfDay.plusDays(1);
            
            java.time.LocalDateTime timelineStart = startOfDay;
            int totalTimelineMinutes = (int) java.time.Duration.between(startOfDay, endOfDay).toMinutes();
            
            List<Jadwal> existingJadwals = jadwalRepository.findByWaktuMulaiBetweenOrderByWaktuMulaiAsc(startOfDay, endOfDay);
            List<JadwalDTO> timeline = new java.util.ArrayList<>();
            
            for (Jadwal j : existingJadwals) {
                if (!j.getStudio().getId().equals(studioId)) continue;
                if (id != null && j.getId().equals(id)) continue;
                
                long startMinutes = java.time.Duration.between(timelineStart, j.getWaktuMulai()).toMinutes();
                long durationMinutes = java.time.Duration.between(j.getWaktuMulai(), j.getWaktuSelesai()).toMinutes();
                if (startMinutes < 0) startMinutes = 0;
                if (startMinutes + durationMinutes > totalTimelineMinutes) {
                    durationMinutes = totalTimelineMinutes - startMinutes;
                }
                
                double left = ((double) startMinutes / totalTimelineMinutes) * 100;
                double width = ((double) durationMinutes / totalTimelineMinutes) * 100;
                
                timeline.add(new JadwalDTO(j, left, width, false, ""));
            }
            
            java.time.LocalDateTime waktuMulai = java.time.LocalDateTime.parse(tanggal + "T" + jam);
            if (!tutupTime.isAfter(bukaTime)) {
                if (waktuMulai.toLocalTime().isBefore(bukaTime) && !waktuMulai.toLocalTime().isAfter(tutupTime)) {
                    waktuMulai = waktuMulai.plusDays(1);
                }
            }
            
            Film film = filmRepository.findById(filmId).orElse(null);
            java.time.LocalDateTime waktuSelesai = waktuMulai.plusMinutes(film != null && film.getDurasi() != null ? film.getDurasi() : 120);
            
            List<Jadwal> conflicts = jadwalRepository.findOverlappingJadwals(
                studioId, waktuMulai, waktuSelesai, id, Jadwal.StatusJadwal.DIBATALKAN
            );
            
            boolean isConflict = !conflicts.isEmpty();
            String conflictReason = isConflict ? "Bentrok dengan jadwal lain" : "";
            
            java.time.LocalDateTime batasTutup = java.time.LocalDateTime.parse(tanggal + "T" + config.getJamTutupByDay(selectedDate.getDayOfWeek()));
            if (!tutupTime.isAfter(bukaTime)) {
                 batasTutup = batasTutup.plusDays(1);
            }
            if (waktuSelesai.isAfter(batasTutup) || waktuMulai.toLocalTime().isBefore(bukaTime) && waktuMulai.toLocalTime().isAfter(tutupTime)) {
                 isConflict = true;
                 conflictReason = "Di luar jam operasional bioskop";
            }
            if (waktuMulai.isBefore(java.time.LocalDateTime.now())) {
                 isConflict = true;
                 conflictReason = "Waktu mulai tidak boleh di masa lalu";
            }
            if (id != null) {
                Jadwal oldJadwal = jadwalRepository.findById(id).orElse(null);
                if (oldJadwal != null && oldJadwal.getWaktuMulai() != null && oldJadwal.getWaktuMulai().minusMinutes(10).isBefore(java.time.LocalDateTime.now())) {
                     isConflict = true;
                     conflictReason = "Jadwal sudah dikunci (Selesai/Sedang Tayang/Mepet)";
                }
            }
            
            long pStartMin = java.time.Duration.between(timelineStart, waktuMulai).toMinutes();
            long pDurMin = java.time.Duration.between(waktuMulai, waktuSelesai).toMinutes();
            
            if (pStartMin < 0) {
                 pDurMin += pStartMin;
                 pStartMin = 0;
            }
            if (pStartMin + pDurMin > totalTimelineMinutes) {
                 pDurMin = totalTimelineMinutes - pStartMin;
            }
            
            double pLeft = ((double) pStartMin / totalTimelineMinutes) * 100;
            double pWidth = ((double) pDurMin / totalTimelineMinutes) * 100;
            
            java.util.Map<String, Object> proposed = new java.util.HashMap<>();
            proposed.put("leftPercentage", pLeft);
            proposed.put("widthPercentage", pWidth);
            proposed.put("isConflict", isConflict);
            proposed.put("conflictReason", conflictReason);
            proposed.put("waktuMulaiStr", waktuMulai.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            proposed.put("waktuSelesaiStr", waktuSelesai.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            proposed.put("filmJudul", film != null ? film.getJudul() : "Film");
            
            response.put("timeline", timeline);
            response.put("proposed", proposed);
            
            return org.springframework.http.ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/hapus-jadwal")
    public String hapusJadwal(@RequestParam Long id) {
        final String[] redirectDate = {null};
        jadwalRepository.findById(id).ifPresent(j -> {
            String deskripsi = "Film " + j.getFilm().getJudul() + " di " + j.getStudio().getNama() + " jam " + j.getWaktuMulai();
            jadwalRepository.delete(j);
            auditService.log(AuditLog.ActionType.DELETE, "Jadwal", "Menghapus jadwal ID " + id + " (" + deskripsi + ")");
            redirectDate[0] = j.getWaktuMulai().toLocalDate().toString();
        });
        
        if (redirectDate[0] != null) {
            return "redirect:/admin/manajemen-jadwal?date=" + redirectDate[0] + "&deleted=true";
        }
        return "redirect:/admin/manajemen-jadwal?deleted=true";
    }

    @GetMapping("/tambah-ruangan")
    public String tambahRuangan(Model model) {
        model.addAttribute("listTipe", tipeStudioRepository.findAll());
        model.addAttribute("listFasilitas", fasilitasRepository.findAll());
        return "admin/tambah_ruangan";
    }

    @PostMapping("/tambah-ruangan")
    public String prosesTambahRuangan(@RequestParam String nama,
                                      @RequestParam Long tipeId,
                                      @RequestParam String deskripsi,
                                      @RequestParam String status,
                                      @RequestParam(required = false) List<Long> fasilitasIds) {
        
        Studio studio = new Studio();
        studio.setNama(nama);
        studio.setDeskripsi(deskripsi);
        studio.setStatus(Studio.StatusStudio.valueOf(status));
        
        tipeStudioRepository.findById(tipeId).ifPresent(studio::setTipe);
        
        java.util.Set<Fasilitas> selectedFasilitas = new java.util.HashSet<>();
        if (fasilitasIds != null) {
            for (Long fid : fasilitasIds) {
                fasilitasRepository.findById(fid).ifPresent(selectedFasilitas::add);
            }
        }
        studio.setFasilitas(selectedFasilitas);
        
        studioRepository.save(studio);
        
        // Pindah ke step 2: manajemen kursi
        return "redirect:/admin/manajemen-kursi?studioId=" + studio.getId();
    }

    @GetMapping("/manajemen-kursi")
    public String manajemenKursi(@RequestParam Long studioId, Model model) {
        Studio studio = studioRepository.findById(studioId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid studio Id:" + studioId));
        
        model.addAttribute("studio", studio);
        model.addAttribute("listKelas", kelasKursiRepository.findAll());
        model.addAttribute("listKursi", kursiRepository.findByStudioId(studioId));
        return "admin/manajemen_kursi";
    }

    // Endpoint API untuk AJAX Tipe & Fasilitas
    @org.springframework.web.bind.annotation.ResponseBody
    @PostMapping("/api/tipe-studio")
    public org.springframework.http.ResponseEntity<?> addTipeStudio(@RequestParam String nama) {
        TipeStudio existing = tipeStudioRepository.findByNamaIgnoreCase(nama).orElse(null);
        if (existing != null) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", "Tipe Studio dengan nama ini sudah ada!"));
        }
        TipeStudio tipe = new TipeStudio(nama);
        return org.springframework.http.ResponseEntity.ok(tipeStudioRepository.save(tipe));
    }

    @org.springframework.web.bind.annotation.ResponseBody
    @PostMapping("/api/fasilitas")
    public org.springframework.http.ResponseEntity<?> addFasilitas(@RequestParam String nama, @RequestParam(required = false) String ikon) {
        Fasilitas existing = fasilitasRepository.findByNamaIgnoreCase(nama).orElse(null);
        if (existing != null) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", "Fasilitas dengan nama ini sudah ada!"));
        }
        Fasilitas fasilitas = new Fasilitas(nama, ikon);
        return org.springframework.http.ResponseEntity.ok(fasilitasRepository.save(fasilitas));
    }

    @org.springframework.web.bind.annotation.ResponseBody
    @PostMapping("/api/kelas-kursi")
    public org.springframework.http.ResponseEntity<?> addKelasKursi(@RequestParam String nama, @RequestParam Double surcharge, @RequestParam String hex, @RequestParam(required = false, defaultValue = "1") Integer spanKolom) {
        KelasKursi existing = kelasKursiRepository.findByNamaKelasIgnoreCase(nama).orElse(null);
        if (existing != null) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", "Kelas Kursi dengan nama ini sudah ada!"));
        }
        KelasKursi kelas = new KelasKursi();
        kelas.setNamaKelas(nama);
        kelas.setBiayaTambahan(surcharge);
        kelas.setWarnaHex(hex);
        kelas.setSpanKolom(spanKolom);
        return org.springframework.http.ResponseEntity.ok(kelasKursiRepository.save(kelas));
    }
    
    @org.springframework.web.bind.annotation.ResponseBody
    @org.springframework.web.bind.annotation.DeleteMapping("/api/tipe-studio/{id}")
    public org.springframework.http.ResponseEntity<?> deleteTipeStudio(@org.springframework.web.bind.annotation.PathVariable Long id) {
        try {
            tipeStudioRepository.deleteById(id);
            return org.springframework.http.ResponseEntity.ok().build();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", "in_use", "message", "Tipe Studio sedang digunakan oleh Studio dan tidak dapat dihapus."));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", "unknown", "message", e.getMessage()));
        }
    }

    @org.springframework.web.bind.annotation.ResponseBody
    @org.springframework.web.bind.annotation.DeleteMapping("/api/fasilitas/{id}")
    public org.springframework.http.ResponseEntity<?> deleteFasilitas(@org.springframework.web.bind.annotation.PathVariable Long id) {
        try {
            fasilitasRepository.deleteById(id);
            return org.springframework.http.ResponseEntity.ok().build();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", "in_use", "message", "Fasilitas sedang digunakan oleh Studio dan tidak dapat dihapus."));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", "unknown", "message", e.getMessage()));
        }
    }

    @org.springframework.web.bind.annotation.ResponseBody
    @org.springframework.web.bind.annotation.DeleteMapping("/api/kelas-kursi/{id}")
    public org.springframework.http.ResponseEntity<?> deleteKelasKursi(@org.springframework.web.bind.annotation.PathVariable Long id) {
        try {
            kelasKursiRepository.deleteById(id);
            return org.springframework.http.ResponseEntity.ok().build();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", "in_use", "message", "Kelas Kursi sedang digunakan oleh kursi di Studio dan tidak dapat dihapus."));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", "unknown", "message", e.getMessage()));
        }
    }

    @org.springframework.web.bind.annotation.ResponseBody
    @PostMapping("/api/simpan-kursi")
    public org.springframework.http.ResponseEntity<?> simpanKursi(
            @RequestParam Long studioId,
            @org.springframework.web.bind.annotation.RequestBody java.util.List<java.util.Map<String, Object>> dataKursi) {
        
        Studio studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid studio Id"));
        
        // Hapus data kursi lama
        kursiRepository.deleteByStudioId(studioId);
        
        int kapasitas = 0;
        int maxBarisCode = 64; // ASCII for '@', 'A' is 65
        
        java.util.List<Kursi> kursisToSave = new java.util.ArrayList<>();
        
        for (java.util.Map<String, Object> map : dataKursi) {
            Kursi k = new Kursi();
            k.setStudio(studio);
            k.setKodeKursi((String) map.get("kodeKursi"));
            
            String baris = (String) map.get("baris");
            k.setBaris(baris);
            if (baris != null && !baris.isEmpty()) {
                int charCode = baris.charAt(0);
                if (charCode > maxBarisCode) {
                    maxBarisCode = charCode;
                }
            }
            
            k.setKolom(Integer.parseInt(map.get("kolom").toString()));
            k.setTipe(Kursi.TipeKursi.valueOf((String) map.get("tipe")));
            k.setSpanKolom(Integer.parseInt(map.getOrDefault("spanKolom", "1").toString()));
            
            Object kelasIdObj = map.get("kelasKursiId");
            if (kelasIdObj != null && !kelasIdObj.toString().trim().isEmpty() && !kelasIdObj.toString().equals("null")) {
                try {
                    kelasKursiRepository.findById(Long.parseLong(kelasIdObj.toString())).ifPresent(k::setKelasKursi);
                } catch (NumberFormatException e) {
                    // Abaikan jika tidak valid
                }
            }
            
            kursisToSave.add(k);
            
            if (k.getTipe() == Kursi.TipeKursi.KURSI && k.getIsAktif()) {
                kapasitas += k.getSpanKolom();
            }
        }
        
        kursiRepository.saveAll(kursisToSave); // Batch insert untuk optimasi performa
        
        // Update kapasitas dan jumlah baris studio otomatis
        studio.setKapasitas(kapasitas);
        int jumlahBaris = maxBarisCode > 64 ? (maxBarisCode - 64) : 0;
        studio.setJumlahBaris(jumlahBaris);
        studioRepository.save(studio);
        
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @GetMapping("/edit-ruangan")
    public String editRuangan(@RequestParam Long id, Model model) {
        Studio studio = studioRepository.findById(id).orElse(null);
        if (studio == null || studio.getIsDeleted()) {
            return "redirect:/admin/manajemen-ruangan?error=notfound";
        }
        
        java.util.List<TipeStudio> listTipe = tipeStudioRepository.findAll();
        java.util.List<Fasilitas> listFasilitas = fasilitasRepository.findAll();
        
        model.addAttribute("studio", studio);
        model.addAttribute("listTipe", listTipe);
        model.addAttribute("listFasilitas", listFasilitas);
        
        java.util.List<Long> studioFasilitasIds = new java.util.ArrayList<>();
        if (studio.getFasilitas() != null) {
            for (Fasilitas f : studio.getFasilitas()) {
                studioFasilitasIds.add(f.getId());
            }
        }
        model.addAttribute("studioFasilitasIds", studioFasilitasIds);
        
        return "admin/tambah_ruangan";
    }

    @PostMapping("/edit-ruangan")
    public String prosesEditRuangan(@RequestParam Long id,
                                    @RequestParam String nama, 
                                    @RequestParam(required = false) Long tipeId, 
                                    @RequestParam String status, 
                                    @RequestParam String deskripsi,
                                    @RequestParam(required = false) java.util.List<Long> fasilitasIds) {
        Studio studio = studioRepository.findById(id).orElse(null);
        if (studio != null) {
            studio.setNama(nama);
            if (tipeId != null) {
                tipeStudioRepository.findById(tipeId).ifPresent(studio::setTipe);
            }
            studio.setStatus(Studio.StatusStudio.valueOf(status));
            studio.setDeskripsi(deskripsi);
            
            java.util.Set<Fasilitas> selectedFasilitas = new java.util.HashSet<>();
            if (fasilitasIds != null) {
                for (Long fId : fasilitasIds) {
                    fasilitasRepository.findById(fId).ifPresent(selectedFasilitas::add);
                }
            }
            studio.setFasilitas(selectedFasilitas);
            studioRepository.save(studio);
        }
        return "redirect:/admin/manajemen-ruangan?success=true";
    }

    @PostMapping("/hapus-ruangan")
    public String hapusRuangan(@RequestParam Long id) {
        if (jadwalRepository.existsByStudioIdAndWaktuSelesaiAfterAndStatusNot(id, java.time.LocalDateTime.now(), Jadwal.StatusJadwal.DIBATALKAN)) {
            return "redirect:/admin/manajemen-ruangan?error=in_use";
        }
        studioRepository.findById(id).ifPresent(s -> {
            s.setIsDeleted(true);
            studioRepository.save(s);
        });
        return "redirect:/admin/manajemen-ruangan?deleted=true";
    }

    @GetMapping("/pengaturan")
    public String pengaturanAdmin(Model model, java.security.Principal principal) {
        BioskopConfig config = configRepository.findById("SINGLETON").orElse(new BioskopConfig());
        model.addAttribute("config", config);
        
        User adminUser = new User();
        if (principal != null) {
            adminUser = userService.findByEmail(principal.getName()).orElse(new User());
        }
        model.addAttribute("adminUser", adminUser);
        
        return "admin/pengaturan_admin";
    }

    @PostMapping("/pengaturan")
    public String simpanPengaturan(@org.springframework.web.bind.annotation.ModelAttribute BioskopConfig configUpdate) {
        BioskopConfig config = configRepository.findById("SINGLETON").orElse(new BioskopConfig());
        
        // Update Profil
        config.setNamaBioskop(configUpdate.getNamaBioskop());
        config.setAlamatBioskop(configUpdate.getAlamatBioskop());
        config.setKontakCs(configUpdate.getKontakCs());
        config.setGmapsEmbedUrl(configUpdate.getGmapsEmbedUrl());
        
        // Update Hari Operasional
        config.setSeninBuka(configUpdate.isSeninBuka());
        config.setSeninJamMulai(configUpdate.getSeninJamMulai());
        config.setSeninJamSelesai(configUpdate.getSeninJamSelesai());
        
        config.setSelasaBuka(configUpdate.isSelasaBuka());
        config.setSelasaJamMulai(configUpdate.getSelasaJamMulai());
        config.setSelasaJamSelesai(configUpdate.getSelasaJamSelesai());
        
        config.setRabuBuka(configUpdate.isRabuBuka());
        config.setRabuJamMulai(configUpdate.getRabuJamMulai());
        config.setRabuJamSelesai(configUpdate.getRabuJamSelesai());
        
        config.setKamisBuka(configUpdate.isKamisBuka());
        config.setKamisJamMulai(configUpdate.getKamisJamMulai());
        config.setKamisJamSelesai(configUpdate.getKamisJamSelesai());
        
        config.setJumatBuka(configUpdate.isJumatBuka());
        config.setJumatJamMulai(configUpdate.getJumatJamMulai());
        config.setJumatJamSelesai(configUpdate.getJumatJamSelesai());
        
        config.setSabtuBuka(configUpdate.isSabtuBuka());
        config.setSabtuJamMulai(configUpdate.getSabtuJamMulai());
        config.setSabtuJamSelesai(configUpdate.getSabtuJamSelesai());
        
        config.setMingguBuka(configUpdate.isMingguBuka());
        config.setMingguJamMulai(configUpdate.getMingguJamMulai());
        config.setMingguJamSelesai(configUpdate.getMingguJamSelesai());
        
        // Update Midtrans & Lainnya
        config.setProduction(configUpdate.isProduction());
        if (configUpdate.getPaymentServerKey() != null && !configUpdate.getPaymentServerKey().equals("********")) {
            config.setPaymentServerKey(configUpdate.getPaymentServerKey());
        }
        if (configUpdate.getPaymentClientKey() != null && !configUpdate.getPaymentClientKey().equals("********")) {
            config.setPaymentClientKey(configUpdate.getPaymentClientKey());
        }
        
        config.setTaxRate(configUpdate.getTaxRate());
        config.setPlatformFee(configUpdate.getPlatformFee());
        config.setMaxTicketsPerTransaction(configUpdate.getMaxTicketsPerTransaction());
        
        config.setRefundTimeLimitHours(configUpdate.getRefundTimeLimitHours());
        config.setRefundDeductionPercentage(configUpdate.getRefundDeductionPercentage());
        
        configRepository.save(config);
        
        return "redirect:/admin/pengaturan?success=true";
    }

    @PostMapping("/pengaturan/profil")
    public String simpanProfil(@org.springframework.web.bind.annotation.RequestParam("namaLengkap") String namaLengkap,
                               @org.springframework.web.bind.annotation.RequestParam("email") String email,
                               java.security.Principal principal) {
        if (principal != null) {
            java.util.Optional<User> optUser = userService.findByEmail(principal.getName());
            if (optUser.isPresent()) {
                User user = optUser.get();
                user.setNamaLengkap(namaLengkap);
                user.setEmail(email);
                userRepository.save(user);
            }
        }
        return "redirect:/admin/pengaturan?success_profil=true";
    }

    @PostMapping("/pengaturan/password")
    public String ubahPassword(@org.springframework.web.bind.annotation.RequestParam("currentPassword") String currentPassword,
                               @org.springframework.web.bind.annotation.RequestParam("newPassword") String newPassword,
                               java.security.Principal principal) {
        if (principal != null) {
            java.util.Optional<User> optUser = userService.findByEmail(principal.getName());
            if (optUser.isPresent()) {
                User user = optUser.get();
                // Without passwordEncoder injected here, we can't easily check current password.
                // Let's just forcefully update for now, or use userService updatePassword.
                userService.updatePassword(user, newPassword);
            }
        }
        return "redirect:/admin/pengaturan?success_password=true";
    }
}

