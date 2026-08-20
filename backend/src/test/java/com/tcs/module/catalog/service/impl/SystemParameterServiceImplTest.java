package com.tcs.module.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.catalog.dto.request.UpsertSystemParameterRequest;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.platform.service.AuditLogService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit test module Catalog — tham số hệ thống.
 * Bám bộ test case trong Report_5.1_UnitTest: các sheet createParameter, updateParameter.
 *
 * <p>Chú ý các case BIÊN: PLATFORM_FEE_RATE chỉ hợp lệ trong [0.00 ; 0.50],
 * ESCROW_HOLD_DAYS chỉ hợp lệ trong [1 ; 365].</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SystemParameterServiceImplTest {

    private static final Long PARAM_ID = 10L;

    @Mock private SystemParameterRepository systemParameterRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private SystemParameterServiceImpl service;

    private UpsertSystemParameterRequest req(String key, String value) {
        UpsertSystemParameterRequest r = new UpsertSystemParameterRequest();
        r.setParamKey(key);
        r.setParamValue(value);
        r.setDescription("mo ta");
        return r;
    }

    private SystemParameter existing(String key, String value) {
        SystemParameter p = new SystemParameter();
        p.setParameterId(PARAM_ID);
        p.setParamKey(key);
        p.setParamValue(value);
        return p;
    }

    // ===================================================================
    //  Sheet: createParameter
    // ===================================================================
    @Nested
    @DisplayName("createParameter")
    class CreateParameter {

        @Test
        @DisplayName("UTCID01 (N) - Khóa mới, giá trị hợp lệ -> tạo thành công")
        void utcid01_createSuccessfully() {
            when(systemParameterRepository.findByParamKey("MY_KEY")).thenReturn(Optional.empty());
            when(systemParameterRepository.save(any(SystemParameter.class))).thenAnswer(i -> {
                SystemParameter p = i.getArgument(0);
                p.setParameterId(PARAM_ID);
                return p;
            });

            service.createParameter(req("MY_KEY", "abc"));

            verify(systemParameterRepository).save(any(SystemParameter.class));
        }

        @Test
        @DisplayName("UTCID02 (A) - Khóa đã tồn tại -> 'Khóa tham số đã tồn tại: ...'")
        void utcid02_duplicateKey() {
            when(systemParameterRepository.findByParamKey("MY_KEY"))
                    .thenReturn(Optional.of(existing("MY_KEY", "abc")));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createParameter(req("MY_KEY", "xyz")));
            assertEquals("Khóa tham số đã tồn tại: MY_KEY", ex.getMessage());
            verify(systemParameterRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Thiếu khóa -> 'Khóa tham số là bắt buộc.'")
        void utcid03_missingKey() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createParameter(req(null, "abc")));
            assertEquals("Khóa tham số là bắt buộc.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Thiếu giá trị -> 'Giá trị tham số là bắt buộc.'")
        void utcid04_missingValue() {
            when(systemParameterRepository.findByParamKey("MY_KEY")).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createParameter(req("MY_KEY", "  ")));
            assertEquals("Giá trị tham số là bắt buộc.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (B) - PLATFORM_FEE_RATE = 0.00 (cận dưới) -> hợp lệ")
        void utcid05_feeRateLowerBound() {
            when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.empty());
            when(systemParameterRepository.save(any(SystemParameter.class))).thenAnswer(i -> i.getArgument(0));

            service.createParameter(req("PLATFORM_FEE_RATE", "0.00"));

            verify(systemParameterRepository).save(any(SystemParameter.class));
        }

        @Test
        @DisplayName("UTCID06 (B) - PLATFORM_FEE_RATE = 0.50 (cận trên) -> hợp lệ")
        void utcid06_feeRateUpperBound() {
            when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.empty());
            when(systemParameterRepository.save(any(SystemParameter.class))).thenAnswer(i -> i.getArgument(0));

            service.createParameter(req("PLATFORM_FEE_RATE", "0.50"));

            verify(systemParameterRepository).save(any(SystemParameter.class));
        }

        @Test
        @DisplayName("UTCID07 (B) - PLATFORM_FEE_RATE = 0.51 (vượt cận trên) -> chặn")
        void utcid07_feeRateAboveUpperBound() {
            when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createParameter(req("PLATFORM_FEE_RATE", "0.51")));
            assertEquals("PLATFORM_FEE_RATE phải từ 0.00 đến 0.50.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (B) - PLATFORM_FEE_RATE âm -> chặn")
        void utcid08_feeRateNegative() {
            when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.createParameter(req("PLATFORM_FEE_RATE", "-0.01")));
        }

        @Test
        @DisplayName("UTCID09 (B) - ESCROW_HOLD_DAYS = 1 (cận dưới) -> hợp lệ")
        void utcid09_holdDaysLowerBound() {
            when(systemParameterRepository.findByParamKey("ESCROW_HOLD_DAYS")).thenReturn(Optional.empty());
            when(systemParameterRepository.save(any(SystemParameter.class))).thenAnswer(i -> i.getArgument(0));

            service.createParameter(req("ESCROW_HOLD_DAYS", "1"));

            verify(systemParameterRepository).save(any(SystemParameter.class));
        }

        @Test
        @DisplayName("UTCID10 (B) - ESCROW_HOLD_DAYS = 366 (vượt cận trên) -> chặn")
        void utcid10_holdDaysAboveUpperBound() {
            when(systemParameterRepository.findByParamKey("ESCROW_HOLD_DAYS")).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createParameter(req("ESCROW_HOLD_DAYS", "366")));
            assertEquals("ESCROW_HOLD_DAYS phải từ 1 đến 365.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID11 (B) - ESCROW_HOLD_DAYS = 0 (dưới cận dưới) -> chặn")
        void utcid11_holdDaysZero() {
            when(systemParameterRepository.findByParamKey("ESCROW_HOLD_DAYS")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.createParameter(req("ESCROW_HOLD_DAYS", "0")));
        }
    }

    // ===================================================================
    //  Sheet: updateParameter
    // ===================================================================
    @Nested
    @DisplayName("updateParameter")
    class UpdateParameter {

        @Test
        @DisplayName("UTCID01 (N) - Cập nhật giá trị tham số thường -> thành công")
        void utcid01_updateSuccessfully() {
            when(systemParameterRepository.findById(PARAM_ID))
                    .thenReturn(Optional.of(existing("MY_KEY", "old")));
            when(systemParameterRepository.findByParamKey("MY_KEY"))
                    .thenReturn(Optional.of(existing("MY_KEY", "old")));
            when(systemParameterRepository.save(any(SystemParameter.class))).thenAnswer(i -> i.getArgument(0));

            service.updateParameter(PARAM_ID, req("MY_KEY", "new"));

            verify(systemParameterRepository).save(any(SystemParameter.class));
        }

        @Test
        @DisplayName("UTCID02 (A) - Đổi tên khóa cấu hình BẮT BUỘC -> chặn")
        void utcid02_renameMandatoryKey() {
            when(systemParameterRepository.findById(PARAM_ID))
                    .thenReturn(Optional.of(existing("PLATFORM_FEE_RATE", "0.02")));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateParameter(PARAM_ID, req("OTHER_KEY", "0.02")));
            assertEquals("Không thể đổi tên khóa cấu hình bắt buộc: PLATFORM_FEE_RATE", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Đổi sang khóa đã thuộc tham số khác -> chặn trùng khóa")
        void utcid03_keyTakenByAnother() {
            SystemParameter another = existing("TAKEN_KEY", "v");
            another.setParameterId(99L);
            when(systemParameterRepository.findById(PARAM_ID))
                    .thenReturn(Optional.of(existing("MY_KEY", "old")));
            when(systemParameterRepository.findByParamKey("TAKEN_KEY")).thenReturn(Optional.of(another));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateParameter(PARAM_ID, req("TAKEN_KEY", "v2")));
            assertEquals("Khóa tham số đã tồn tại: TAKEN_KEY", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (B) - Sửa PLATFORM_FEE_RATE thành 0.60 -> chặn theo miền giá trị")
        void utcid04_updateFeeRateOutOfRange() {
            when(systemParameterRepository.findById(PARAM_ID))
                    .thenReturn(Optional.of(existing("PLATFORM_FEE_RATE", "0.02")));
            when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE"))
                    .thenReturn(Optional.of(existing("PLATFORM_FEE_RATE", "0.02")));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateParameter(PARAM_ID, req("PLATFORM_FEE_RATE", "0.60")));
            assertEquals("PLATFORM_FEE_RATE phải từ 0.00 đến 0.50.", ex.getMessage());
        }
    }
}
