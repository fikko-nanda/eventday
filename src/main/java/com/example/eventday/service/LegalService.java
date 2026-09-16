package com.example.eventday.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LegalService {

    public Map<String, Object> getTermsAndConditions() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Syarat dan Ketentuan EventDay");
        data.put("slug", "terms-conditions");
        data.put("version", "1.0");
        data.put("content", "<h1>Syarat dan Ketentuan EventDay</h1>"
                + "<p>Selamat datang di <strong>EventDay</strong>. Dengan mengakses dan menggunakan platform ini, Anda menyetujui seluruh syarat dan ketentuan berikut:</p>"
                + "<h2>1. Akun Pengguna</h2><p>Pengguna wajib memberikan data yang valid saat registrasi. Akun dengan data palsu dapat ditangguhkan.</p>"
                + "<h2>2. Pembelian Tiket</h2><p>Tiket yang telah dibeli tidak dapat dipindahtangankan kecuali melalui fitur resmi refund/reschedule.</p>"
                + "<h2>3. Pembayaran</h2><p>Pembayaran diproses via Midtrans Snap. Pesanan PENDING akan expired dalam 15 menit jika tidak dibayar.</p>"
                + "<h2>4. Pengembalian Dana</h2><p>Refund hanya berlaku untuk event yang dibatalkan/dijadwalkan ulang oleh penyelenggara.</p>"
                + "<h2>5. Tanggung Jawab EO</h2><p>Event Organizer bertanggung jawab atas pelaksanaan event sesuai deskripsi yang dipublikasikan.</p>");
        data.put("sections", java.util.List.of(
                Map.of("heading", "Akun Pengguna", "body", "Pengguna wajib memberikan data valid."),
                Map.of("heading", "Pembelian Tiket", "body", "Tiket tidak dapat dipindahtangankan kecuali via refund resmi."),
                Map.of("heading", "Pembayaran", "body", "Midtrans Snap, expiry 15 menit."),
                Map.of("heading", "Refund", "body", "Hanya untuk event batal/reschedule."),
                Map.of("heading", "Tanggung Jawab EO", "body", "EO bertanggung jawab atas pelaksanaan event.")
        ));
        data.put("updated_at", "2026-09-16");
        return data;
    }

    public Map<String, Object> getPrivacyPolicy() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Kebijakan Privasi EventDay");
        data.put("slug", "privacy-policy");
        data.put("version", "1.0");
        data.put("content", "<h1>Kebijakan Privasi EventDay</h1>"
                + "<p><strong>EventDay</strong> berkomitmen melindungi kerahasiaan data dan privasi setiap pengguna sesuai UU PDP.</p>"
                + "<h2>1. Data yang Dikumpulkan</h2><p>Nama, email, NIK, nomor HP, data transaksi dan log aktivitas.</p>"
                + "<h2>2. Penggunaan Data</h2><p>Untuk verifikasi akun, pemrosesan tiket, notifikasi OTP/reset, dan analitik platform.</p>"
                + "<h2>3. Penyimpanan</h2><p>Data disimpan terenkripsi di PostgreSQL, password di-BCrypt, token JWT HttpOnly 24 jam.</p>"
                + "<h2>4. Berbagi Data</h2><p>Tidak dibagikan ke pihak ketiga tanpa persetujuan, kecuali payment gateway Midtrans & audit internal.</p>"
                + "<h2>5. Hak Pengguna</h2><p>Pengguna dapat meminta akses, koreksi, atau penghapusan data via support@eventday.local.</p>");
        data.put("sections", java.util.List.of(
                Map.of("heading", "Data yang Dikumpulkan", "body", "Nama, email, NIK, HP, transaksi."),
                Map.of("heading", "Penggunaan Data", "body", "Verifikasi, ticketing, OTP, analitik."),
                Map.of("heading", "Penyimpanan", "body", "PostgreSQL terenkripsi, BCrypt, JWT HttpOnly."),
                Map.of("heading", "Berbagi Data", "body", "Hanya Midtrans & audit internal."),
                Map.of("heading", "Hak Pengguna", "body", "Akses/koreksi/hapus via support.")
        ));
        data.put("updated_at", "2026-09-16");
        return data;
    }
}