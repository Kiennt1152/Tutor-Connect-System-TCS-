package com.tcs.module.catalog.controller;

import com.tcs.module.catalog.dto.request.UpsertSystemParameterRequest;
import com.tcs.module.catalog.dto.response.SystemParameterResponse;
import com.tcs.module.catalog.service.SystemParameterService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/parameters")
@RequiredArgsConstructor
public class SystemParameterController {

    private final SystemParameterService systemParameterService;

    // =========================================================================
    // LUỒNG 10: CẤU HÌNH THAM SỐ NỀN TẢNG & TỶ LỆ PHÍ ĐỘNG (UC-46)
    // =========================================================================

    // Luồng 10 - Tra cứu danh sách tham số nền tảng
    @GetMapping
    public List<SystemParameterResponse> getParameters(
            @RequestParam(required = false) String prefix,
            @RequestParam(required = false) String keyword
    ) {
        return systemParameterService.getParameters(prefix, keyword);
    }

    @GetMapping("/{parameterId}")
    public SystemParameterResponse getParameter(@PathVariable Long parameterId) {
        return systemParameterService.getParameter(parameterId);
    }

    // Luồng 10 - Tạo mới tham số hệ thống
    @PostMapping
    public SystemParameterResponse createParameter(@Valid @RequestBody UpsertSystemParameterRequest request) {
        return systemParameterService.createParameter(request);
    }

    // Luồng 10 - Bước 1: Tiếp nhận PATCH request cập nhật tham số (như PLATFORM_FEE_RATE)
    @PatchMapping("/{parameterId}")
    public SystemParameterResponse updateParameter(
            @PathVariable Long parameterId,
            @Valid @RequestBody UpsertSystemParameterRequest request
    ) {
        return systemParameterService.updateParameter(parameterId, request);
    }

    // Luồng 10 - Xóa tham số tùy chỉnh (có chặn MANDATORY_KEYS ở Service)
    @DeleteMapping("/{parameterId}")
    public void deleteParameter(@PathVariable Long parameterId) {
        systemParameterService.deleteParameter(parameterId);
    }
}
