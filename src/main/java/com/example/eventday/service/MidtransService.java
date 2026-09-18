package com.example.eventday.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings("null")
public class MidtransService {

    @Value("${midtrans.server-key}")
    private String serverKey;

    @Value("${midtrans.snap-url}")
    private String snapUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, String> createSnapTransaction(String orderId, BigDecimal grossAmount, String customerName, String customerEmail) {
        String cleanServerKey = (serverKey != null) ? serverKey.trim() : "";

        if (cleanServerKey.isEmpty()) {
            log.error("Midtrans Server Key kosong! Periksa application.properties");
            throw new IllegalStateException("Midtrans Server Key belum dikonfigurasi.");
        }

        // Log awalan key untuk memastikan properti ter-load dengan benar di terminal
        String maskedKey = cleanServerKey.length() > 12 
                ? cleanServerKey.substring(0, 12) + "..." 
                : cleanServerKey;
        log.info("Mengirim transaksi ke Midtrans. URL: {}, Key Prefix: {}, Length: {}", 
                snapUrl, maskedKey, cleanServerKey.length());

        // 1. Setup Basic Auth Header (ServerKey + ":") Base64
        String authStr = cleanServerKey + ":";
        String base64Auth = Base64.getEncoder().encodeToString(authStr.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Basic " + base64Auth);

        // 2. Setup Payload JSON Midtrans
        Map<String, Object> transactionDetails = Map.of(
                "order_id", orderId,
                "gross_amount", grossAmount.longValue()
        );

        Map<String, Object> customerDetails = Map.of(
                "first_name", customerName != null ? customerName : "",
                "email", customerEmail != null ? customerEmail : ""
        );

        Map<String, Object> requestBody = Map.of(
                "transaction_details", transactionDetails,
                "customer_details", customerDetails
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 3. Eksekusi HTTP Request
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    snapUrl,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if ((response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) 
                    && response.getBody() != null) {

                Map<String, Object> body = response.getBody();
                String token = String.valueOf(body.get("token"));
                String redirectUrl = String.valueOf(body.get("redirect_url"));

                log.info("Midtrans Snap Token berhasil didapatkan: {}", token);

                return Map.of(
                        "snapToken", token,
                        "redirectUrl", redirectUrl
                );
            }
        } catch (HttpStatusCodeException e) {
            // Menangkap dan mencetak detail response error dari API Midtrans
            log.error("Midtrans API Error Status: {}, Response Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Midtrans API Error (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error tidak terduga saat menghubungi Midtrans: ", e);
            throw new RuntimeException("Gagal menghubungkan transaksi ke Midtrans Snap API: " + e.getMessage());
        }

        throw new RuntimeException("Gagal menghubungkan transaksi ke Midtrans Snap API");
    }
}