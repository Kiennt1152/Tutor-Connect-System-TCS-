package com.tcs.module.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.identity.dto.response.FileUploadResponse;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.profile.entity.MediaFile;
import com.tcs.module.profile.repository.MediaFileRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * Unit test cho {@link FileStorageServiceImpl}.
 *
 * <p>Bam bo test case trong Report_5.1_UnitTest: sheet uploadFile va sheet getFileUrl.</p>
 *
 * <p>MIME duoc nhan dang bang magic bytes (khong tin Content-Type client gui), nen du lieu
 * test phai dung chu ky that: PNG = 89 50 4E 47 0D 0A 1A 0A, GIF = 47 49 46 38.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileStorageServiceImplTest {

    private static final Long USER_ID = 1L;

    private static final byte[] PNG_HEADER =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, (byte) 0x1A, 0x0A, 1, 2, 3, 4};
    private static final byte[] GIF_HEADER = {0x47, 0x49, 0x46, 0x38, 1, 2, 3, 4};

    @Mock private MediaFileRepository mediaFileRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private FileStorageServiceImpl fileStorageService;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileStorageService, "storagePath", tempDir.toString());
        fileStorageService.init();
    }

    private MockMultipartFile pngFile() {
        return new MockMultipartFile("file", "anh.png", "image/png", PNG_HEADER);
    }

    private void givenUserExists() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new User()));
        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ===================================================================
    //  Sheet: uploadFile
    // ===================================================================
    @Nested
    @DisplayName("uploadFile")
    class UploadFile {

        @Test
        @DisplayName("UTCID01 (N) - file PNG hop le duoi 10MB -> luu file va tao ban ghi MediaFile")
        void utcid01_uploadSuccessfully() {
            givenUserExists();

            FileUploadResponse res = fileStorageService.uploadFile(pngFile(), USER_ID);

            assertEquals("anh.png", res.getFileName());
            assertEquals("image/png", res.getMimeType());
            assertTrue(res.getFileUrl().startsWith("/uploads/private/"),
                    "file khong cong khai phai nam trong thu muc private");
            verify(mediaFileRepository).save(any(MediaFile.class));
        }

        @Test
        @DisplayName("UTCID02 (A) - file rong hoac null -> 'File is empty'")
        void utcid02_emptyFile() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> fileStorageService.uploadFile(
                            new MockMultipartFile("file", "rong.png", "image/png", new byte[0]), USER_ID));
            assertEquals("File is empty", ex.getMessage());

            assertThrows(IllegalArgumentException.class,
                    () -> fileStorageService.uploadFile(null, USER_ID));
            verify(mediaFileRepository, never()).save(any(MediaFile.class));
        }

        @Test
        @DisplayName("UTCID03 (B) - kich thuoc vuot 10MB dung 1 byte -> 'File size exceeds 10MB limit'")
        void utcid03_fileTooLarge() throws IOException {
            MultipartFile big = org.mockito.Mockito.mock(MultipartFile.class);
            when(big.isEmpty()).thenReturn(false);
            when(big.getSize()).thenReturn(10L * 1024 * 1024 + 1);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> fileStorageService.uploadFile(big, USER_ID));
            assertEquals("File size exceeds 10MB limit", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - dinh dang khong duoc phep (GIF) -> 'File type not allowed. Allowed: PDF, JPEG, PNG, WEBP'")
        void utcid04_typeNotAllowed() {
            MockMultipartFile gif = new MockMultipartFile("file", "anh.gif", "image/gif", GIF_HEADER);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> fileStorageService.uploadFile(gif, USER_ID));
            assertEquals("File type not allowed. Allowed: PDF, JPEG, PNG, WEBP", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - khong doc duoc file de nhan dang -> RuntimeException 'Failed to read file for type detection'")
        void utcid05_cannotReadForDetection() throws IOException {
            MultipartFile broken = org.mockito.Mockito.mock(MultipartFile.class);
            when(broken.isEmpty()).thenReturn(false);
            when(broken.getSize()).thenReturn(100L);
            when(broken.getInputStream()).thenThrow(new IOException("stream loi"));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> fileStorageService.uploadFile(broken, USER_ID));
            assertEquals("Failed to read file for type detection", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - loi ghi file xuong dia -> RuntimeException 'Failed to store file'")
        void utcid06_cannotWriteToDisk() throws IOException {
            // Xoa thu muc private de Files.copy that bai
            Files.delete(tempDir.resolve("private"));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> fileStorageService.uploadFile(pngFile(), USER_ID));
            assertEquals("Failed to store file", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - uploadedBy khong khop nguoi dung nao -> 'User not found: <id>'")
        void utcid07_userNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> fileStorageService.uploadFile(pngFile(), USER_ID));
            assertEquals("User not found: " + USER_ID, ex.getMessage());
            verify(mediaFileRepository, never()).save(any(MediaFile.class));
        }
    }

    // ===================================================================
    //  Sheet: getFileUrl
    //  Ham chi noi chuoi, khong kiem tra dau vao va khong nem ngoai le.
    // ===================================================================
    @Nested
    @DisplayName("getFileUrl")
    class GetFileUrl {

        @Test
        @DisplayName("UTCID01 (N) - ten file thong thuong -> '/uploads/public/' + fileName")
        void utcid01_ordinaryFileName() {
            assertEquals("/uploads/public/abc.pdf", fileStorageService.getFileUrl("abc.pdf"));
        }

        @Test
        @DisplayName("UTCID02 (B) - fileName rong -> chi con tien to '/uploads/public/'")
        void utcid02_emptyFileName() {
            assertEquals("/uploads/public/", fileStorageService.getFileUrl(""));
        }

        @Test
        @DisplayName("UTCID03 (B) - fileName = null -> '/uploads/public/null', khong nem ngoai le")
        void utcid03_nullFileName() {
            assertEquals("/uploads/public/null", fileStorageService.getFileUrl(null));
        }

        @Test
        @DisplayName("UTCID04 (B) - fileName da co dau gach dau -> khong duoc chuan hoa, sinh hai dau gach")
        void utcid04_leadingSlashNotNormalised() {
            assertEquals("/uploads/public//abc.pdf", fileStorageService.getFileUrl("/abc.pdf"));
        }
    }
}
