package com.tcs.module.profile.service;

import com.tcs.module.profile.dto.CccdInfoDto;
import org.springframework.web.multipart.MultipartFile;

/** Đọc QR ảnh CCCD tự điền + lưu/đọc thông tin CCCD của người dùng (cho khối BÊN B hợp đồng). */
public interface CccdService {

    /** Đọc mã QR trên ảnh CCCD, trả về thông tin đã tách (CHƯA lưu — để user xác nhận). */
    CccdInfoDto scanFromImage(MultipartFile file);

    /** Thông tin CCCD đã lưu của người dùng đang đăng nhập. */
    CccdInfoDto getMine();

    /** Lưu thông tin CCCD (sau khi user xác nhận). */
    CccdInfoDto saveMine(CccdInfoDto request);

    /** Thông tin CCCD của một user cụ thể (module hợp đồng dùng để dựng BÊN B). */
    CccdInfoDto getByUserId(Long userId);

    /** Đã đủ thông tin bắt buộc để ký hợp đồng chưa. */
    boolean isComplete(Long userId);
}
