package com.cinemax.cinemax.domain.movie;

import jakarta.persistence.*;

@Entity
@Table(name = "tipe_studios")
public class TipeStudio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String nama;

    public TipeStudio() {}

    public TipeStudio(String nama) {
        this.nama = nama;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
}
