package com.tcs.module.identity.controller;

import com.tcs.module.profile.entity.MediaFile;
import com.tcs.module.profile.repository.MediaFileRepository;
import com.tcs.security.AuthHelper;
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
    private final AuthHelper authHelper;

    @Value("${tcs.file.storage.path:uploads}")
    private String storagePath;

    @GetMapping("/api/files/private/{fileId}")
    public ResponseEntity<Resource> getPrivateFile(@PathVariable Long fileId) {
        Long userId = authHelper.currentUserId();

        MediaFile file = mediaFileRepo.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        // Only the file owner or a platform admin may access private files.
        boolean isOwner = file.getUploadedBy() != null
                && file.getUploadedBy().getUserId().equals(userId);
        boolean isAdmin = authHelper.hasRole("PLATFORM_ADMIN");

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

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
}
