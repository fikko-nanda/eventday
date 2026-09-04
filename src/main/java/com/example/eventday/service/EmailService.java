package com.example.eventday.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@eventday.local}")
    private String fromEmail;

    @Value("${app.mail.from-name:Eventday}")
    private String fromName;

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    public void sendOtpEmail(String toEmail, String toName, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Kode OTP Verifikasi Email - Eventday");

            String html = buildOtpHtml(toName, otpCode);

            helper.setText(html, true);

            mailSender.send(message);
            log.info("OTP email terkirim ke {} (otp={})", toEmail, otpCode);
        } catch (MessagingException e) {
            log.error("Gagal kirim OTP ke {}: {}", toEmail, e.getMessage());
            if (e.getMessage() != null && (e.getMessage().contains("Too many emails") || e.getMessage().contains("550 5.7.0"))) {
                log.warn("[DEV] Mailtrap rate limit, OTP untuk {} adalah: {}", toEmail, otpCode);
                return;
            }
            throw new RuntimeException("Gagal mengirim email OTP: " + e.getMessage());
        } catch (Exception e) {
            log.error("Gagal kirim OTP ke {}: {}", toEmail, e.getMessage());
        
            if (e.getMessage() != null && (e.getMessage().contains("Mail server connection failed") || e.getMessage().contains("Too many emails") || e.getMessage().contains("550 5.7.0") || e.getMessage().contains("Failed messages"))) {
                log.warn("[DEV] Mailtrap belum dikonfigurasi/rate limit, OTP untuk {} adalah: {}", toEmail, otpCode);
                return;
            }
            throw new RuntimeException("Gagal mengirim email OTP: " + e.getMessage());
        }
    }

    private String buildOtpHtml(String name, String otpCode) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #eee;border-radius:8px;overflow:hidden">
                  <div style="background:#4F46E5;color:white;padding:20px;text-align:center">
                    <h2 style="margin:0">Eventday - Verifikasi Email</h2>
                  </div>
                  <div style="padding:24px;color:#333">
                    <p>Halo <b>%s</b>,</p>
                    <p>Terima kasih telah mendaftar di <b>Eventday</b>. Gunakan kode OTP berikut untuk verifikasi email Anda:</p>
                    <div style="text-align:center;margin:24px 0">
                      <span style="font-size:32px;font-weight:bold;letter-spacing:8px;background:#f5f5ff;padding:12px 24px;border-radius:8px;border:1px dashed #4F46E5">%s</span>
                    </div>
                    <p style="color:#666;font-size:14px">Kode berlaku <b>%d menit</b>. Jangan bagikan kode ini kepada siapapun.</p>
                    <p style="color:#666;font-size:14px">Jika Anda tidak merasa mendaftar, abaikan email ini.</p>
                    <hr style="border:none;border-top:1px solid #eee;margin:24px 0"/>
                    <p style="font-size:12px;color:#999;text-align:center">Eventday Ticketing &copy; 2026</p>
                  </div>
                </div>
                """.formatted(name != null ? name : "Buyer", otpCode, otpExpiryMinutes);
    }

    public void sendResetOtpEmail(String toEmail, String toName, String otpCode, int expiryMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Kode Reset Password - Eventday");

            String html = buildResetOtpHtml(toName, otpCode, expiryMinutes);

            helper.setText(html, true);

            mailSender.send(message);
            log.info("Reset OTP email terkirim ke {} (otp={})", toEmail, otpCode);
        } catch (MessagingException e) {
            log.error("Gagal kirim reset OTP ke {}: {}", toEmail, e.getMessage());
            if (e.getMessage() != null && (e.getMessage().contains("Too many emails") || e.getMessage().contains("550 5.7.0"))) {
                log.warn("[DEV] Mailtrap rate limit, Reset OTP untuk {} adalah: {}", toEmail, otpCode);
                return;
            }
            throw new RuntimeException("Gagal mengirim email reset OTP: " + e.getMessage());
        } catch (Exception e) {
            log.error("Gagal kirim reset OTP ke {}: {}", toEmail, e.getMessage());

            if (e.getMessage() != null && (e.getMessage().contains("Mail server connection failed") || e.getMessage().contains("Too many emails") || e.getMessage().contains("550 5.7.0") || e.getMessage().contains("Failed messages"))) {
                log.warn("[DEV] Mailtrap belum dikonfigurasi/rate limit, Reset OTP untuk {} adalah: {}", toEmail, otpCode);
                return;
            }
            throw new RuntimeException("Gagal mengirim email reset OTP: " + e.getMessage());
        }
    }

    private String buildResetOtpHtml(String name, String otpCode, int expiryMinutes) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #eee;border-radius:8px;overflow:hidden">
                  <div style="background:#DC2626;color:white;padding:20px;text-align:center">
                    <h2 style="margin:0">Eventday - Reset Password</h2>
                  </div>
                  <div style="padding:24px;color:#333">
                    <p>Halo <b>%s</b>,</p>
                    <p>Anda meminta reset password. Gunakan kode OTP berikut untuk melanjutkan:</p>
                    <div style="text-align:center;margin:24px 0">
                      <span style="font-size:32px;font-weight:bold;letter-spacing:8px;background:#fff0f0;padding:12px 24px;border-radius:8px;border:1px dashed #DC2626">%s</span>
                    </div>
                    <p style="color:#666;font-size:14px">Kode berlaku <b>%d menit</b>. Jangan bagikan kode ini kepada siapapun.</p>
                    <p style="color:#666;font-size:14px">Jika Anda tidak meminta reset password, abaikan email ini.</p>
                    <hr style="border:none;border-top:1px solid #eee;margin:24px 0"/>
                    <p style="font-size:12px;color:#999;text-align:center">Eventday Ticketing &copy; 2026</p>
                  </div>
                </div>
                """.formatted(name != null ? name : "Buyer", otpCode, expiryMinutes);
    }

    public void sendResetPasswordEmail(String toEmail, String toName, String resetLink, int expiryMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Reset Password - Eventday");

            String html = buildResetPasswordHtml(toName, resetLink, expiryMinutes);

            helper.setText(html, true);

            mailSender.send(message);
            log.info("Reset password email terkirim ke {}", toEmail);
        } catch (MessagingException e) {
            log.error("Gagal kirim reset password ke {}: {}", toEmail, e.getMessage());
            if (e.getMessage() != null && (e.getMessage().contains("Too many emails") || e.getMessage().contains("550 5.7.0"))) {
                log.warn("[DEV] Mailtrap rate limit, Reset link untuk {} adalah: {}", toEmail, resetLink);
                return;
            }
            throw new RuntimeException("Gagal mengirim email reset password: " + e.getMessage());
        } catch (Exception e) {
            log.error("Gagal kirim reset password ke {}: {}", toEmail, e.getMessage());

            if (e.getMessage() != null && (e.getMessage().contains("Mail server connection failed") || e.getMessage().contains("Too many emails") || e.getMessage().contains("550 5.7.0") || e.getMessage().contains("Failed messages"))) {
                log.warn("[DEV] Mailtrap belum dikonfigurasi/rate limit, Reset link untuk {} adalah: {}", toEmail, resetLink);
                return;
            }
            throw new RuntimeException("Gagal mengirim email reset password: " + e.getMessage());
        }
    }

    private String buildResetPasswordHtml(String name, String resetLink, int expiryMinutes) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #eee;border-radius:8px;overflow:hidden">
                  <div style="background:#DC2626;color:white;padding:20px;text-align:center">
                    <h2 style="margin:0">Eventday - Reset Password</h2>
                  </div>
                  <div style="padding:24px;color:#333">
                    <p>Halo <b>%s</b>,</p>
                    <p>Anda meminta reset password untuk akun <b>Eventday</b>. Klik tombol di bawah untuk membuat password baru:</p>
                    <div style="text-align:center;margin:24px 0">
                      <a href="%s" style="background:#DC2626;color:white;padding:14px 28px;border-radius:6px;text-decoration:none;font-weight:bold;display:inline-block">Reset Password</a>
                    </div>
                    <p style="color:#666;font-size:14px">Link berlaku <b>%d menit</b>. Jangan bagikan link ini kepada siapapun.</p>
                    <p style="color:#666;font-size:14px">Jika Anda tidak meminta reset password, abaikan email ini.</p>
                    <hr style="border:none;border-top:1px solid #eee;margin:24px 0"/>
                    <p style="font-size:12px;color:#999;text-align:center">Eventday Ticketing &copy; 2026</p>
                  </div>
                </div>
                """.formatted(name != null ? name : "Buyer", resetLink, expiryMinutes);
    }
}
