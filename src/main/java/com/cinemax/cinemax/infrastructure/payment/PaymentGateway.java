package com.cinemax.cinemax.infrastructure.payment;

import com.cinemax.cinemax.domain.booking.Transaksi;

public interface PaymentGateway {
    
    /**
     * Menghasilkan URL Pembayaran (Snap URL) untuk transaksi tertentu.
     * @param transaksi Object transaksi yang sudah disimpan di database
     * @return URL pembayaran yang bisa di-redirect oleh frontend
     */
    String generatePaymentUrl(Transaksi transaksi) throws Exception;

}
