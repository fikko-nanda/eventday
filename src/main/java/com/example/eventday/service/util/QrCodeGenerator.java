package com.example.eventday.service.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Generator QR code PNG untuk e-ticket.
 *
 * QR di-generate di sisi server supaya kode tiket tidak pernah dikirim ke
 * layanan pihak ketiga. Kode tiket adalah kunci akses ke
 * GET /api/tickets/issued-detail?ticketCode=, jadi tidak boleh bocor.
 */
@Slf4j
public final class QrCodeGenerator {

    private static final int DEFAULT_SIZE = 300;

    private QrCodeGenerator() {
    }

    /**
     * @return byte PNG, atau null bila gagal (supaya email tetap terkirim tanpa QR)
     */
    public static byte[] toPng(String content, int size) {
        if (content == null || content.isBlank()) {
            return null;
        }
        int dimension = size > 0 ? size : DEFAULT_SIZE;
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, dimension, dimension);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(matrix, "PNG", out);
                return out.toByteArray();
            }
        } catch (WriterException | IOException | IllegalArgumentException e) {
            log.warn("Gagal generate QR code untuk konten '{}': {}", content, e.getMessage());
            return null;
        }
    }
}
