package com.tcs.module.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FileUploadRequest {

    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name must be at most 255 characters")
    private String fileName;

    @NotBlank(message = "File URL is required")
    @Size(max = 500, message = "File URL must be at most 500 characters")
    private String fileUrl;

    @Size(max = 100, message = "MIME type must be at most 100 characters")
    private String mimeType;

    private Long fileSize;
}
