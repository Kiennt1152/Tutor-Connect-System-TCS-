package com.tcs.module.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.catalog.dto.request.UpsertSystemParameterRequest;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.platform.service.AuditLogService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
class Report52SystemParameterITTest {

    private static final Long PARAM_ID = 10L;

    @Mock private SystemParameterRepository systemParameterRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private SystemParameterServiceImpl systemParameterService;

    @Test
    @Tag("report52-it")
    void IT_CAT_014_PlatformFeeParameterStoresAcceptedValueForFinance() {
        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.empty());
        when(systemParameterRepository.save(any(SystemParameter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        systemParameterService.createParameter(request("PLATFORM_FEE_RATE", "0.00"));

        ArgumentCaptor<SystemParameter> parameterCaptor = ArgumentCaptor.forClass(SystemParameter.class);
        verify(systemParameterRepository).save(parameterCaptor.capture());
        assertEquals("PLATFORM_FEE_RATE", parameterCaptor.getValue().getParamKey());
        assertEquals("0", parameterCaptor.getValue().getParamValue());
    }

    @Test
    void SUPPORT_SYSTEM_PARAMETER_CreatePlatformFeeRateAtUpperBoundary() {
        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.empty());
        when(systemParameterRepository.save(any(SystemParameter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        systemParameterService.createParameter(request("PLATFORM_FEE_RATE", "0.50"));

        verify(systemParameterRepository).save(any(SystemParameter.class));
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_005_RejectPlatformFeeRateAboveAllowedRange() {
        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemParameterService.createParameter(request("PLATFORM_FEE_RATE", "0.51")));

        assertEquals("PLATFORM_FEE_RATE phải từ 0.00 đến 0.50.", exception.getMessage());
        verify(systemParameterRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_ADM_004_RejectBlankPlatformFeeValueBeforeSavingConfig() {
        UpsertSystemParameterRequest request = request("PLATFORM_FEE_RATE", " ");

        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemParameterService.createParameter(request));

        assertEquals("Giá trị tham số là bắt buộc.", exception.getMessage());
        verify(systemParameterRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    @Tag("report52-it")
    void IT_ADM_008_RejectDuplicatePlatformFeeParameterBeforeCreatingConfigRow() {
        UpsertSystemParameterRequest request = request("PLATFORM_FEE_RATE", "0.02");

        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE"))
                .thenReturn(Optional.of(parameter(1L, "PLATFORM_FEE_RATE", "0.02")));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemParameterService.createParameter(request));

        assertEquals("Khóa tham số đã tồn tại: PLATFORM_FEE_RATE", exception.getMessage());
        verify(systemParameterRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    @Tag("report52-it")
    void IT_ADM_013_UpdatePlatformFeeParameterStoresValidatedValueAndAuditSnapshot() {
        SystemParameter parameter = parameter(PARAM_ID, "PLATFORM_FEE_RATE", "0.02");
        UpsertSystemParameterRequest request = request("PLATFORM_FEE_RATE", "0.05");

        when(systemParameterRepository.findById(PARAM_ID)).thenReturn(Optional.of(parameter));
        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.of(parameter));
        when(systemParameterRepository.save(parameter)).thenReturn(parameter);

        var response = systemParameterService.updateParameter(PARAM_ID, request);

        assertEquals("PLATFORM_FEE_RATE", response.getParamKey());
        assertEquals("0.05", response.getParamValue());
        verify(systemParameterRepository).save(parameter);
        verify(auditLogService).record(eq("UPDATE_SYSTEM_PARAMETER"), eq("SystemParameter"), eq(PARAM_ID), any(), eq(request));
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_017_RejectRenamingMandatoryPlatformFeeKey() {
        when(systemParameterRepository.findById(PARAM_ID))
                .thenReturn(Optional.of(parameter(PARAM_ID, "PLATFORM_FEE_RATE", "0.02")));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemParameterService.updateParameter(PARAM_ID, request("OTHER_KEY", "0.02")));

        assertEquals("Không thể đổi tên khóa cấu hình bắt buộc: PLATFORM_FEE_RATE", exception.getMessage());
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_009_RejectUpdatingToKeyAlreadyUsedByAnotherParameter() {
        when(systemParameterRepository.findById(PARAM_ID))
                .thenReturn(Optional.of(parameter(PARAM_ID, "MY_KEY", "old")));
        when(systemParameterRepository.findByParamKey("TAKEN_KEY"))
                .thenReturn(Optional.of(parameter(99L, "TAKEN_KEY", "value")));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemParameterService.updateParameter(PARAM_ID, request("TAKEN_KEY", "new")));

        assertEquals("Khóa tham số đã tồn tại: TAKEN_KEY", exception.getMessage());
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_018_SearchSystemParametersByPrefixAndKeywordReturnsSortedResult() {
        SystemParameter financeFee = parameter(1L, "finance.platform_fee_percent", "10");
        SystemParameter financeHold = parameter(2L, "finance.escrow_hold_days", "7");
        SystemParameter authOtp = parameter(3L, "auth.otp_minutes", "5");

        when(systemParameterRepository.findByParamKeyStartingWith("finance."))
                .thenReturn(List.of(financeHold, financeFee, authOtp));

        var responses = systemParameterService.getParameters(" finance. ", "fee");

        assertEquals(1, responses.size());
        assertEquals("finance.platform_fee_percent", responses.get(0).getParamKey());
        verify(systemParameterRepository).findByParamKeyStartingWith(eq("finance."));
    }

    private UpsertSystemParameterRequest request(String key, String value) {
        UpsertSystemParameterRequest request = new UpsertSystemParameterRequest();
        request.setParamKey(key);
        request.setParamValue(value);
        request.setDescription("IT config value");
        return request;
    }

    private SystemParameter parameter(Long id, String key, String value) {
        SystemParameter parameter = new SystemParameter();
        parameter.setParameterId(id);
        parameter.setParamKey(key);
        parameter.setParamValue(value);
        return parameter;
    }
}
