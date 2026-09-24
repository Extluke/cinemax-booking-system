package com.cinemax.cinemax.domain.schedule;

import jakarta.persistence.*;

@Entity
@Table(name = "kelas_kursi")
public class KelasKursi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String namaKelas;
    
    @Column(columnDefinition = "TEXT")
    private String deskripsi;
    
    // Ini adalah Surcharge (biaya tambahan yang dikalikan/ditambahkan ke base price)
    private Double biayaTambahan = 0.0;
    
    @Column(length = 7)
    private String warnaHex; // Contoh: "#E74C3C"

    private Integer spanKolom = 1;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNamaKelas() { return namaKelas; }
    public void setNamaKelas(String namaKelas) { this.namaKelas = namaKelas; }

    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    public Double getBiayaTambahan() { return biayaTambahan; }
    public void setBiayaTambahan(Double biayaTambahan) { this.biayaTambahan = biayaTambahan; }

    public String getWarnaHex() { return warnaHex; }
    public void setWarnaHex(String warnaHex) { this.warnaHex = warnaHex; }

    public Integer getSpanKolom() { return spanKolom; }
    public void setSpanKolom(Integer spanKolom) { this.spanKolom = spanKolom; }
}
