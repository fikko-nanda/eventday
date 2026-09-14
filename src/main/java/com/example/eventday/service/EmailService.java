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

    // 1. Kirim OTP Verifikasi Pendaftaran
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

    // 2. Kirim Kode Reset Password (Lupa Password)
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

    // 3. Notifikasi Sukses Ganti Password Mandiri
    public void sendPasswordChangedNotification(String to) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Eventday - Keamanan Akun: Password Berhasil Diubah");
            message.setText("Halo,\n\nPassword akun Eventday Anda baru saja berhasil diubah.\n\nJika Anda tidak melakukan perubahan ini, segera hubungi admin atau lakukan reset password.\n\nTerima kasih,\n" + fromName);
            mailSender.send(message);
            log.info("Password changed notification sent to {}", to);
        } catch (Exception e) {
            log.warn("Gagal kirim notifikasi password changed ke {}: {}", to, e.getMessage());
        }
    }

    // 4. Notifikasi Konfirmasi Pembayaran & E-Ticket
    public void sendOrderConfirmationEmail(String to, String orderNumber, String eventTitle, int qty) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Eventday - Konfirmasi Pembayaran & E-Ticket: " + orderNumber);
            message.setText("Halo,\n\nPembayaran untuk pesanan " + orderNumber + " telah berhasil diverifikasi!\n\nDetail Pesanan:\n"
                    + "Event: " + eventTitle + "\n"
                    + "Jumlah Tiket: " + qty + "\n\n"
                    + "E-ticket Anda sudah terbit dan dapat dilihat pada menu 'My Tickets' di aplikasi Eventday.\n\nTerima kasih,\n" + fromName);
            mailSender.send(message);
            log.info("Order confirmation email sent to {}", to);
        } catch (Exception e) {
            log.warn("Gagal kirim order confirmation email ke {}: {}", to, e.getMessage());
        }
    }
}