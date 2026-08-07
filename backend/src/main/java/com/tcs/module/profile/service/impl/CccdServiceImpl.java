package com.tcs.module.profile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.profile.dto.CccdInfoDto;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.security.AuthHelper;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class CccdServiceImpl implements CccdService {

    private final AuthHelper authHelper;
    private final SystemParameterRepository systemParameterRepository;
    private final ClientRepository clientRepository;
    private final TutorRepository tutorRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DOB_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String KEY_PREFIX = "cccd:";
    private static final String DEFAULT_ISSUE_PLACE = "Cục Cảnh sát QLHC về TTXH";

    @Override
    public CccdInfoDto scanFromImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh CCCD.");
        }
        BufferedImage image;
        try {
            image = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("Không đọc được file ảnh.");
        }
        if (image == null) {
            throw new IllegalArgumentException(
                    "Không đọc được file ảnh (định dạng không hỗ trợ). Hãy dùng ảnh JPG hoặc PNG.");
        }

        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));

        // 1) Thử đọc toàn ảnh. 2) Không được -> crop vùng góc trên-phải (vị trí QR trên CCCD) đọc lại.
        String qr = tryDecode(image, hints);
        if (qr == null) {
            BufferedImage cropped = cropTopRight(image);
            if (cropped != null) {
                qr = tryDecode(cropped, hints);
            }
        }
        if (qr == null) {
            throw new IllegalArgumentException(
                    "Không đọc được mã QR trên ảnh CCCD. Hãy chụp lại MẶT TRƯỚC thẻ: cận vào mã QR "
                            + "(góc trên bên phải), đủ sáng, rõ nét, không chói/nghiêng.");
        }
        CccdInfoDto dto = parseQr(qr);
        // Chặn ngay khi quét: CCCD đã thuộc tài khoản khác -> không hợp lệ, bắt chụp lại đúng thẻ của mình.
        assertCccdNotUsedByOthers(dto.getCccdNumber(), authHelper.currentUserId());
        return dto;
    }

    /**
     * Số CCCD phải là duy nhất giữa các tài khoản: nếu đã có user KHÁC dùng số này -> không hợp lệ.
     */
    private void assertCccdNotUsedByOthers(String cccdNumber, Long ownerUserId) {
        if (!StringUtils.hasText(cccdNumber)) {
            return;
        }
        String norm = cccdNumber.trim();
        for (SystemParameter p : systemParameterRepository.findByParamKeyStartingWith(KEY_PREFIX)) {
            Long otherId;
            try {
                otherId = Long.valueOf(p.getParamKey().substring(KEY_PREFIX.length()));
            } catch (NumberFormatException e) {
                continue;
            }
            if (otherId.equals(ownerUserId)) {
                continue; // bỏ qua chính mình
            }
            String otherNumber;
            try {
                otherNumber = MAPPER.readValue(p.getParamValue(), CccdInfoDto.class).getCccdNumber();
            } catch (Exception e) {
                continue;
            }
            if (otherNumber != null && norm.equalsIgnoreCase(otherNumber.trim())) {
                throw new IllegalArgumentException(
                        "Số CCCD này đã được đăng ký cho tài khoản khác — không hợp lệ. "
                                + "Vui lòng dùng đúng CCCD của bạn (chụp lại nếu quét nhầm thẻ).");
            }
        }
    }

    /** Trả về nội dung QR hoặc null nếu không tìm thấy mã. */
    private String tryDecode(BufferedImage image, Map<DecodeHintType, Object> hints) {
        try {
            BinaryBitmap bitmap = new BinaryBitmap(
                    new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            return new MultiFormatReader().decode(bitmap, hints).getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    /** Cắt vùng góc trên bên phải (~55% chiều rộng × ~55% chiều cao) — nơi có mã QR trên CCCD. */
    private BufferedImage cropTopRight(BufferedImage image) {
        try {
            int w = image.getWidth();
            int h = image.getHeight();
            int x = (int) (w * 0.45);
            int cw = w - x;
            int ch = (int) (h * 0.55);
            if (cw <= 0 || ch <= 0) {
                return null;
            }
            return image.getSubimage(x, 0, cw, ch);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * QR CCCD gắn chip (mặt trước) có dạng:
     * SốCCCD|SốCMND_cũ|Họ tên|NgàySinh(ddMMyyyy)|GiớiTính|Địa chỉ|NgàyCấp(ddMMyyyy)
     */
    private CccdInfoDto parseQr(String qr) {
        String[] p = qr.split("\\|", -1);
        if (p.length < 7) {
            throw new IllegalArgumentException(
                    "Mã QR không đúng định dạng CCCD. Vui lòng chụp lại mặt trước thẻ CCCD gắn chip.");
        }
        CccdInfoDto dto = CccdInfoDto.builder()
                .cccdNumber(trimToNull(p[0]))
                .fullName(trimToNull(p[2]))
                .dateOfBirth(formatQrDate(p[3]))
                .gender(trimToNull(p[4]))
                .permanentAddress(trimToNull(p[5]))
                .issueDate(formatQrDate(p[6]))
                .issuePlace(DEFAULT_ISSUE_PLACE)
                .build();
        return dto;
    }

    /** ddMMyyyy -> dd/MM/yyyy; giữ nguyên nếu không đúng 8 số. */
    private String formatQrDate(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.length() == 8 && s.chars().allMatch(Character::isDigit)) {
            return s.substring(0, 2) + "/" + s.substring(2, 4) + "/" + s.substring(4);
        }
        return trimToNull(s);
    }

    private String trimToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public CccdInfoDto getMine() {
        return getByUserId(authHelper.currentUserId());
    }

    @Override
    @Transactional
    public CccdInfoDto saveMine(CccdInfoDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu CCCD.");
        }
        Long userId = authHelper.currentUserId();
        // Chốt chặn: không cho lưu nếu số CCCD trùng tài khoản khác.
        assertCccdNotUsedByOthers(request.getCccdNumber(), userId);
        // Không lưu cờ 'complete' (chỉ để hiển thị).
        request.setComplete(null);
        String json;
        try {
            json = MAPPER.writeValueAsString(request);
        } catch (Exception e) {
            throw new IllegalArgumentException("Dữ liệu CCCD không hợp lệ.");
        }
        String key = KEY_PREFIX + userId;
        SystemParameter param = systemParameterRepository.findByParamKey(key)
                .orElseGet(SystemParameter::new);
        param.setParamKey(key);
        param.setParamValue(json);
        param.setDescription("Thông tin CCCD của user (cho hợp đồng)");
        systemParameterRepository.save(param);
        // CCCD là nguồn chuẩn: đồng bộ ngày sinh + giới tính CCCD -> hồ sơ (không gõ tay, đáng tin).
        syncProfileFromCccd(userId, request.getDateOfBirth(), request.getGender());
        return withComplete(request);
    }

    /** Ghi đè ngày sinh + giới tính hồ sơ (client/tutor) bằng dữ liệu đọc từ CCCD. */
    private void syncProfileFromCccd(Long userId, String dobStr, String genderStr) {
        final LocalDate dob = parseDob(dobStr);
        final com.tcs.module.profile.enums.Gender gender = mapGender(genderStr);
        if (dob == null && gender == null) {
            return;
        }
        clientRepository.findByUser_UserId(userId).ifPresent(c -> {
            if (dob != null) {
                c.setDateOfBirth(dob);
            }
            if (gender != null) {
                c.setGender(gender);
            }
            clientRepository.save(c);
        });
        tutorRepository.findByUser_UserId(userId).ifPresent(t -> {
            if (dob != null) {
                t.setDateOfBirth(dob);
            }
            if (gender != null) {
                t.setGender(gender);
            }
            tutorRepository.save(t);
        });
    }

    private LocalDate parseDob(String dobStr) {
        if (dobStr == null || dobStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dobStr.trim(), DOB_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    /** "Nam" -> MALE, "Nữ" -> FEMALE, còn lại -> OTHER; rỗng -> null (không đổi). */
    private com.tcs.module.profile.enums.Gender mapGender(String genderStr) {
        if (genderStr == null || genderStr.isBlank()) {
            return null;
        }
        String g = genderStr.trim().toLowerCase(java.util.Locale.ROOT);
        if (g.equals("nam")) {
            return com.tcs.module.profile.enums.Gender.MALE;
        }
        if (g.equals("nữ") || g.equals("nu")) {
            return com.tcs.module.profile.enums.Gender.FEMALE;
        }
        return com.tcs.module.profile.enums.Gender.OTHER;
    }

    @Override
    @Transactional(readOnly = true)
    public CccdInfoDto getByUserId(Long userId) {
        CccdInfoDto dto = systemParameterRepository.findByParamKey(KEY_PREFIX + userId)
                .map(p -> {
                    try {
                        return MAPPER.readValue(p.getParamValue(), CccdInfoDto.class);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .orElse(null);
        return dto == null ? CccdInfoDto.builder().complete(false).build() : withComplete(dto);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isComplete(Long userId) {
        return Boolean.TRUE.equals(getByUserId(userId).getComplete());
    }

    /** Đủ trường bắt buộc để ký hợp đồng (chỉ các trường định danh từ CCCD). */
    private CccdInfoDto withComplete(CccdInfoDto dto) {
        boolean complete = StringUtils.hasText(dto.getFullName())
                && StringUtils.hasText(dto.getCccdNumber())
                && StringUtils.hasText(dto.getDateOfBirth())
                && StringUtils.hasText(dto.getIssueDate())
                && StringUtils.hasText(dto.getIssuePlace())
                && StringUtils.hasText(dto.getPermanentAddress());
        dto.setComplete(complete);
        return dto;
    }
}
