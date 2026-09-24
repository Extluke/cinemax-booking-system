package com.cinemax.cinemax.domain.movie;

import jakarta.persistence.*;

@Entity
@Table(name = "films")
public class Film {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String judul;
    
    @Column(columnDefinition = "TEXT")
    private String sinopsis;
    
    private Integer durasi; // dalam menit
    private String batasUsia;

    
    @ManyToMany
    @JoinTable(
        name = "film_genres",
        joinColumns = @JoinColumn(name = "film_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private java.util.Set<Genre> genres = new java.util.HashSet<>();
    
    private String posterUrl;
    
    @Enumerated(EnumType.STRING)
    private StatusFilm status;
    
    public enum StatusFilm {
        SEDANG_TAYANG,
        SEGERA,
        TIDAK_TAYANG
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getJudul() { return judul; }
    public void setJudul(String judul) { this.judul = judul; }
    public String getSinopsis() { return sinopsis; }
    public void setSinopsis(String sinopsis) { this.sinopsis = sinopsis; }
    public Integer getDurasi() { return durasi; }
    public void setDurasi(Integer durasi) { this.durasi = durasi; }
    public String getBatasUsia() { return batasUsia; }
    public void setBatasUsia(String batasUsia) { this.batasUsia = batasUsia; }
    public java.util.Set<Genre> getGenres() { return genres; }
    public void setGenres(java.util.Set<Genre> genres) { this.genres = genres; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public StatusFilm getStatus() { return status; }
    public void setStatus(StatusFilm status) { this.status = status; }
}
