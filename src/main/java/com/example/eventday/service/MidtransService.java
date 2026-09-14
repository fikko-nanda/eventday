package com.example.eventday.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class MidtransService {

    @Value("${midtrans.server-key}")
    private String serverKey;

    @Value("${midtrans.snap-url}")
    private String snapUrl;

    public Map<String, String> createSnapTransaction(String orderId, BigDecimal grossAmount, String customerName, String customerEmail) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. Setup Basic Auth Header (ServerKey + ":") Base64
        String authStr = serverKey + ":";
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
                "first_name", customerName,
                "email", customerEmail
        );

        Map<String, Object> requestBody = Map.of(
                "transaction_details", transactionDetails,
                "customer_details", customerDetails
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 3. Eksekusi HTTP Request ke Midtrans
        ResponseEntity<Map> response = restTemplate.postForEntity(snapUrl, entity, Map.class);

        if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> body = response.getBody();
            if (body != null) {
                return Map.of(
                        "snapToken", (String) body.get("token"),
                        "redirectUrl", (String) body.get("redirect_url")
                );
            }
        }
        throw new RuntimeException("Gagal menghubungkan transaksi ke Midtrans Snap API");
    }
}