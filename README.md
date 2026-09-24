# Cinemax - Sistem Manajemen Bioskop & Ticketing

Cinemax adalah aplikasi berbasis web untuk manajemen operasional bioskop dan pemesanan tiket (*online ticketing*). Dibangun menggunakan **Java Spring Boot 3**, **Hibernate/JPA**, dan **Thymeleaf**, dengan integrasi **Midtrans Payment Gateway**.

## 🚀 Fitur Utama
- **Public & User System**: Jelajah film, registrasi, login, lupa password (via email SMTP).
- **Online Ticketing**: Pemesanan tiket secara real-time dengan integrasi Midtrans.
- **Admin Dashboard**: Manajemen Film, Jadwal Tayang, Studio, Staf, Promo, dan Refund.
- **Super Admin**: Konfigurasi global sistem, Audit Log, dan Laporan Pendapatan.
- **Security**: Dilindungi dengan Spring Security (BCrypt, Role-based Access Control, Method Security).

## 🛠️ Tech Stack
- **Backend:** Java 17, Spring Boot 3.x, Spring Data JPA, Spring Security, Spring Mail.
- **Database:** MySQL.
- **Frontend:** HTML5, CSS3, Vanilla JavaScript, Thymeleaf.
- **Payment Gateway:** Midtrans (Snap API).

## ⚙️ Cara Menjalankan Project (Local Development)

### 1. Persiapan Database
1. Buka XAMPP / MySQL server Anda.
2. Buat database baru bernama `cinemax_db` (bisa menggunakan phpMyAdmin atau command line).
```sql
CREATE DATABASE cinemax_db;
```

### 2. Konfigurasi `application.properties`
Secara bawaan, aplikasi akan membaca pengaturan di `src/main/resources/application.properties`. Pastikan kredensial database sudah sesuai dengan lokal Anda:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cinemax_db?serverTimezone=Asia/Jakarta&useSSL=false
spring.datasource.username=root
spring.datasource.password=
```
*(Catatan: Anda juga bisa mengkonfigurasi kredensial SMTP Mailtrap untuk fitur kirim email).*

### 3. Jalankan Aplikasi
Gunakan terminal pada root folder proyek, lalu jalankan perintah:
```bash
# Windows
.\mvnw spring-boot:run

# Linux / Mac
./mvnw spring-boot:run
```

Aplikasi akan berjalan di `http://localhost:8081`.

### 4. Akun Default (Otomatis Dibuat)
Sistem ini menggunakan *Data Seeder* yang otomatis membuat 2 akun *administrator* saat aplikasi pertama kali dijalankan:
- **Super Admin:**
  - Email: `superadmin@cineplex.com`
  - Password: `superadmin123`
- **Admin Biasa (Kasir):**
  - Email: `admin@cineplex.com`
  - Password: `admin123`

## 📁 Struktur Direktori Utama
- `src/main/java/.../cinemax` : Berisi seluruh logika backend (Controller, Domain/Entity, Service, Infrastructure).
- `src/main/resources/templates` : Berisi file tampilan Thymeleaf HTML (terbagi menjadi *public*, *user*, dan *admin*).
- `src/main/resources/static` : Berisi file CSS, JavaScript, gambar statis, dan direktori penyimpanan upload poster.

## ⚠️ Perhatian Keamanan (Security Notes)
Sistem ini mengimplementasikan pengamanan terhadap IDOR, *Double Booking* (Race Condition), *Web Shell Upload*, dan pencegahan akses tidak sah melalui `@EnableMethodSecurity`. Pastikan Anda selalu menjaga **Midtrans Server Key** dan **SMTP Password** Anda sebagai rahasia dan hindari meng-commit key produksi ke dalam *repository* publik.

---
*Dibuat untuk keperluan Tugas Besar IPPL Semester 5.*
