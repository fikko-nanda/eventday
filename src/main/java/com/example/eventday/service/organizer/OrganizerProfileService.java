package com.example.eventday.service.organizer;

import com.example.eventday.entity.Organizer;
import com.example.eventday.entity.User;
import com.example.eventday.repository.OrganizerRepository;
import com.example.eventday.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerProfileService {

    private final OrganizerHelperService helperService;
    private final OrganizerRepository organizerRepository;
    private final UserRepository userRepository;

    public Map<String, Object> getProfile() {
        UUID uid = helperService.currentUserId();
        if (uid != null) {
            Optional<Organizer> opt = organizerRepository.findByUserUserId(uid);
            if (opt.isPresent()) {
                Organizer o = opt.get();
                User u = o.getUser();
                Map<String, Object> profile = new HashMap<>();
                profile.put("organizer_id", o.getOrganizerId().toString());
                profile.put("user_id", u.getUserId().toString());
                profile.put("name", o.getNameOrganizer());
                profile.put("pic_name", u.getName());
                profile.put("email", u.getEmail());
                profile.put("phone", u.getPhone());
                profile.put("npwp", o.getNpwpNumber());
                profile.put("bank_name", o.getBankName());
                profile.put("bank_account_number", o.getBankAccountNumber());
                profile.put("verification_status", o.getVerificationStatus());
                profile.put("avatar_url", "https://api.dicebear.com/7.x/identicon/svg?seed=" + o.getOrganizerId());
                profile.put("created_at", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
                return profile;
            }
            User u = userRepository.findById(uid).orElse(null);
            if (u != null) {
                Map<String, Object> profile = new HashMap<>();
                profile.put("name", u.getName());
                profile.put("pic_name", u.getName());
                profile.put("email", u.getEmail());
                profile.put("phone", u.getPhone());
                profile.put("verification_status", "UNREGISTERED");
                profile.put("note", "Belum terdaftar sebagai organizer — POST /organizer/register dulu");
                return profile;
            }
        }
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", "PT Harmoni Musik Indonesia");
        profile.put("pic_name", "Budi Santoso");
        profile.put("email", "budi.santoso@harmoni.co.id");
        profile.put("phone", "+6281234567890");
        profile.put("npwp", "01.234.567.8-901.000");
        profile.put("avatar_url", "https://api.dicebear.com/7.x/identicon/svg?seed=harmoni");
        profile.put("mock", true);
        return profile;
    }

    @Transactional
    public Map<String, Object> updateProfile(Map<String, Object> payload) {
        UUID uid = helperService.currentUserId();
        if (uid != null) {
            Optional<Organizer> opt = organizerRepository.findByUserUserId(uid);
            if (opt.isPresent()) {
                Organizer o = opt.get();
                if (payload.containsKey("name")) o.setNameOrganizer((String) payload.get("name"));
                if (payload.containsKey("organizer_name")) o.setNameOrganizer((String) payload.get("organizer_name"));
                if (payload.containsKey("npwp")) o.setNpwpNumber((String) payload.get("npwp"));
                if (payload.containsKey("npwp_number")) o.setNpwpNumber((String) payload.get("npwp_number"));
                if (payload.containsKey("bank_name")) o.setBankName((String) payload.get("bank_name"));
                if (payload.containsKey("bank_account_number")) o.setBankAccountNumber((String) payload.get("bank_account_number"));
                o.setUpdatedAt(LocalDateTime.now());
                organizerRepository.save(o);

                User u = o.getUser();
                if (payload.containsKey("pic_name")) u.setName((String) payload.get("pic_name"));
                if (payload.containsKey("phone")) u.setPhone((String) payload.get("phone"));
                userRepository.save(u);

                payload.put("organizer_id", o.getOrganizerId().toString());
                payload.put("updated_at", o.getUpdatedAt().toString());
                return payload;
            }
        }
        return payload;
    }

    public Map<String, Object> uploadAvatar(MultipartFile file) {
        String url = helperService.saveFile(file, "avatars");
        Map<String, Object> data = new HashMap<>();
        data.put("avatar_url", url);
        data.put("file_name", file.getOriginalFilename());
        data.put("file_size", file.getSize());
        return data;
    }

    public Map<String, Object> uploadPortfolio(MultipartFile file) {
        String url = helperService.saveFile(file, "organizer-docs/portfolio");
        Map<String, Object> data = new HashMap<>();
        data.put("portfolio_url", url);
        data.put("file_name", file.getOriginalFilename());
        data.put("file_size", file.getSize());
        return data;
    }

    public Map<String, Object> uploadDeed(MultipartFile file) {
        String url = helperService.saveFile(file, "organizer-docs/deeds");
        Map<String, Object> data = new HashMap<>();
        data.put("company_deed_url", url);
        data.put("file_name", file.getOriginalFilename());
        data.put("file_size", file.getSize());
        return data;
    }

    public Map<String, Object> getProfileDocuments() {
        UUID uid = helperService.currentUserId();
        if (uid != null) {
            Optional<Organizer> opt = organizerRepository.findByUserUserId(uid);
            if (opt.isPresent()) {
                Organizer o = opt.get();
                Map<String, Object> docs = new HashMap<>();
                docs.put("organizer_id", o.getOrganizerId().toString());
                docs.put("akta_perusahaan", o.getAktaPerusahaan());
                docs.put("has_akta", o.getAktaPerusahaan() != null);
                docs.put("portfolio_name", "portfolio_" + o.getOrganizerId() + ".pdf");
                docs.put("deed_name", o.getAktaPerusahaan() != null ? o.getAktaPerusahaan() : "Akta_Perusahaan.pdf");
                docs.put("ktp_name", "KTP_PIC.jpg");
                return docs;
            }
        }
        Map<String, Object> docs = new HashMap<>();
        docs.put("portfolio_name", "CV_Portofolio.pdf");
        docs.put("deed_name", "Akta_Perusahaan.pdf");
        docs.put("ktp_name", "KTP_PIC.jpg");
        docs.put("mock", true);
        return docs;
    }
}