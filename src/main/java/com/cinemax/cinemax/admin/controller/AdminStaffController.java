package com.cinemax.cinemax.admin.controller;

import com.cinemax.cinemax.domain.user.User;
import com.cinemax.cinemax.domain.user.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/staf")
public class AdminStaffController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String viewStaff(Model model) {
        List<User> stafList = userRepository.findAll();
        // Filter out normal users if needed, or show all admin/superadmin
        stafList.removeIf(u -> "ROLE_USER".equals(u.getRole()));
        model.addAttribute("stafList", stafList);
        model.addAttribute("activePage", "staf");
        model.addAttribute("newUser", new User());
        return "admin/manajemen_staf";
    }

    @PostMapping("/tambah")
    public String addStaff(@ModelAttribute User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return "redirect:/admin/staf?error=exists";
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Ensure role is valid
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("ROLE_ADMIN");
        }
        userRepository.save(user);
        return "redirect:/admin/staf?success=true";
    }

    @PostMapping("/hapus/{id}")
    public String deleteStaff(@PathVariable Long id) {
        userRepository.deleteById(id);
        return "redirect:/admin/staf?deleted=true";
    }
}
