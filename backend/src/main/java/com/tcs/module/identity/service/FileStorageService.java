package com.tcs.module.identity.service;

import com.tcs.module.identity.dto.request.FileUploadRequest;
import com.tcs.module.identity.dto.response.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    FileUploadResponse uploadFile(MultipartFile file, Long uploadedBy);

    String getFileUrl(String fileName);
}
