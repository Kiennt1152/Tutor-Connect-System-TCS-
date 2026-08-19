package com.tcs.module.identity.controller;

import com.tcs.module.identity.enums.VerificationDocumentType;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.profile.entity.MediaFile;
import com.tcs.module.profile.repository.MediaFileRepository;
import com.tcs.security.AuthHelper;
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
 * Serves sensitive uploaded files (CCCD, business licenses, verification documents)
 * through an authenticated endpoint with owner + admin access control.
 *
 * <p>Public files (avatars) are still served directly via /uploads/public/** (permitAll).
 * Private files are accessed via /api/files/private/{fileId} which checks that the
 * requesting user is either the file owner or a PLATFORM_ADMIN.</p>
 */
@RestController
@RequiredArgsConstructor
public class FileAccessController {

    private final MediaFileRepository mediaFileRepo;
    private final VerificationDocumentRepository verificationDocumentRepo;
    private final AuthHelper authHelper;

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

        // Only the file owner or a platform admin may access private files.
        boolean isOwner = file.getUploadedBy() != null
                && file.getUploadedBy().getUserId().equals(userId);
        boolean isAdmin = authHelper.hasRole("PLATFORM_ADMIN");

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
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
