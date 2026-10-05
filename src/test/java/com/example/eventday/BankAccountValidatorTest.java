package com.example.eventday;

import com.example.eventday.util.BankAccountValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BankAccountValidatorTest {

    private void assertRejected(Object raw, String expectedMessage) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> BankAccountValidator.clean(raw));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(expectedMessage, ex.getReason());
    }

    @Test
    @DisplayName("Nilai kosong dianggap tidak diisi, bukan error")
    void kosongDianggapOptional() {
        assertNull(BankAccountValidator.clean(null));
        assertNull(BankAccountValidator.clean(""));
        assertNull(BankAccountValidator.clean("   "));
    }

    @Test
    @DisplayName("Nomor rekening bank Indonesia 6-16 digit diterima apa adanya")
    void nomorPendekTerima() {
        assertEquals("123456", BankAccountValidator.clean("123456"));
        assertEquals("1234567890", BankAccountValidator.clean("1234567890"));
        assertEquals("123456789012", BankAccountValidator.clean("123456789012"));
        assertEquals("1234567890123456", BankAccountValidator.clean("1234567890123456"));
    }

    @Test
    @DisplayName("Lebih dari 16 digit ditolak")
    void lebihDari16DigitDitolak() {
        assertRejected("12345678901234567", "Nomor rekening maksimal 16 digit");
    }

    @Test
    @DisplayName("Separator spasi dan strip dibersihkan sebelum dicek panjang")
    void separatorDibersihkan() {
        assertEquals("12345678", BankAccountValidator.clean("1234 5678"));
        assertEquals("12345678", BankAccountValidator.clean("1234-5678"));
        assertEquals("1234567890123456", BankAccountValidator.clean("1234 5678-9012 3456"));
    }

    @Test
    @DisplayName("Titik tidak dianggap separator, agar angka desimal tidak ikut berubah")
    void titikDitolak() {
        assertRejected("1234.5678", "Nomor rekening hanya boleh berisi angka");
    }

    @Test
    @DisplayName("Panjang dihitung setelah separator dibuang, bukan karakter mentah")
    void panjangDihitungSetelahStrip() {
        // 17 angka dipisah strip -> tetap 17 digit -> harus ditolak
        assertRejected("1234-5678-9012-3456-7", "Nomor rekening maksimal 16 digit");
    }

    @Test
    @DisplayName("Huruf ditolak, tidak ikut ter-strip diam-diam")
    void hurufDitolak() {
        assertRejected("abc", "Nomor rekening hanya boleh berisi angka");
        assertRejected("1234abc", "Nomor rekening hanya boleh berisi angka");
        assertRejected("12a34", "Nomor rekening hanya boleh berisi angka");
    }

    @Test
    @DisplayName("Isian non-angka yang habis di-strip ditolak")
    void nonAngkaKosongDitolak() {
        assertRejected("- -", "Nomor rekening harus berisi angka");
        assertRejected("--", "Nomor rekening harus berisi angka");
        assertRejected(".", "Nomor rekening hanya boleh berisi angka");
    }

    @Test
    @DisplayName("JSON number dari input type=number diterima tanpa ClassCastException")
    void jsonNumberDiterima() {
        assertEquals("123456", BankAccountValidator.clean(123456L));
        assertEquals("123456", BankAccountValidator.clean(123456));
        assertEquals("1234", BankAccountValidator.clean(1234L));
    }

    @Test
    @DisplayName("JSON number yang melebihi batas juga ditolak")
    void jsonNumberLebihDitolak() {
        assertRejected(12345678901234567L, "Nomor rekening maksimal 16 digit");
    }

    @Test
    @DisplayName("Nilai yang valid tidak tertinggal spasi di hasil")
    void hasilBersihTanpaSpasi() {
        assertEquals("12345678", BankAccountValidator.clean("  1234-5678  "));
    }
}
