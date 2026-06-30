package com.tcs.module.identity.service.impl;

import com.tcs.module.identity.dto.request.FileUploadRequest;
import com.tcs.module.identity.dto.response.FileUploadResponse;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.service.FileStorageService;
import com.tcs.module.profile.entity.MediaFile;
import com.tcs.module.profile.repository.MediaFileRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final MediaFileRepository mediaFileRepository;

    @Value("${tcs.file.storage.path:uploads}")
    private String storagePath;

    private Path storageLocation;

    @PostConstruct
    public void init() {
        this.storageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    @Override
    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file, Long uploadedBy) {
        validateFile(file);

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        String storedName = UUID.randomUUID() + extension;

        try {
            Path targetLocation = storageLocation.resolve(storedName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }

        User user = new User();
        user.setUserId(uploadedBy);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileName(originalName);
        mediaFile.setFileUrl("/uploads/" + storedName);
        mediaFile.setMimeType(file.getContentType());
        mediaFile.setFileSize(file.getSize());
        mediaFile.setUploadedBy(user);

        MediaFile saved = mediaFileRepository.save(mediaFile);

        return FileUploadResponse.builder()
                .fileId(saved.getFileId())
                .fileName(saved.getFileName())
                .fileUrl(saved.getFileUrl())
                .mimeType(saved.getMimeType())
                .fileSize(saved.getFileSize())
                .build();
    }

    @Override
    public String getFileUrl(String fileName) {
        return "/uploads/" + fileName;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("File type not allowed. Allowed: PDF, JPEG, PNG, WEBP");
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
