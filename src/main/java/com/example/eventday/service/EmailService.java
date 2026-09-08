package com.example.eventday.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@eventday.local}")
    private String from;

    @Value("${app.mail.from-name:Eventday}")
    private String fromName;

    public void sendOtpEmail(String to, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Eventday - Kode OTP Verifikasi");
            message.setText("Halo,\n\nKode OTP Anda adalah: " + otpCode + "\nBerlaku 5 menit.\n\nTerima kasih,\n" + fromName);
            mailSender.send(message);
            log.info("OTP email sent to {}", to);
        } catch (Exception e) {
            log.warn("Gagal kirim OTP email ke {}: {} | OTP: {}", to, e.getMessage(), otpCode);
        }
    }

    public void sendResetPasswordEmail(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Eventday - Kode Reset Password");
            message.setText("Halo,\n\nKode reset password Anda adalah: " + code + "\nBerlaku 15 menit.\n\nJika tidak merasa meminta reset, abaikan email ini.\n\nTerima kasih,\n" + fromName);
            mailSender.send(message);
            log.info("Reset password email sent to {}", to);
        } catch (Exception e) {
            log.warn("Gagal kirim reset password email ke {}: {} | Code: {}", to, e.getMessage(), code);
        }
    }
}
