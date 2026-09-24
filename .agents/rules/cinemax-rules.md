---
trigger: always_on
description: Konfigurasi penting dan riwayat perbaikan error untuk proyek Cinemax
---

# Aturan Proyek Cinemax

## 1. Konfigurasi Teknis (Wajib)
1. **Port Server**: Aplikasi ini **HARUS** berjalan di port 8081. Pastikan `server.port=8081` selalu ada di `application.properties` karena port 8080 sudah digunakan oleh servis lain (XAMPP/dll).
2. **Dialek Hibernate**: Karena menggunakan Spring Boot versi 3+ dan Hibernate versi terbaru, **JANGAN** menggunakan `MySQL8Dialect` atau `MySQL5Dialect`. Gunakan nama dialek universal yaitu `org.hibernate.dialect.MySQLDialect`.
3. **Struktur HTML**: Halaman untuk *User* berada langsung di `src/main/resources/templates/`, sedangkan halaman untuk *Admin* dikelompokkan di dalam sub-folder `src/main/resources/templates/admin/`.

## 2. Disiplin Scope — DILARANG Melebar
4. **Kerjakan HANYA yang diminta.** Jika saya minta "perbaiki fitur X di halaman dashboard", JANGAN menyentuh, merefactor, atau "merapikan" file/fitur lain yang tidak disebutkan — walaupun kamu melihat kode yang menurutmu jelek atau bisa diperbaiki.
5. **Jika menemukan bug lain** di luar scope yang saya minta saat sedang bekerja, JANGAN langsung diperbaiki. Cukup **laporkan** di akhir ("Saya juga menemukan bug Y di file Z, mau saya perbaiki juga?") dan tunggu konfirmasi saya.
6. **Jangan mengubah nama variabel, method, struktur folder, atau style/formatting** kode yang tidak berhubungan langsung dengan task yang diminta.
7. Kalau instruksi saya menyebut "halaman dashboard", itu artinya **satu controller/halaman itu saja** — bukan seluruh modul admin, kecuali saya sebut eksplisit.

## 3. Definition of Done — WAJIB Sebelum Bilang "Selesai"
8. **Dilarang menyatakan task selesai tanpa verifikasi nyata.** Sebelum melaporkan "sudah diperbaiki", agent WAJIB:
   - Membaca ulang seluruh file yang diubah untuk mengecek tidak ada syntax error atau referensi yang rusak (contoh: element Thymeleaf `th:*` yang menunjuk ke variabel/field yang sudah tidak ada, endpoint yang dipanggil JS tapi controller-nya tidak ada).
   - Menjalankan build (`mvn compile` atau setara) dan memastikan **tidak ada error**, bukan cuma "kelihatannya benar".
   - Mengecek **semua elemen** yang disebutkan di permintaan awal, bukan cuma sebagian. Kalau saya bilang "perbaiki fitur di dashboard" dan dashboard punya 3 tombol (export CSV, filter, refresh), agent harus mengecek KETIGANYA — bukan berhenti setelah 1 tombol berhasil.
   - Jika ada endpoint/API yang dipanggil dari halaman tersebut (misal via fetch/AJAX untuk export CSV), agent wajib mengecek controller endpoint-nya benar-benar ada dan return response yang sesuai — jangan asumsikan endpoint sudah benar hanya karena kode frontend-nya terlihat benar.
9. **Buat daftar checklist eksplisit** di awal sebelum mulai kerja untuk task yang menyangkut lebih dari 1 komponen (contoh: "Yang akan saya cek: [ ] tombol export CSV [ ] filter tanggal [ ] tabel data [ ] tidak ada error saat load halaman"), lalu centang satu per satu sebelum melaporkan selesai.
10. **Halaman tidak boleh error/blank/white screen** setelah perubahan. Ini adalah kegagalan fatal — kalau agent tidak 100% yakin halaman akan render dengan benar, agent harus mengatakan itu secara eksplisit, bukan melaporkan "selesai" begitu saja.

## 4. Mencegah Regresi (Bug Lama Muncul Lagi / Bug Baru Muncul)
11. **Sebelum mengubah kode untuk fix bug baru, agent WAJIB membaca kembali fix sebelumnya** yang berkaitan dengan file/fitur yang sama di percakapan ini, supaya tidak menimpa atau membatalkan perbaikan yang sudah dilakukan.
12. **Setelah setiap perbaikan bug, agent WAJIB mengecek ulang bug-bug sebelumnya yang berhubungan dengan file yang sama** — bukan cuma mengecek bug yang baru saja diperbaiki. Contoh: kalau baru selesai fix Bug 2 di file `DashboardController.java`, agent harus cek juga apakah Bug 1 yang tadinya sudah fix di file yang sama masih tetap fix.
13. **Dilarang melakukan perbaikan dengan cara "coba-coba" (trial and error) tanpa memahami root cause.** Jika akar masalah belum jelas, agent harus menelusuri dulu (baca stack trace, baca kode terkait) sebelum mengubah kode — jangan asal ganti-ganti sampai error hilang, karena ini yang sering menyebabkan bug baru muncul di tempat lain.
14. **Kalau satu perbaikan menyentuh method/class yang dipakai di banyak tempat** (shared/reusable code), agent WAJIB menyebutkan semua tempat lain yang memanggil method tersebut dan menjelaskan bahwa itu sudah dicek tidak akan rusak.
15. Di akhir sesi perbaikan multi-bug, agent WAJIB memberi **ringkasan status semua bug** yang dibahas di sesi itu (bukan cuma bug terakhir), formatnya: `Bug 1: [status] | Bug 2: [status] | Bug 3: [status]`.

## 5. Komunikasi
16. Jika ada bagian dari permintaan yang tidak bisa diselesaikan 100% (misalnya butuh info tambahan, atau ada keterbatasan), **katakan dengan jelas dan spesifik** bagian mana yang belum selesai — jangan melaporkan semua selesai padahal masih ada yang tertinggal.