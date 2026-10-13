package com.example.eventday.service;

import com.example.eventday.dto.TicketDetailResponse;
import com.example.eventday.dto.TicketEmailData;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:noreply@eventday.local}")
    private String from;

    @Value("${app.mail.from-name:Eventday}")
    private String fromName;

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

    // 4. Notifikasi Konfirmasi Pembayaran & E-Ticket
    @Async
    public void sendOrderConfirmationEmail(String to, String orderNumber, String eventTitle, int qty) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(getFormattedFrom());
            helper.setTo(to);
            helper.setSubject("Eventday - Konfirmasi Pembayaran & E-Ticket: " + orderNumber);

            message.addHeader("Precedence", "bulk");
            message.addHeader("List-Unsubscribe", "<mailto:unsubscribe@eventday.web.app?subject=unsubscribe>");

            String text = "Halo,\n\nPembayaran untuk pesanan " + orderNumber + " telah berhasil diverifikasi!\n\nDetail Pesanan:\n"
                    + "Event: " + eventTitle + "\n"
                    + "Jumlah Tiket: " + qty + "\n\n"
                    + "E-ticket Anda sudah terbit dan dapat dilihat pada menu 'My Tickets' di aplikasi Eventday.\n\nTerima kasih,\n" + fromName;
            helper.setText(text, false);

            mailSender.send(message);
            log.info("Order confirmation email sent to {}", to);
        } catch (Exception e) {
            log.warn("Gagal kirim order confirmation email ke {}: {}", to, e.getMessage());
        }
    }

    // 5. Kirim E-Ticket Email dengan Detail Lengkap (Multiple Tickets per Recipient)
    @Async
    public void sendTicketEmail(TicketEmailData ticketData) {
        String to = ticketData.getRecipientEmail();
        if (to == null || to.isBlank()) {
            log.warn("Recipient email is empty, skipping email send");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(getFormattedFrom());
            helper.setTo(to);
            helper.setSubject("E-Ticket Anda: " + ticketData.getEventTitle());

            message.addHeader("Precedence", "bulk");
            message.addHeader("List-Unsubscribe", "<mailto:unsubscribe@eventday.web.app?subject=unsubscribe>");

            // Render HTML template with ticket data
            Context context = new Context();
            context.setVariable("data", ticketData);
            String html = templateEngine.process("email/ticket-email", context);

            helper.setText(html, true);

            mailSender.send(message);
            log.info("E-Ticket email sent to {} for order {}", to, ticketData.getOrderNumber());
        } catch (Exception e) {
            log.error("Gagal kirim E-Ticket email ke {}: {}", to, e.getMessage(), e);
        }
    }

    // 5. Kirim PDF Tiket ke Attendee (Single PDF dengan QR code embedded)
    @Transactional
    public void sendTicketPdf(String to, String attendeeName, TicketDetailResponse detail) {
        try {
            // 1. Generate QR Code as PNG Base64 Data URI (exact display size 220x220)
            String qrCodeBase64 = generateQrCodeBase64(detail.getTicketCode());

            // 2. Render HTML template with QR code embedded
            Context context = new Context();
            context.setVariable("detail", detail);
            context.setVariable("qrCodeBase64", qrCodeBase64);
            String html = templateEngine.process("email/ticket-pdf", context);

            // 3. Convert HTML -> PDF (QR code embedded in PDF)
            byte[] pdfBytes = htmlToPdf(html);

            // 4. Send email with PDF attachment
            sendEmailWithPdfAttachment(to, attendeeName, detail, pdfBytes);

            log.info("PDF ticket sent to {} for attendee {}", to, attendeeName);
        } catch (Exception e) {
            log.error("Gagal kirim PDF ticket ke {}: {}", to, e.getMessage(), e);
        }
    }

    private String generateQrCodeBase64(String ticketCode) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            // Generate at exact display size (220x220) to avoid downscaling artifacts
            BitMatrix bitMatrix = qrCodeWriter.encode(ticketCode, BarcodeFormat.QR_CODE, 220, 220);

            BufferedImage image = new BufferedImage(220, 220, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 220; x++) {
                for (int y = 0; y < 220; y++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.warn("Gagal generate QR code: {}", e.getMessage());
            return "";
        }
    }

    private byte[] htmlToPdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "classpath:/templates/email/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            log.error("Gagal convert HTML to PDF: {}", e.getMessage());
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private void sendEmailWithPdfAttachment(String to, String attendeeName,
            TicketDetailResponse detail, byte[] pdfBytes) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(getFormattedFrom());
        helper.setTo(to);
        helper.setSubject("E-Ticket Anda: " + detail.getEventTitle());

        // Header tambahan untuk improve deliverability
        message.addHeader("Precedence", "bulk");
        message.addHeader("List-Unsubscribe", "<mailto:unsubscribe@eventday.web.app?subject=unsubscribe>");

        // HTML body (tanpa inline QR, QR sudah di PDF)
        String htmlBody = buildHtmlEmailBody(detail, attendeeName);
        helper.setText(htmlBody, true);

        // Attach PDF (sudah include QR code)
        String safeEventTitle = detail.getEventTitle().replaceAll("[^a-zA-Z0-9]", "-");
        String safeAttendeeName = attendeeName.replaceAll("[^a-zA-Z0-9]", "-");
        String filename = "ticket-" + safeEventTitle + "-" + safeAttendeeName + ".pdf";
        helper.addAttachment(filename, new ByteArrayDataSource(pdfBytes, "application/pdf"));

        mailSender.send(message);
    }

    private String buildHtmlEmailBody(TicketDetailResponse detail, String attendeeName) {
        return String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #1a1a2e;">Halo %s,</h2>
                <p>E-Ticket Anda untuk <strong>%s</strong> sudah siap.</p>
                <p>File PDF tiket terlampir di email ini (sudah include QR code).</p>
                <p><strong>Kode Tiket:</strong> %s</p>
                <hr style="margin: 20px 0;" />
                <p style="font-size: 12px; color: #666;">
                    Eventday | %s | %s
                </p>
            </div>
            """,
            attendeeName,
            detail.getEventTitle(),
            detail.getTicketCode(),
            detail.getEventDate(),
            detail.getVenueName()
        );
    }

    // Helper untuk format pengirim: "Eventday <noreply@eventday.local>"
    private String getFormattedFrom() {
        if (fromName != null && !fromName.isBlank()) {
            return String.format("%s <%s>", fromName, from);
        }
        return from;
    }
}