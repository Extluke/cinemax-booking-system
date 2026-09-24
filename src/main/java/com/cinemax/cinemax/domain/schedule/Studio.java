package com.cinemax.cinemax.domain.schedule;

import com.cinemax.cinemax.domain.movie.TipeStudio;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "studios")
@SQLRestriction("is_deleted = false")
public class Studio {
    
    public enum StatusStudio {
        AKTIF,
        RENOVASI,
        DITUTUP
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nama;
    @ManyToOne
    @JoinColumn(name = "tipe_id")
    private TipeStudio tipe;
    
    private Integer kapasitas;
    private Integer jumlahBaris;
    private String deskripsi;
    
    @ManyToMany
    @JoinTable(
        name = "studio_fasilitas",
        joinColumns = @JoinColumn(name = "studio_id"),
        inverseJoinColumns = @JoinColumn(name = "fasilitas_id")
    )
    private java.util.Set<Fasilitas> fasilitas = new java.util.HashSet<>();
    
    @OneToMany(mappedBy = "studio", cascade = CascadeType.ALL)
    private java.util.Set<Kursi> listKursi = new java.util.HashSet<>();
    
    @Enumerated(EnumType.STRING)
    private StatusStudio status;
    
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean isDeleted = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    
    public TipeStudio getTipe() { return tipe; }
    public void setTipe(TipeStudio tipe) { this.tipe = tipe; }
    
    public Integer getKapasitas() { return kapasitas; }
    public void setKapasitas(Integer kapasitas) { this.kapasitas = kapasitas; }
    
    public Integer getJumlahBaris() { return jumlahBaris; }
    public void setJumlahBaris(Integer jumlahBaris) { this.jumlahBaris = jumlahBaris; }
    
    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }
    
    public java.util.Set<Fasilitas> getFasilitas() { return fasilitas; }
    public void setFasilitas(java.util.Set<Fasilitas> fasilitas) { this.fasilitas = fasilitas; }
    
    public java.util.Set<Kursi> getListKursi() { return listKursi; }
    public void setListKursi(java.util.Set<Kursi> listKursi) { this.listKursi = listKursi; }
    
    public StatusStudio getStatus() { return status; }
    public void setStatus(StatusStudio status) { this.status = status; }
    
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
}
