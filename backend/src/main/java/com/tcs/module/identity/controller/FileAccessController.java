package com.tcs.module.identity.controller;

import com.tcs.module.identity.enums.VerificationDocumentType;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.profile.entity.MediaFile;
import com.tcs.module.profile.repository.MediaFileRepository;
import com.tcs.security.AuthHelper;
import com.tcs.module.finance.service.DisputeService;
import com.tcs.exception.ForbiddenException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * ============================================================================
 * [UC-04] PHÂN PHỐI & KIỂM SOÁT BẢO MẬT TỆP TIN TẢI LÊN (FILE ACCESS CONTROLLER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Đồng tác giả: tienanh6677 (Nguyễn Tiến Anh)
 * Ngày tạo: 2026-08-11
 * 
 * Mô tả Use Case:
 *   - Kiểm soát truy cập bảo mật đối với các tệp tin và tài liệu nhạy cảm được tải lên hệ thống.
 *   - Chống tấn công khai thác trực tiếp tệp tin (IDOR) cho các hình ảnh CCCD, chứng chỉ và bằng chứng tranh chấp.
 * 
 * Chức năng chính:
 *   1. Bảo vệ tài liệu định danh: Chỉ cho phép chủ sở hữu tệp tin hoặc Quản trị viên sàn truy cập.
 *   2. Thẩm định quyền liên quan: Cho phép Trung tâm gia sư xem xét chứng cứ khi có tranh chấp liên quan đến lớp của mình.
 *   3. Phân phối an toàn (Secure Streaming): Trả về luồng tài nguyên kèm định dạng MIME tương ứng.
 *   4. Bảo vệ đường dẫn: Ngăn chặn duyệt thư mục trái phép (Path Traversal Protection).
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Yêu cầu tải hoặc xem tệp gửi tới `/api/files/private/{fileId}`.
 *   - Bước 2: Kiểm tra phiên đăng nhập và định danh người dùng qua `AuthHelper`.
 *   - Bước 3: Xác minh quyền sở hữu tệp tin (chủ sở hữu, Platform Admin hoặc bên liên quan).
 *   - Bước 4: Tải file từ đường dẫn lưu trữ và stream trả về cho trình duyệt.
 * ============================================================================
 */
@RestController
@RequiredArgsConstructor
public class FileAccessController {

    private final MediaFileRepository mediaFileRepo;
    private final VerificationDocumentRepository verificationDocumentRepo;
    private final ReportRepository reportRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final AuthHelper authHelper;
    private final DisputeService disputeService;

    @Value("${tcs.file.storage.path:uploads}")
    private String storagePath;

    @GetMapping("/api/files/private/{fileId}")
    public ResponseEntity<Resource> getPrivateFile(@PathVariable Long fileId) {
        MediaFile file = mediaFileRepo.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        authorizePrivateFile(file);
        return serve(file);
    }

    @GetMapping("/api/files/private/by-url")
    public ResponseEntity<Resource> getPrivateFileByUrl(@RequestParam("url") String fileUrl) {
        String normalizedUrl = normalizePrivateFileUrl(fileUrl);
        MediaFile file = mediaFileRepo.findFirstByFileUrl(normalizedUrl)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        authorizePrivateFile(file);
        return serve(file);
    }

    /**
     * Phục vụ file chứng chỉ (bằng cấp) của gia sư cho bất kỳ người dùng đã đăng nhập —
     * để trung tâm/phụ huynh xem khi đánh giá gia sư ứng tuyển. An toàn vì chỉ phục vụ đúng
     * file là tài liệu loại CERTIFICATE thuộc hồ sơ đã VERIFIED; CCCD và tài liệu chưa duyệt
     * KHÔNG khớp điều kiện nên không thể lấy qua endpoint này.
     */
    @GetMapping("/api/files/certificate/{fileId}")
    public ResponseEntity<Resource> getCertificateFile(@PathVariable Long fileId) {
        authHelper.currentUserId(); // yêu cầu đã đăng nhập

        boolean isVerifiedCertificate = verificationDocumentRepo
                .existsByFile_FileIdAndDocumentTypeAndVerificationRequest_Status(
                        fileId, VerificationDocumentType.CERTIFICATE, VerificationStatus.VERIFIED);
        if (!isVerifiedCertificate) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a shareable certificate");
        }

        MediaFile file = mediaFileRepo.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        return serve(file);
    }

    private ResponseEntity<Resource> serve(MediaFile file) {
        try {
            // Extract stored filename from the fileUrl (e.g. "/uploads/private/uuid.pdf" → "private/uuid.pdf")
            String storedPath = file.getFileUrl().replaceFirst("^/uploads/", "");
            Path filePath = Paths.get(storagePath).toAbsolutePath().normalize().resolve(storedPath);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found on disk");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.getMimeType()))
                    .body(resource);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file");
        }
    }

    private void authorizePrivateFile(MediaFile file) {
        Long userId = authHelper.currentUserId();

        boolean isOwner = file.getUploadedBy() != null
                && file.getUploadedBy().getUserId().equals(userId);
        boolean isAdmin = authHelper.hasRole("PLATFORM_ADMIN");
        boolean isCenterOwner = !isOwner
                && !isAdmin
                && isCenterOwnerOfReportedClass(file, userId);

        boolean isDisputeParticipant = !isOwner && !isAdmin && !isCenterOwner
                && disputeService.canReadDisputeEvidence(file.getFileUrl(), userId);
        if (!isOwner && !isAdmin && !isCenterOwner && !isDisputeParticipant) {
            throw new ForbiddenException("Bạn không có quyền xem tệp này");
        }
    }

    /**
     * Center chỉ được xem bằng chứng gắn với báo cáo sự cố/tranh chấp của
     * chính lớp CENTER do center đó quản lý. Không mở quyền đọc toàn bộ
     * private files của người dùng khác.
     */
    private boolean isCenterOwnerOfReportedClass(MediaFile file, Long centerUserId) {
        if (file == null
                || centerUserId == null
                || !authHelper.hasRole("TUTOR_CENTER")
                || file.getFileUrl() == null
                || file.getFileUrl().isBlank()) {
            return false;
        }

        return reportRepository
                .findByTargetTypeAndEvidenceUrlsContaining(
                        ReportTargetType.CLASS, file.getFileUrl())
                .stream()
                .map(Report::getTargetId)
                .filter(targetId -> targetId != null)
                .anyMatch(classId -> tutoringClassRepository.existsCenterOwnedClass(
                        classId, ClassType.CENTER, centerUserId));
    }

    private String normalizePrivateFileUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing file URL");
        }

        String url = rawUrl.trim();
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                url = URI.create(url).getPath();
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file URL");
        }

        int queryIndex = url.indexOf('?');
        if (queryIndex >= 0) {
            url = url.substring(0, queryIndex);
        }

        if (!url.startsWith("/uploads/private/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only private upload URLs are supported");
        }

        return url;
    }
}
