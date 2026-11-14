package com.cinemax.cinemax.domain.user;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String namaLengkap;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false)
    private String role; // e.g., "ROLE_USER" or "ROLE_ADMIN"

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'LOCAL'")
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(nullable = true)
    private String googleId;

    @Column(nullable = true)
    private String profilePicture;

    public enum AuthProvider {
        LOCAL,
        GOOGLE
    }

    public User() {
    }

    public User(Long id, String namaLengkap, String email, String password, String role) {
        this.id = id;
        this.namaLengkap = namaLengkap;
        this.email = email;
        this.password = password;
        this.role = role;
        this.enabled = true;
        this.authProvider = AuthProvider.LOCAL;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNamaLengkap() {
        return namaLengkap;
    }

    public void setNamaLengkap(String namaLengkap) {
        this.namaLengkap = namaLengkap;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}

