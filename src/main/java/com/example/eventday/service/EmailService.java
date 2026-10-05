package com.example.eventday.service;

import com.example.eventday.dto.TicketEmailData;
import com.example.eventday.service.util.QrCodeGenerator;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.mail.from-name:Eventday}")
    private String fromName;

    @Value("${app.reset-password.frontend-url:https://eventday.dnabisa.tech}")
    private String frontendUrl;

    // Helper untuk format pengirim: "Eventday <email>"
    private String getFormattedFrom() {
        if (fromName != null && !fromName.isBlank()) {
            return String.format("%s <%s>", fromName, from);
        }
        return from;
    }

    /**
     * Mengirim email e-ticket (HTML + QR inline) ke satu alamat penerima.
     * One email per penerima, berisi semua tiket milik penerima tersebut.
     */
    @Async
    public void sendTicketEmail(TicketEmailData data) {
        if (data == null || data.getRecipientEmail() == null || data.getRecipientEmail().isBlank()) {
            log.warn("Penerima email tiket kosong, email tidak dikirim.");
            return;
        }
        if (from == null || from.isBlank()) {
            log.error("app.mail.from kosong (kemungkinan MAIL_USERNAME belum di-set). Email tiket {} tidak dikirim.",
                    data.getOrderNumber());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(getFormattedFrom());
            helper.setTo(data.getRecipientEmail());
            helper.setSubject("Eventday - E-Ticket Anda: " + data.getEventTitle());

            StringBuilder html = new StringBuilder();
            html.append("<div style=\"font-family:Arial,Helvetica,sans-serif;font-size:14px;color:#1f2937;\">");
            html.append("<p>Halo ").append(escapeHtml(data.getBuyerName())).append(",</p>");
            html.append("<p>Pembayaran Anda berhasil. E-ticket resmi sudah terbit dan terlampir di bawah ini.</p>");

            html.append("<table cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse:collapse;margin:16px 0;\">");
            appendRow(html, "Nomor Pesanan", data.getOrderNumber());
            appendRow(html, "Event", data.getEventTitle());
            appendRow(html, "Tanggal", data.getEventDate());
            appendRow(html, "Lokasi", data.getVenueName());
            appendRow(html, "Total", data.getTotalAmount());
            html.append("</table>");

            int qrIndex = 0;
            for (TicketEmailData.TicketLine ticket : data.getTickets()) {
                byte[] qr = QrCodeGenerator.toPng(ticket.getTicketCode(), 300);
                String cid = null;
                if (qr != null) {
                    cid = "qr" + qrIndex++;
                    helper.addInline(cid, new ByteArrayResource(qr), "image/png");
                }
                html.append(buildTicketCard(ticket, cid));
            }

            html.append("<p style=\"margin-top:20px;font-size:12px;color:#6b7280;\">");
            html.append("Simpan email ini atau buka halaman My Tickets di ")
                    .append("<a href=\"").append(frontendUrl).append("\">").append(escapeHtml(frontendUrl)).append("</a>")
                    .append(" untuk melihat detail tiket.</p>");
            html.append("<p style=\"font-size:12px;color:#6b7280;\">Terima kasih,<br>").append(escapeHtml(fromName)).append("</p>");
            html.append("</div>");

            helper.setText(html.toString(), true);

            mailSender.send(message);
            log.info("E-ticket email terkirim ke {} untuk order {} ({} tiket)", data.getRecipientEmail(),
                    data.getOrderNumber(), data.getTickets().size());
        } catch (Exception e) {
            log.error("Gagal kirim e-ticket ke {} untuk order {}: {}", data.getRecipientEmail(),
                    data.getOrderNumber(), e.getMessage());
        }
    }

    private String buildTicketCard(TicketEmailData.TicketLine ticket, String qrContentId) {
        StringBuilder html = new StringBuilder();
        html.append("<div style=\"border:1px solid #e5e7eb;border-radius:8px;padding:16px;margin:12px 0;\">");

        if (qrContentId != null) {
            html.append("<img src=\"cid:").append(qrContentId)
                    .append("\" width=\"220\" height=\"220\" alt=\"QR Code Tiket\" ")
                    .append("style=\"display:block;margin-bottom:12px;\">");
        }

        html.append("<table cellpadding=\"4\" cellspacing=\"0\">");
        appendRow(html, "Nama Peserta", ticket.getAttendeeName());
        appendRow(html, "Tipe Tiket", ticket.getTierName());
        appendRow(html, "Harga", ticket.getPriceDisplay());
        appendRow(html, "Kode Tiket", ticket.getTicketCode());
        html.append("</table>");
        html.append("</div>");
        return html.toString();
    }

    private void appendRow(StringBuilder html, String label, String value) {
        html.append("<tr>");
        html.append("<td style=\"color:#6b7280;padding-right:16px;\">").append(escapeHtml(label)).append("</td>");
        html.append("<td><strong>").append(escapeHtml(value)).append("</strong></td>");
        html.append("</tr>");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "-";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // 1. Kirim OTP Verifikasi Pendaftaran
    @Async
    public void sendOtpEmail(String to, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getFormattedFrom());
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
    @Async
    public void sendResetPasswordEmail(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getFormattedFrom());
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
    @Async
    public void sendPasswordChangedNotification(String to) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getFormattedFrom());
            message.setTo(to);
            message.setSubject("Eventday - Keamanan Akun: Password Berhasil Diubah");
            message.setText("Halo,\n\nPassword akun Eventday Anda baru saja berhasil diubah.\n\nJika Anda tidak melakukan perubahan ini, segera hubungi admin atau lakukan reset password.\n\nTerima kasih,\n" + fromName);
            mailSender.send(message);
            log.info("Password changed notification sent to {}", to);
        } catch (Exception e) {
            log.warn("Gagal kirim notifikasi password changed ke {}: {}", to, e.getMessage());
        }
    }
}