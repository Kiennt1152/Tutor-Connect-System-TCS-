package com.tcs.module.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.identity.dto.response.FileUploadResponse;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.profile.entity.MediaFile;
import com.tcs.module.profile.repository.MediaFileRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * System Test: ST-PROF-004
 * Title: Upload validation - invalid file type and oversized file.
 *
 * Steps:
 * 1. Upload a .exe file.
 * 2. Upload a file >10MB.
 * 3. Upload a valid PDF / JPEG document.
 *
 * Expected:
 * - .exe is rejected with "File type not allowed. Allowed: PDF, JPEG, PNG, WEBP".
 * - File >10MB is rejected with "File size exceeds 10MB limit".
 * - Valid file is stored and media metadata is returned.
 */
@Tag("system-test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ST_PROF_004_FileUploadValidationTest {

    private static final Long USER_ID = 29L;

    @Mock private MediaFileRepository mediaFileRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    @TempDir
    Path tempDir;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(USER_ID);
        user.setEmail("tutor02@tcs.test");

        ReflectionTestUtils.setField(fileStorageService, "storagePath", tempDir.toString());
        fileStorageService.init();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("ST-PROF-004: Uploading a .exe file (executable magic bytes MZ) -> Throws IllegalArgumentException")
    void testUploadExecutableFile_ThrowsIllegalArgumentException() {
        // Windows PE executable header starts with 'M' 'Z' (0x4D, 0x5A)
        byte[] exeBytes = new byte[] {0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};
        MockMultipartFile exeFile = new MockMultipartFile(
                "file", "virus.exe", "application/x-msdownload", exeBytes);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.uploadFile(exeFile, USER_ID));

        assertEquals("File type not allowed. Allowed: PDF, JPEG, PNG, WEBP", ex.getMessage());
    }

    @Test
    @DisplayName("ST-PROF-004: Uploading an executable renamed to .pdf (content sniffing) -> Throws IllegalArgumentException")
    void testUploadDisguisedExecutableFile_ThrowsIllegalArgumentException() {
        // Renamed to degree.pdf, but content is text or executable
        byte[] fakePdfBytes = "This is not a real PDF file".getBytes();
        MockMultipartFile fakeFile = new MockMultipartFile(
                "file", "degree.pdf", "application/pdf", fakePdfBytes);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.uploadFile(fakeFile, USER_ID));

        assertEquals("File type not allowed. Allowed: PDF, JPEG, PNG, WEBP", ex.getMessage());
    }

    @Test
    @DisplayName("ST-PROF-004: Uploading a file >10MB -> Throws IllegalArgumentException")
    void testUploadOversizedFile_ThrowsIllegalArgumentException() {
        // 10MB + 1 byte
        int oversizedLength = 10 * 1024 * 1024 + 1;
        // MockMultipartFile size is length of byte array
        byte[] oversizedBytes = new byte[oversizedLength];
        // Even if header is valid PDF (%PDF-1.4)
        oversizedBytes[0] = 0x25;
        oversizedBytes[1] = 0x50;
        oversizedBytes[2] = 0x44;
        oversizedBytes[3] = 0x46;

        MockMultipartFile oversizedFile = new MockMultipartFile(
                "file", "large_file.pdf", "application/pdf", oversizedBytes);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.uploadFile(oversizedFile, USER_ID));

        assertEquals("File size exceeds 10MB limit", ex.getMessage());
    }

    @Test
    @DisplayName("ST-PROF-004: Uploading an empty file -> Throws IllegalArgumentException")
    void testUploadEmptyFile_ThrowsIllegalArgumentException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.uploadFile(emptyFile, USER_ID));

        assertEquals("File is empty", ex.getMessage());
    }

    @Test
    @DisplayName("ST-PROF-004: Uploading a valid PDF file under 10MB -> Successfully saved and metadata returned")
    void testUploadValidPdfFile_Success() throws IOException {
        // Valid PDF header: %PDF
        byte[] validPdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A};
        MockMultipartFile validPdf = new MockMultipartFile(
                "file", "degree.pdf", "application/pdf", validPdfBytes);

        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile mf = invocation.getArgument(0);
            mf.setFileId(888L);
            return mf;
        });

        FileUploadResponse response = fileStorageService.uploadFile(validPdf, USER_ID, false);

        assertNotNull(response);
        assertEquals(888L, response.getFileId());
        assertEquals("degree.pdf", response.getFileName());
        assertEquals("application/pdf", response.getMimeType());
        verify(mediaFileRepository).save(any(MediaFile.class));
    }

    @Test
    @DisplayName("ST-PROF-004: Uploading a valid JPEG image -> Successfully saved and metadata returned")
    void testUploadValidJpegFile_Success() {
        // Valid JPEG header: FF D8 FF
        byte[] validJpegBytes = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        MockMultipartFile validJpeg = new MockMultipartFile(
                "file", "id.jpg", "image/jpeg", validJpegBytes);

        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile mf = invocation.getArgument(0);
            mf.setFileId(889L);
            return mf;
        });

        FileUploadResponse response = fileStorageService.uploadFile(validJpeg, USER_ID, false);

        assertNotNull(response);
        assertEquals(889L, response.getFileId());
        assertEquals("id.jpg", response.getFileName());
        assertEquals("image/jpeg", response.getMimeType());
        verify(mediaFileRepository).save(any(MediaFile.class));
    }
}
