package com.cinemax.cinemax.domain.schedule;

import jakarta.persistence.*;

@Entity
@Table(name = "kursi", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"studio_id", "kode_kursi"})
})
public class Kursi {

    public enum TipeKursi {
        KURSI,
        JALAN_SETAPAK
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Studio studio;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "kelas_kursi_id")
    private KelasKursi kelasKursi; // Bisa null jika jalan setapak
    
    @Column(name = "kode_kursi", nullable = false, length = 10)
    private String kodeKursi; // Contoh: "A1"
    
    @Column(nullable = false, length = 5)
    private String baris; // Contoh: "A"
    
    @Column(nullable = false)
    private Integer kolom; // Contoh: 1
    
    @Enumerated(EnumType.STRING)
    private TipeKursi tipe = TipeKursi.KURSI;
    
    private Integer spanKolom = 1; // Untuk kursi panjang (Sweetbox)
    
    private Boolean isAktif = true;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Studio getStudio() { return studio; }
    public void setStudio(Studio studio) { this.studio = studio; }

    public KelasKursi getKelasKursi() { return kelasKursi; }
    public void setKelasKursi(KelasKursi kelasKursi) { this.kelasKursi = kelasKursi; }

    public String getKodeKursi() { return kodeKursi; }
    public void setKodeKursi(String kodeKursi) { this.kodeKursi = kodeKursi; }

    public String getBaris() { return baris; }
    public void setBaris(String baris) { this.baris = baris; }

    public Integer getKolom() { return kolom; }
    public void setKolom(Integer kolom) { this.kolom = kolom; }

    public TipeKursi getTipe() { return tipe; }
    public void setTipe(TipeKursi tipe) { this.tipe = tipe; }

    public Integer getSpanKolom() { return spanKolom; }
    public void setSpanKolom(Integer spanKolom) { this.spanKolom = spanKolom; }

    public Boolean getIsAktif() { return isAktif; }
    public void setIsAktif(Boolean isAktif) { this.isAktif = isAktif; }
}
