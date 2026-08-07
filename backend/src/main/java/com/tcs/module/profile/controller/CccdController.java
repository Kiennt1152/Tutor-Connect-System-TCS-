package com.tcs.module.profile.controller;

import com.tcs.module.profile.dto.CccdInfoDto;
import com.tcs.module.profile.service.CccdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Thông tin CCCD của người dùng (đọc QR tự điền + xác nhận) — dùng cho khối BÊN B hợp đồng. */
@RestController
@RequestMapping("/api/profile/cccd")
@RequiredArgsConstructor
public class CccdController {

    private final CccdService cccdService;

    /** Đọc mã QR trên ảnh CCCD, trả về thông tin đã tách (chưa lưu). */
    @PostMapping(value = "/scan", consumes = "multipart/form-data")
    public CccdInfoDto scan(@RequestParam("file") MultipartFile file) {
        return cccdService.scanFromImage(file);
    }

    @GetMapping
    public CccdInfoDto getMine() {
        return cccdService.getMine();
    }

    @PutMapping
    public CccdInfoDto saveMine(@RequestBody CccdInfoDto request) {
        return cccdService.saveMine(request);
    }
}
