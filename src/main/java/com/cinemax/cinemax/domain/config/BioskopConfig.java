package com.cinemax.cinemax.domain.config;

import com.cinemax.cinemax.domain.booking.Refund;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class BioskopConfig {
    
    @Id
    private String id = "SINGLETON";
    
    // Operasional Per Hari
    private boolean seninBuka = true;
    private String seninJamMulai = "10:00";
    private String seninJamSelesai = "22:00";
    
    private boolean selasaBuka = true;
    private String selasaJamMulai = "10:00";
    private String selasaJamSelesai = "22:00";
    
    private boolean rabuBuka = true;
    private String rabuJamMulai = "10:00";
    private String rabuJamSelesai = "22:00";
    
    private boolean kamisBuka = true;
    private String kamisJamMulai = "10:00";
    private String kamisJamSelesai = "22:00";
    
    private boolean jumatBuka = true;
    private String jumatJamMulai = "10:00";
    private String jumatJamSelesai = "23:00";
    
    private boolean sabtuBuka = true;
    private String sabtuJamMulai = "09:00";
    private String sabtuJamSelesai = "23:59";
    
    private boolean mingguBuka = true;
    private String mingguJamMulai = "09:00";
    private String mingguJamSelesai = "23:59";

    // Profil Bioskop
    private String namaBioskop = "Cinemax Multiplex";
    private String alamatBioskop = "Jl. Sudirman No. 123, Jakarta";
    private String kontakCs = "08123456789";
    private String gmapsEmbedUrl = "";

    // Midtrans
    private boolean isProduction = false;
    private String paymentServerKey = "";
    private String paymentClientKey = "";

    // Biaya & Pajak
    private double taxRate = 11.0;
    private double platformFee = 4000.0;
    private int maxTicketsPerTransaction = 10;

    // Refund
    private int refundTimeLimitHours = 2;
    private double refundDeductionPercentage = 20.0;

    public BioskopConfig() {
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public boolean isSeninBuka() { return seninBuka; }
    public void setSeninBuka(boolean seninBuka) { this.seninBuka = seninBuka; }
    public String getSeninJamMulai() { return seninJamMulai; }
    public void setSeninJamMulai(String seninJamMulai) { this.seninJamMulai = seninJamMulai; }
    public String getSeninJamSelesai() { return seninJamSelesai; }
    public void setSeninJamSelesai(String seninJamSelesai) { this.seninJamSelesai = seninJamSelesai; }

    public boolean isSelasaBuka() { return selasaBuka; }
    public void setSelasaBuka(boolean selasaBuka) { this.selasaBuka = selasaBuka; }
    public String getSelasaJamMulai() { return selasaJamMulai; }
    public void setSelasaJamMulai(String selasaJamMulai) { this.selasaJamMulai = selasaJamMulai; }
    public String getSelasaJamSelesai() { return selasaJamSelesai; }
    public void setSelasaJamSelesai(String selasaJamSelesai) { this.selasaJamSelesai = selasaJamSelesai; }

    public boolean isRabuBuka() { return rabuBuka; }
    public void setRabuBuka(boolean rabuBuka) { this.rabuBuka = rabuBuka; }
    public String getRabuJamMulai() { return rabuJamMulai; }
    public void setRabuJamMulai(String rabuJamMulai) { this.rabuJamMulai = rabuJamMulai; }
    public String getRabuJamSelesai() { return rabuJamSelesai; }
    public void setRabuJamSelesai(String rabuJamSelesai) { this.rabuJamSelesai = rabuJamSelesai; }

    public boolean isKamisBuka() { return kamisBuka; }
    public void setKamisBuka(boolean kamisBuka) { this.kamisBuka = kamisBuka; }
    public String getKamisJamMulai() { return kamisJamMulai; }
    public void setKamisJamMulai(String kamisJamMulai) { this.kamisJamMulai = kamisJamMulai; }
    public String getKamisJamSelesai() { return kamisJamSelesai; }
    public void setKamisJamSelesai(String kamisJamSelesai) { this.kamisJamSelesai = kamisJamSelesai; }

    public boolean isJumatBuka() { return jumatBuka; }
    public void setJumatBuka(boolean jumatBuka) { this.jumatBuka = jumatBuka; }
    public String getJumatJamMulai() { return jumatJamMulai; }
    public void setJumatJamMulai(String jumatJamMulai) { this.jumatJamMulai = jumatJamMulai; }
    public String getJumatJamSelesai() { return jumatJamSelesai; }
    public void setJumatJamSelesai(String jumatJamSelesai) { this.jumatJamSelesai = jumatJamSelesai; }

    public boolean isSabtuBuka() { return sabtuBuka; }
    public void setSabtuBuka(boolean sabtuBuka) { this.sabtuBuka = sabtuBuka; }
    public String getSabtuJamMulai() { return sabtuJamMulai; }
    public void setSabtuJamMulai(String sabtuJamMulai) { this.sabtuJamMulai = sabtuJamMulai; }
    public String getSabtuJamSelesai() { return sabtuJamSelesai; }
    public void setSabtuJamSelesai(String sabtuJamSelesai) { this.sabtuJamSelesai = sabtuJamSelesai; }

    public boolean isMingguBuka() { return mingguBuka; }
    public void setMingguBuka(boolean mingguBuka) { this.mingguBuka = mingguBuka; }
    public String getMingguJamMulai() { return mingguJamMulai; }
    public void setMingguJamMulai(String mingguJamMulai) { this.mingguJamMulai = mingguJamMulai; }
    public String getMingguJamSelesai() { return mingguJamSelesai; }
    public void setMingguJamSelesai(String mingguJamSelesai) { this.mingguJamSelesai = mingguJamSelesai; }

    public String getNamaBioskop() { return namaBioskop; }
    public void setNamaBioskop(String namaBioskop) { this.namaBioskop = namaBioskop; }

    public String getAlamatBioskop() { return alamatBioskop; }
    public void setAlamatBioskop(String alamatBioskop) { this.alamatBioskop = alamatBioskop; }

    public String getKontakCs() { return kontakCs; }
    public void setKontakCs(String kontakCs) { this.kontakCs = kontakCs; }
    
    public String getGmapsEmbedUrl() { return gmapsEmbedUrl; }
    public void setGmapsEmbedUrl(String gmapsEmbedUrl) { this.gmapsEmbedUrl = gmapsEmbedUrl; }

    public boolean isProduction() { return isProduction; }
    public void setProduction(boolean production) { isProduction = production; }

    public String getPaymentServerKey() { return paymentServerKey; }
    public void setPaymentServerKey(String paymentServerKey) { this.paymentServerKey = paymentServerKey; }

    public String getPaymentClientKey() { return paymentClientKey; }
    public void setPaymentClientKey(String paymentClientKey) { this.paymentClientKey = paymentClientKey; }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }

    public double getPlatformFee() { return platformFee; }
    public void setPlatformFee(double platformFee) { this.platformFee = platformFee; }

    public int getMaxTicketsPerTransaction() { return maxTicketsPerTransaction; }
    public void setMaxTicketsPerTransaction(int maxTicketsPerTransaction) { this.maxTicketsPerTransaction = maxTicketsPerTransaction; }

    public int getRefundTimeLimitHours() { return refundTimeLimitHours; }
    public void setRefundTimeLimitHours(int refundTimeLimitHours) { this.refundTimeLimitHours = refundTimeLimitHours; }

    public double getRefundDeductionPercentage() { return refundDeductionPercentage; }
    public void setRefundDeductionPercentage(double refundDeductionPercentage) { this.refundDeductionPercentage = refundDeductionPercentage; }

    public String getJamBukaByDay(java.time.DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return isSeninBuka() ? getSeninJamMulai() : null;
            case TUESDAY: return isSelasaBuka() ? getSelasaJamMulai() : null;
            case WEDNESDAY: return isRabuBuka() ? getRabuJamMulai() : null;
            case THURSDAY: return isKamisBuka() ? getKamisJamMulai() : null;
            case FRIDAY: return isJumatBuka() ? getJumatJamMulai() : null;
            case SATURDAY: return isSabtuBuka() ? getSabtuJamMulai() : null;
            case SUNDAY: return isMingguBuka() ? getMingguJamMulai() : null;
            default: return null;
        }
    }

    public String getJamTutupByDay(java.time.DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return isSeninBuka() ? getSeninJamSelesai() : null;
            case TUESDAY: return isSelasaBuka() ? getSelasaJamSelesai() : null;
            case WEDNESDAY: return isRabuBuka() ? getRabuJamSelesai() : null;
            case THURSDAY: return isKamisBuka() ? getKamisJamSelesai() : null;
            case FRIDAY: return isJumatBuka() ? getJumatJamSelesai() : null;
            case SATURDAY: return isSabtuBuka() ? getSabtuJamSelesai() : null;
            case SUNDAY: return isMingguBuka() ? getMingguJamSelesai() : null;
            default: return null;
        }
    }
}
