package com.cinemax.cinemax.api.dto;

import java.util.List;

public class CheckoutRequestDTO {
    private Long jadwalId;
    private Long userId;
    private List<String> kursiKode;

    // Getters and Setters
    public Long getJadwalId() {
        return jadwalId;
    }

    public void setJadwalId(Long jadwalId) {
        this.jadwalId = jadwalId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getKursiKode() {
        return kursiKode;
    }

    public void setKursiKode(List<String> kursiKode) {
        this.kursiKode = kursiKode;
    }
}
