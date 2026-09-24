package com.cinemax.cinemax.admin.controller;

import com.cinemax.cinemax.domain.booking.TransaksiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/laporan")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminLaporanController {

    @Autowired
    private TransaksiRepository transaksiRepository;

    @GetMapping
    public String laporanPendapatan(Model model) {
        List<Object[]> revenueByFilm = transaksiRepository.calculateRevenueByFilm();
        List<Object[]> revenueByDate = transaksiRepository.calculateRevenueByDate();
        Double totalRevenue = transaksiRepository.calculateTotalRevenue();

        model.addAttribute("revenueByFilm", revenueByFilm);
        model.addAttribute("revenueByDate", revenueByDate);
        model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        model.addAttribute("activePage", "laporan");

        return "admin/laporan_pendapatan";
    }
}
