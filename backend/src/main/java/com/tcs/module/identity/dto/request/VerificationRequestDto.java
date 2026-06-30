package com.tcs.module.identity.dto.request;

import com.tcs.module.identity.enums.VerificationDocumentType;
import com.tcs.module.identity.enums.VerificationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VerificationRequestDto {

    @NotNull(message = "Verification type is required")
    private VerificationType verificationType;

    @NotEmpty(message = "At least one document is required")
    private List<DocumentUpload> documents;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DocumentUpload {

        @NotNull(message = "Document type is required")
        private VerificationDocumentType documentType;

        private Long fileId;
    }
}
