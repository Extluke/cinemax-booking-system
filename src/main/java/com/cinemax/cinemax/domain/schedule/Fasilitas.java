package com.cinemax.cinemax.domain.schedule;

import jakarta.persistence.*;

@Entity
@Table(name = "fasilitas")
public class Fasilitas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String nama;
    
    private String ikon; // Contoh: "fa-solid fa-snowflake"

    public Fasilitas() {}
    public Fasilitas(String nama, String ikon) {
        this.nama = nama;
        this.ikon = ikon;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getIkon() { return ikon; }
    public void setIkon(String ikon) { this.ikon = ikon; }
}
