package com.cinemax.cinemax.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String beranda() {
        return "beranda"; // Merender templates/beranda.html
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // Merender templates/login.html
    }

    @GetMapping("/register")
    public String register() {
        return "register"; // Merender templates/register.html
    }

    @GetMapping("/jelajah")
    public String jelajah() {
        return "jelajah"; // Merender templates/jelajah.html
    }

    @GetMapping("/profil")
    public String profil() {
        return "profil"; // Merender templates/profil.html
    }
    
    @GetMapping("/pilih-kursi")
    public String pilihKursi() {
        return "pilih_kursi"; // Merender templates/pilih_kursi.html
    }

    @GetMapping("/lupa-password")
    public String lupaPassword() {
        return "lupa_password";
    }

    @GetMapping("/pilih-jadwal")
    public String pilihJadwal() {
        return "pilih_jadwal";
    }

    @GetMapping("/daftar-tiket")
    public String daftarTiket() {
        return "daftar_tiket";
    }

    @GetMapping("/detail-tiket")
    public String detailTiket() {
        return "detail_tiket";
    }

    @GetMapping("/detail-transaksi")
    public String detailTransaksi() {
        return "detail_transaksi";
    }

    @GetMapping("/faktur")
    public String fakturPesanan() {
        return "faktur_pesanan";
    }
}
