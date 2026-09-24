# Panduan Arsitektur & Struktur Direktori Cinemax (Modular Monolith)

Struktur folder ini merupakan acuan mutlak untuk pengembangan proyek Cinemax ke depannya, memisahkan secara jelas antara tanggung jawab halaman Admin, halaman User (API), core domain, dan konfigurasi teknis.

```text
com.cinemax.cinemax
├── admin/                  <-- (AREA KERJA ANDA - DEVELOPER ADMIN)
│   ├── controller/         (Controller khusus halaman HTML Admin & Super Admin)
│   ├── dto/                (Data Transfer Object untuk form admin)
│   └── service/            (Logika bisnis khusus untuk Admin)
│
├── api/                    <-- (AREA KERJA TEMAN ANDA - DEVELOPER USER/FRONTEND)
│   ├── controller/         (REST API Controller yang akan di-fetch oleh frontend User)
│   ├── dto/                (Format JSON untuk request/response User)
│   └── service/            (Logika bisnis khusus untuk User, misal: pesanan tiket)
│
├── domain/                 <-- (AREA BERSAMA / CORE DATA)
│   ├── movie/              (Film, Genre, TipeStudio, AudioType)
│   ├── schedule/           (Jadwal, Studio, Kursi)
│   ├── booking/            (Tiket, Transaksi, Promo, dsb)
│   ├── user/               (User, Role, Token)
│   └── config/             (BioskopConfig, RefundPolicy, AuditLog)
│
├── infrastructure/         <-- (KONFIGURASI TEKNIS & INTEGRASI PIHAK KE-3)
│   ├── security/           (Konfigurasi Spring Security, RBAC Super Admin vs Admin)
│   ├── payment/            (Konfigurasi & Client untuk Payment Gateway, misal: Midtrans)
│   ├── mail/               (Konfigurasi & Client Email)
│   └── config/             (Konfigurasi MVC, CORS, Database Seeder)
```

## Aturan Tambahan:
1. **Pemisahan Kerja**: Developer Admin HANYA bekerja pada package `admin/`, dan tidak boleh memodifikasi controller di `api/` kecuali diinstruksikan secara eksplisit.
2. **Abstraksi Pihak Ketiga**: Integrasi pihak ketiga seperti Payment Gateway (Midtrans) harus diletakkan di `infrastructure/payment/` dengan nama-nama yang abstrak (misal: `PaymentGateway`, bukan terpaku nama `Midtrans`), karena provider bisa berubah di masa depan.
3. **Data Core**: Folder `domain/` menampung semua Entitas (Entity JPA) dan Repository. Semua logika relasi database berada di sini.
