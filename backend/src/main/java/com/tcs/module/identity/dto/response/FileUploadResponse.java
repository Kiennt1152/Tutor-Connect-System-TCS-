package com.tcs.module.identity.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FileUploadResponse {

    private Long fileId;
    private String fileName;
    private String fileUrl;
    private String mimeType;
    private Long fileSize;
}
