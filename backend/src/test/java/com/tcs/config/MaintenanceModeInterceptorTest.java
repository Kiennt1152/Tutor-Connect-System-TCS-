package com.tcs.config;

import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.security.AuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceModeInterceptorTest {

    @Mock
    private SystemParameterRepository systemParameterRepository;

    @Mock
    private AuthHelper authHelper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private MaintenanceModeInterceptor interceptor;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        responseWriter = new StringWriter();
    }

    @Test
    @DisplayName("preHandle: cho phép phương thức GET ngay cả khi đang bảo trì (Read-only mode)")
    void preHandle_AllowsGetDuringMaintenance() throws Exception {
        when(request.getMethod()).thenReturn("GET");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(systemParameterRepository, never()).findByParamKey(any());
    }

    @Test
    @DisplayName("preHandle: cho phép request khi MAINTENANCE_MODE = false")
    void preHandle_AllowsMutationsWhenMaintenanceIsFalse() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/marketplace/classes");
        when(systemParameterRepository.findByParamKey("MAINTENANCE_MODE")).thenReturn(Optional.empty());

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    @DisplayName("preHandle: cho phép Admin thực hiện POST khi đang bảo trì")
    void preHandle_AllowsAdminDuringMaintenance() throws Exception {
        SystemParameter param = new SystemParameter();
        param.setParamKey("MAINTENANCE_MODE");
        param.setParamValue("true");

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/marketplace/classes");
        when(systemParameterRepository.findByParamKey("MAINTENANCE_MODE")).thenReturn(Optional.of(param));
        when(authHelper.hasRole("PLATFORM_ADMIN")).thenReturn(true);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    @DisplayName("preHandle: chặn request POST của người dùng thường khi đang bảo trì (503 Service Unavailable)")
    void preHandle_BlocksUserMutationsDuringMaintenance() throws Exception {
        SystemParameter param = new SystemParameter();
        param.setParamKey("MAINTENANCE_MODE");
        param.setParamValue("true");

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/marketplace/classes");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(systemParameterRepository.findByParamKey("MAINTENANCE_MODE")).thenReturn(Optional.of(param));
        when(authHelper.hasRole("PLATFORM_ADMIN")).thenReturn(false);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(503);
        assertTrue(responseWriter.toString().contains("SERVICE_UNAVAILABLE"));
    }
}
