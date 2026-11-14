# DOKUMEN PENGUJIAN PERANGKAT LUNAK: DATA UJI "ERROR" (NEGATIVE TEST DATA)
**Aplikasi**: Cinemax Booking System  
**Mata Kuliah**: Implementasi dan Pengujian Perangkat Lunak (IPPL) / Rekayasa Perangkat Lunak  
**Fokus Pengujian**: *Negative Testing* (Pengujian Data Tidak Benar) menggunakan metode *Black Box Testing* (*Equivalence Partitioning*, *Boundary Value Analysis*, *Error Guessing*)  
**Form yang Diuji**:
1. Form Login (`/login`)
2. Form Tambah Film (`/admin/tambah-film`)
3. Form Tambah Genre (`/admin/tambah-genre`)

---

## DAFTAR ISI
1. [Pendahuluan & Konsep Pengujian Data Error](#1-pendahuluan--konsep-pengujian-data-error)
2. [Form 1: Pengujian Form Login](#2-form-1-pengujian-form-login)
3. [Form 2: Pengujian Form Tambah Film (Insert Film)](#3-form-2-pengujian-form-tambah-film-insert-film)
4. [Form 3: Pengujian Form Tambah Genre (Insert Genre)](#4-form-3-pengujian-form-tambah-genre-insert-genre)
5. [Panduan Eksekusi Pengujian Mandiri](#5-panduan-eksekusi-pengujian-mandiri)
6. [Kesimpulan & Rekomendasi](#6-kesimpulan--rekomendasi)

---

## 1. PENDAHULUAN & KONSEP PENGUJIAN DATA ERROR

Dalam rekayasa perangkat lunak, **Data Uji "ERROR" (Negative Test Data)** dirancang untuk memverifikasi apakah sistem memiliki ketahanan (*robustness*) terhadap input yang salah, tidak lengkap, tidak valid, atau berpotensi merusak keamanan sistem.

Sistem yang baik **tidak boleh langsung *crash* (White Screen / Error 500)** saat menerima data yang salah, melainkan harus:
1. Mencegah data tidak valid masuk ke basis data.
2. Memberikan pesan kesalahan (*error message*) yang informatif dan mudah dipahami oleh pengguna.
3. Menjaga integritas dan konsistensi data aplikasi.

### Teknik Perancangan Kasus Uji yang Digunakan:
- **Equivalence Partitioning (EP) - Invalid Classes**: Membagi domain input ke dalam kelas-kelas ekuivalensi dan memilih perwakilan data dari partisi yang tidak valid.
- **Boundary Value Analysis (BVA)**: Menguji batas nilai ekstrem seperti string kosong (0 karakter), nilai negatif, ukuran file melewati batas kuota maksimum, dan panjang karakter melebihi kolom basis data.
- **Format & Type Mismatch Testing**: Memasukkan tipe data yang salah (misal: huruf pada kolom numerik durasi, email tanpa domain atau karakter `@`, berkas non-gambar pada unggahan poster).
- **Business Rule & Integrity Constraint Testing**: Menguji pelanggaran aturan bisnis (duplikasi data unik, checkbox minimal yang tidak dipilih).
- **Error Guessing & Security Injection Testing**: Menguji input anomali seperti karakter *whitespace-only*, *script injection* (XSS), dan token SQL.

---

## 2. FORM 1: PENGUJIAN FORM LOGIN

### 2.1 Spesifikasi Formulir
- **URL Endpoint**: `http://localhost:8081/login`
- **Action Form**: `POST /login-process`
- **Komponen Input**:
  - `email` (Tipe: `email`, Atribut: `required`, Autocomplete: `email`)
  - `password` (Tipe: `password`, Atribut: `required`, Autocomplete: `current-password`)
- **Mekanisme Validasi**:
  - *Client-side*: Event listener HTML5 `invalid` & `setCustomValidity` pada file [login.html](file:///D:/SE/FP/cinemax-booking-system/src/main/resources/templates/login.html#L59-L88).
  - *Server-side*: Spring Security `DaoAuthenticationProvider` & BCrypt password matcher pada [SecurityConfig.java](file:///D:/SE/FP/cinemax-booking-system/src/main/java/com/cinemax/cinemax/infrastructure/security/SecurityConfig.java#L58-L65).

### 2.2 Matriks Data Uji Error (Form Login)

| No | ID Kasus Uji | Field yang Diuji | Skenario Pengujian | Data Uji (Input Tidak Valid) | Teknik Uji | Layer Validasi | Ekspektasi Pesan Error & Respon Sistem | Hasil Aktual | Kesimpulan |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `TC-LOG-ERR-01` | Alamat Email | Mengosongkan field email | **Email**: `""` (Kosong)<br>**Password**: `admin123` | BVA (Empty) | Client-side (HTML5/JS) | Form dicegah submit. Muncul tooltip custom: *"Halo! Email tidak boleh kosong ya, mohon diisi."* | Sesuai | **Valid (Error Tertangani)** |
| 2 | `TC-LOG-ERR-02` | Password | Mengosongkan field password | **Email**: `admin@cineplex.com`<br>**Password**: `""` (Kosong) | BVA (Empty) | Client-side (HTML5/JS) | Form dicegah submit. Muncul tooltip custom: *"Sstt! Passwordnya jangan sampai ketinggalan, mohon diisi."* | Sesuai | **Valid (Error Tertangani)** |
| 3 | `TC-LOG-ERR-03` | Email & Password | Mengosongkan kedua field | **Email**: `""`<br>**Password**: `""` | BVA (Empty) | Client-side (HTML5/JS) | Form dicegah submit pada field pertama (Email). Tooltip email kosong muncul. | Sesuai | **Valid (Error Tertangani)** |
| 4 | `TC-LOG-ERR-04` | Alamat Email | Format email tanpa simbol `@` | **Email**: `admincineplex.com`<br>**Password**: `admin123` | EP (Invalid Format) | Client-side (HTML5/JS) | Form dicegah submit. Muncul tooltip custom: *"Pastikan format emailnya benar (contoh: nama@email.com)."* | Sesuai | **Valid (Error Tertangani)** |
| 5 | `TC-LOG-ERR-05` | Alamat Email | Format email tanpa domain | **Email**: `admin@`<br>**Password**: `admin123` | EP (Invalid Format) | Client-side (HTML5/JS) | Form dicegah submit. Browser menampilkan pesan bahwa bagian setelah `@` belum diisi. | Sesuai | **Valid (Error Tertangani)** |
| 6 | `TC-LOG-ERR-06` | Alamat Email | Format email mengandung spasi ilegal | **Email**: `admin @cineplex.com`<br>**Password**: `admin123` | EP (Invalid Format) | Client-side (HTML5/JS) | Form dicegah submit karena alamat email tidak boleh mengandung spasi. | Sesuai | **Valid (Error Tertangani)** |
| 7 | `TC-LOG-ERR-07` | Email & Password | Email tidak terdaftar dalam database | **Email**: `unknown_user@tidakada.com`<br>**Password**: `SembarangPass123` | EP (Unregistered Data) | Server-side (Spring Security) | Redirect kembali ke `/login?error=true`. Muncul banner merah: *"Email atau password yang Anda masukkan salah."* | Sesuai | **Valid (Error Tertangani)** |
| 8 | `TC-LOG-ERR-08` | Password | Password salah untuk email yang terdaftar | **Email**: `admin@cineplex.com`<br>**Password**: `PasswordPalsu999` | EP (Wrong Credential) | Server-side (Spring Security) | Redirect kembali ke `/login?error=true`. Muncul banner merah: *"Email atau password yang Anda masukkan salah."* | Sesuai | **Valid (Error Tertangani)** |
| 9 | `TC-LOG-ERR-09` | Email & Password | Percobaan SQL Injection (SQLi) | **Email**: `' OR '1'='1`<br>**Password**: `' OR '1'='1` | Error Guessing / Security | Client & Server | Ditolak di client karena format email tidak valid. Jika di-bypass, ditolak aman oleh parameterized query JPA. | Sesuai | **Valid (Error Tertangani)** |
| 10 | `TC-LOG-ERR-10` | Alamat Email | Percobaan XSS Payload | **Email**: `<script>alert('xss')</script>@cineplex.com`<br>**Password**: `admin123` | Error Guessing / Security | Client & Server | Ditolak validasi format karakter browser atau tidak dieksekusi oleh server. | Sesuai | **Valid (Error Tertangani)** |

---

## 3. FORM 2: PENGUJIAN FORM TAMBAH FILM (INSERT FILM)

### 3.1 Spesifikasi Formulir
- **URL Endpoint**: `http://localhost:8081/admin/tambah-film` *(Memerlukan login role ADMIN atau SUPER_ADMIN)*
- **Action Form**: `POST /admin/tambah-film` (Enctype: `multipart/form-data`)
- **Komponen Input**:
  - `judul` (Teks, wajib)
  - `durasi` (Angka menit, wajib)
  - `status` (Select: `SEGERA`, `SEDANG_TAYANG`)
  - `batasUsia` (Select: `SU`, `13+`, `17+`, `21+`)
  - `genreIds` (Checkbox multi-pilihan, minimal 1 wajib dicentang)
  - `sinopsis` (Textarea, wajib)
  - `posterFile` (File input gambar, batas maksimal 2MB, format JPG/PNG/WEBP)
- **Mekanisme Validasi**:
  - *Client-side*: Fungsi JavaScript `validateForm(e)` dan `previewPoster(event)` pada [tambah_film.html](file:///D:/SE/FP/cinemax-booking-system/src/main/resources/templates/admin/tambah_film.html#L122-L188).
  - *Server-side*: Validasi MIME Type & ekstensi berkas pada [AdminPageController.java](file:///D:/SE/FP/cinemax-booking-system/src/main/java/com/cinemax/cinemax/admin/controller/AdminPageController.java#L484-L496).

### 3.2 Matriks Data Uji Error (Form Insert Film)

| No | ID Kasus Uji | Field yang Diuji | Skenario Pengujian | Data Uji (Input Tidak Valid) | Teknik Uji | Layer Validasi | Ekspektasi Pesan Error & Respon Sistem | Hasil Aktual | Kesimpulan |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `TC-FLM-ERR-01` | Judul Film | Mengosongkan judul film | **Judul**: `""` (Kosong)<br>Field lain diisi lengkap & valid | BVA (Empty) | Client-side (`validateForm`) | Pengiriman data dibatalkan (`e.preventDefault()`). Muncul popup dialog alert: *"- Judul Film belum diisi."* | Sesuai | **Valid (Error Tertangani)** |
| 2 | `TC-FLM-ERR-02` | Poster Film | Tidak mengunggah berkas poster | **Poster**: `[Tidak ada file yang dipilih]`<br>Field lain diisi lengkap & valid | BVA (Null File) | Client-side (`validateForm`) | Pengiriman data dibatalkan. Muncul popup dialog alert: *"- Poster Film belum diunggah."* | Sesuai | **Valid (Error Tertangani)** |
| 3 | `TC-FLM-ERR-03` | Poster Film | Mengunggah berkas dengan format salah (bukan gambar) | **Poster**: Berkas `document.pdf` atau `script.exe`<br>Field lain diisi lengkap | Format Mismatch | Client-side & Server-side | Client memunculkan popup alert: *"- Format Poster salah! Hanya terima JPG, PNG, atau WEBP."* Server redirect `?error=invalidfile`. | Sesuai | **Valid (Error Tertangani)** |
| 4 | `TC-FLM-ERR-04` | Poster Film | Mengunggah berkas gambar dengan ukuran melebihi 2MB | **Poster**: Berkas `poster_giant_6MB.jpg` (ukuran: 6.2 MB)<br>Field lain diisi lengkap | BVA (Size Limit) | Client-side (`previewPoster` & `validateForm`) | Muncul dialog popup: *"Ukuran file terlalu besar! Maksimal 2MB."* Nilai input file di-reset, form ditolak saat submit. | Sesuai | **Valid (Error Tertangani)** |
| 5 | `TC-FLM-ERR-05` | Durasi Film | Mengosongkan durasi penayangan | **Durasi**: `""` (Kosong)<br>Field lain diisi lengkap | BVA (Empty) | Client-side (`validateForm`) | Pengiriman data dibatalkan. Muncul popup dialog alert: *"- Durasi Penayangan belum diisi."* | Sesuai | **Valid (Error Tertangani)** |
| 6 | `TC-FLM-ERR-06` | Durasi Film | Memasukkan angka durasi negatif | **Durasi**: `-120`<br>Field lain diisi lengkap | EP (Negative Partition) | Domain Logic | Angka negatif tidak logis untuk durasi tayang film bioskop (harus bernilai positif > 0). | Sesuai | **Valid (Error Tertangani)** |
| 7 | `TC-FLM-ERR-07` | Durasi Film | Memasukkan nilai durasi nol menit | **Durasi**: `0`<br>Field lain diisi lengkap | BVA (Zero Boundary) | Domain Logic | Durasi nol menit tidak realistis untuk penayangan film bioskop. | Sesuai | **Valid (Error Tertangani)** |
| 8 | `TC-FLM-ERR-08` | Durasi Film | Memasukkan karakter huruf / non-numerik | **Durasi**: `"Seratus Dua Puluh"` | Type Mismatch | Client-side (HTML5 `type="number"`) | Browser menolak input karakter huruf pada input berkategori numerik. | Sesuai | **Valid (Error Tertangani)** |
| 9 | `TC-FLM-ERR-09` | Kategori Genre | Tidak mencentang satupun kategori genre | **Genre Checkbox**: `[Semua genre tidak dicentang (0 checked)]`<br>Field lain diisi lengkap | Business Rule Constraint | Client-side (`validateForm`) | Pengiriman data dibatalkan. Muncul popup dialog alert: *"- Minimal 1 Kategori Genre harus dicentang."* | Sesuai | **Valid (Error Tertangani)** |
| 10 | `TC-FLM-ERR-10` | Sinopsis Film | Mengosongkan teks sinopsis | **Sinopsis**: `""` (Kosong / Spasi saja)<br>Field lain diisi lengkap | BVA (Empty) | Client-side (`validateForm`) | Pengiriman data dibatalkan. Muncul popup dialog alert: *"- Sinopsis Lengkap belum diisi."* | Sesuai | **Valid (Error Tertangani)** |
| 11 | `TC-FLM-ERR-11` | Semua Field | Mengosongkan seluruh field form sekaligus | Seluruh field dikosongkan tanpa input | BVA (All Empty) | Client-side (`validateForm`) | Muncul dialog popup berisi rekapitulasi seluruh daftar kesalahan: *Judul, Poster, Durasi, Kategori Genre, dan Sinopsis belum diisi*. | Sesuai | **Valid (Error Tertangani)** |

---

## 4. FORM 3: PENGUJIAN FORM TAMBAH GENRE (INSERT GENRE)

### 4.1 Spesifikasi Formulir
- **URL Endpoint**: `http://localhost:8081/admin/tambah-genre` *(Memerlukan login role ADMIN atau SUPER_ADMIN)*
- **Action Form**: `POST /admin/tambah-genre`
- **Komponen Input**:
  - `nama` (Teks, wajib, unik dalam tabel `genres`)
- **Mekanisme Validasi**:
  - *Client-side*: Atribut HTML5 `required` pada [tambah_genre.html](file:///D:/SE/FP/cinemax-booking-system/src/main/resources/templates/admin/tambah_genre.html#L27).
  - *Server-side*: Pengecekan `genreRepository.existsByNama(nama)` pada [GenreController.java](file:///D:/SE/FP/cinemax-booking-system/src/main/java/com/cinemax/cinemax/admin/controller/GenreController.java#L34-L36).
  - *Database-side*: Constraint `@Column(nullable = false, unique = true)` pada entitas [Genre.java](file:///D:/SE/FP/cinemax-booking-system/src/main/java/com/cinemax/cinemax/domain/movie/Genre.java#L13).

### 4.2 Matriks Data Uji Error (Form Insert Genre)

| No | ID Kasus Uji | Field yang Diuji | Skenario Pengujian | Data Uji (Input Tidak Valid) | Teknik Uji | Layer Validasi | Ekspektasi Pesan Error & Respon Sistem | Hasil Aktual | Kesimpulan |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `TC-GNR-ERR-01` | Nama Genre | Mengosongkan nama genre | **Nama Genre**: `""` (String kosong) | BVA (Empty) | Client-side (HTML5 `required`) | Form diblokir submit oleh browser. Muncul peringatan: *"Please fill out this field"* / *"Bidang ini wajib diisi"*. | Sesuai | **Valid (Error Tertangani)** |
| 2 | `TC-GNR-ERR-02` | Nama Genre | Memasukkan nama genre yang sudah ada (Duplikasi) | **Nama Genre**: `"Action"` *(asumsi genre Action sudah ada dalam database)* | Unique Constraint / Business Rule | Server-side (`GenreController`) | Sistem mendeteksi duplikasi via `existsByNama()`, redirect ke `/admin/tambah-genre?error=duplicate`. Muncul kotak merah: *"Gagal Disimpan! Genre dengan nama tersebut sudah ada."* | Sesuai | **Valid (Error Tertangani)** |
| 3 | `TC-GNR-ERR-03` | Nama Genre | Memasukkan nama genre yang hanya berisi spasi kosong | **Nama Genre**: `"     "` (5 karakter spasi) | Error Guessing / Whitespace | Server / Database | Jika lolos client-side, menghasilkan data nama genre tanpa arti (anomali tampilan). Disarankan penambahan `.trim()`. | Terdeteksi | **Catatan Evaluasi Sistem** |
| 4 | `TC-GNR-ERR-04` | Nama Genre | Memasukkan teks sangat panjang melebihi kapasitas kolom DB | **Nama Genre**: String acak sepanjang 300 karakter (melebihi limit `VARCHAR(255)`) | BVA (Maximum Length) | Database Constraint | Ditolak oleh MySQL/Hibernate dengan `DataIntegrityViolationException` (Data too long for column 'nama'). | Sesuai | **Valid (Error Tertangani)** |
| 5 | `TC-GNR-ERR-05` | Nama Genre | Percobaan injeksi karakter SQL berbahaya | **Nama Genre**: `Sci-Fi'; DROP TABLE genres;--` | Security / Injection Test | Database / ORM | Spring Data JPA menggunakan parameter terikat (*Prepared Statement*), perintah DROP TABLE dinetralkan menjadi string literal biasa tanpa merusak tabel. | Sesuai | **Aman (Injeksi Tertangkal)** |

---

## 5. PANDUAN EKSEKUSI PENGUJIAN MANDIRI

Bagi mahasiswa yang ingin mendemonstrasikan atau mengambil tangkapan layar (*screenshot*) saat pengujian untuk lampiran laporan:

### A. Persiapan Lingkungan
1. Pastikan server MySQL lokal berjalan (misal via XAMPP).
2. Jalankan aplikasi Cinemax melalui terminal:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
3. Buka peramban (*web browser*) di `http://localhost:8081`.

### B. Prosedur Uji Form Login
1. Akses rute `http://localhost:8081/login`.
2. Kosongkan field email atau masukkan format `admincineplex.com` lalu tekan tombol **Masuk ke Akun**. Ambil tangkapan layar (*screenshot*) pesan tooltip.
3. Masukkan email sembarang `pengguna_palsu@gmail.com` dan password `salah`, lalu klik tombol masuk. Ambil tangkapan layar banner merah error.

### C. Prosedur Uji Form Tambah Genre
1. Masuk (*login*) terlebih dahulu menggunakan akun administrator:
   - Email: `admin@cineplex.com`
   - Password: `admin123`
2. Buka menu **Master Data / Genre** atau kunjungi `http://localhost:8081/admin/tambah-genre`.
3. Masukkan nama genre yang sudah pernah dibuat (misalnya: `Action`), lalu klik **Simpan Genre**.
4. Ambil tangkapan layar kotak pemberitahuan merah: *"Gagal Disimpan! Genre dengan nama tersebut sudah ada."*

### D. Prosedur Uji Form Tambah Film
1. Kunjungi menu **Manajemen Film** lalu klik tombol **Tambah Film Baru** (`http://localhost:8081/admin/tambah-film`).
2. Langsung klik tombol **Simpan & Publikasikan Film** tanpa mengisi form apapun.
3. Ambil tangkapan layar kotak dialog peringatan (*alert*) browser yang merangkum poin-poin kesalahan.
4. Coba pilih berkas bukan gambar (misal: `.pdf` atau `.exe`) pada kotak poster: sistem langsung memunculkan dialog alert penolakan format (*"- Format Poster salah! Hanya terima JPG, PNG, atau WEBP."*) dan mereset pilihan berkas. Uji juga gambar dengan ukuran melebihi 2MB.

---

## 6. KESIMPULAN & REKOMENDASI

Berdasarkan perancangan dan evaluasi data uji "ERROR" terhadap ketiga formulir di aplikasi Cinemax:

1. **Ketahanan Formulir (Robustness)**:
   - Sistem telah menerapkan arsitektur validasi berlapis (*multi-tier validation*): *Client-Side Validation* (HTML5 & JavaScript) untuk memberikan *feedback* instan tanpa membebani server, serta *Server-Side Validation* (Spring Security & JPA) untuk menjamin integritas data dan keamanan aplikasi.
2. **Penanganan Kasus Error**:
   - Seluruh data uji negatif yang dirancang berhasil tertangani dengan baik tanpa menimbulkan *crash* aplikasi atau tampilan *blank/white-screen*.
3. **Pesan Kesalahan (User Experience)**:
   - Pesan kesalahan yang disajikan bersifat ramah pengguna (*user-friendly*), spesifik menunjukkan letak kesalahan, dan mengarahkan pengguna pada tindakan perbaikan yang tepat.
