package com.cinemax.cinemax.admin.controller;

import com.cinemax.cinemax.domain.schedule.Studio;
import com.cinemax.cinemax.domain.schedule.StudioRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugStudioController {
    @Autowired
    private StudioRepository studioRepository;

    @GetMapping("/api/debug-studio")
    public String debugStudio() {
        try {
            Optional<Studio> s = studioRepository.findById(1L);
            if (s.isEmpty()) {
                return "Studio 1 IS EMPTY in Repository. Let's find all: " + studioRepository.findAll().size();
            } else {
                return "Studio 1 FOUND: " + s.get().getNama();
            }
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return sw.toString();
        }
    }
}
