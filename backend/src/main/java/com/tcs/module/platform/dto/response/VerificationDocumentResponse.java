package com.tcs.module.platform.dto.response;

import com.tcs.module.identity.enums.VerificationDocumentType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerificationDocumentResponse {

    private Long documentId;
    private VerificationDocumentType documentType;
    private Long fileId;
    private String fileName;
    private String fileUrl;
    private String mimeType;
    /** false neu file_id/file_url bi thieu hoac hong (AF-03). */
    private boolean available;
}
