package com.cinemax.cinemax.infrastructure.payment;

import com.cinemax.cinemax.domain.booking.Transaksi;
import com.cinemax.cinemax.domain.config.BioskopConfig;
import com.cinemax.cinemax.domain.config.BioskopConfigRepository;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CinemaxPaymentGateway implements PaymentGateway {

    @Autowired
    private BioskopConfigRepository configRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String generatePaymentUrl(Transaksi transaksi) throws Exception {
        BioskopConfig config = configRepository.findById("SINGLETON").orElse(null);
        if (config == null || config.getPaymentServerKey() == null || config.getPaymentServerKey().isEmpty()) {
            throw new Exception("Server Key Midtrans belum dikonfigurasi di Pengaturan.");
        }

        String serverKey = config.getPaymentServerKey();
        
        // Pilih URL Sandbox atau Production (Disini kita default ke Sandbox)
        String apiUrl = "https://app.sandbox.midtrans.com/snap/v1/transactions";

        // Setup Headers Auth Basic
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        
        String authHeader = "Basic " + Base64.getEncoder().encodeToString((serverKey + ":").getBytes());
        headers.set("Authorization", authHeader);

        // Setup Request Body
        Map<String, Object> body = new HashMap<>();
        
        Map<String, Object> transactionDetails = new HashMap<>();
        transactionDetails.put("order_id", transaksi.getId().toString()); // order_id harus string unik
        transactionDetails.put("gross_amount", transaksi.getTotalHarga()); // total yang harus dibayar
        
        body.put("transaction_details", transactionDetails);

        // Informasi Customer (Opsional tapi direkomendasikan)
        Map<String, Object> customerDetails = new HashMap<>();
        customerDetails.put("first_name", transaksi.getPelanggan().getNamaLengkap());
        customerDetails.put("email", transaksi.getPelanggan().getEmail());
        body.put("customer_details", customerDetails);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // Hit API Midtrans
        ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String redirectUrl = (String) response.getBody().get("redirect_url");
            if (redirectUrl != null) {
                return redirectUrl;
            }
        }
        
        throw new Exception("Gagal mendapatkan link pembayaran dari Midtrans: " + response.getBody());
    }
}
