package com.tcs.module.center.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.ForbiddenException;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.center.dto.request.SaveContractTemplateRequest;
import com.tcs.module.center.dto.response.ContractTemplateResponse;
import com.tcs.module.contract.entity.ContractTemplate;
import com.tcs.module.contract.enums.ContractTemplateStatus;
import com.tcs.module.contract.repository.ContractTemplateRepository;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.WalletStatus;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.security.AuthHelper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * System Test ST-ECO-004: Admin manages contract templates.
 * Procedure:
 * 1. Admin creates/edits a contract template.
 * 2. Create a new contract using that template.
 * Expected:
 * Template saved correctly; the new contract applies the chosen template's content.
 *
 * Finding / Role Mismatch:
 * - The contract template management feature is implemented exclusively for TUTOR_CENTER role
 *   via `/api/center/contract-templates` and CenterContractTemplatesPage.tsx.
 * - PLATFORM_ADMIN has NO API or UI to manage contract templates (system default templates are seeded in DB).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ST_ECO_004_ContractTemplateManagementTest {

    private static final Long CENTER_USER_ID = 100L;
    private static final Long ADMIN_USER_ID = 1L;
    private static final Long TEMPLATE_ID = 50L;

    @Mock private AuthHelper authHelper;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private ContractTemplateRepository contractTemplateRepository;
    @Mock private SystemParameterRepository systemParameterRepository;
    @Mock private WalletRepository walletRepository;

    @InjectMocks private CenterServiceImpl centerService;

    private TutorCenter center;
    private User centerUser;
    private ContractTemplate template;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        centerUser = new User();
        centerUser.setUserId(CENTER_USER_ID);
        centerUser.setEmail("center01@tcs.test");

        center = new TutorCenter();
        center.setCenterId(10L);
        center.setUser(centerUser);
        center.setVerificationStatus(ProfileVerificationStatus.VERIFIED);
        center.setCompanyName("Trung tâm Gia sư Sao Mai");

        wallet = new Wallet();
        wallet.setStatus(WalletStatus.ACTIVE);

        template = new ContractTemplate();
        template.setTemplateId(TEMPLATE_ID);
        template.setName("Tutoring Contract v1");
        template.setContent("Dieu khoan cu");
        template.setCenter(center);
        template.setCreatedBy(centerUser);
        template.setStatus(ContractTemplateStatus.ACTIVE);

        when(walletRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(wallet));
    }

    @Test
    @DisplayName("ST-ECO-004: Vai trò Trung tâm tạo mẫu hợp đồng mới thành công")
    void testCenterCreatesContractTemplate() {
        when(authHelper.currentUserId()).thenReturn(CENTER_USER_ID);
        when(tutorCenterRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(center));
        when(contractTemplateRepository.save(any(ContractTemplate.class))).thenAnswer(inv -> {
            ContractTemplate t = inv.getArgument(0);
            t.setTemplateId(TEMPLATE_ID);
            return t;
        });

        SaveContractTemplateRequest request = new SaveContractTemplateRequest();
        request.setName("Tutoring Contract v2");
        request.setContent("Dieu khoan hop dong gia su cap nhat v2");
        request.setContractType("RECRUITMENT");

        ContractTemplateResponse response = centerService.createContractTemplate(request);

        assertNotNull(response);
        assertEquals("Tutoring Contract v2", response.getName());
        assertEquals("Dieu khoan hop dong gia su cap nhat v2", response.getContent());
        verify(contractTemplateRepository).save(any(ContractTemplate.class));
    }

    @Test
    @DisplayName("ST-ECO-004: Vai trò Trung tâm chỉnh sửa mẫu hợp đồng của mình thành công")
    void testCenterUpdatesContractTemplate() {
        when(authHelper.currentUserId()).thenReturn(CENTER_USER_ID);
        when(tutorCenterRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(center));
        when(contractTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(contractTemplateRepository.save(any(ContractTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        SaveContractTemplateRequest request = new SaveContractTemplateRequest();
        request.setName("Tutoring Contract v2");
        request.setContent("Dieu khoan moi v2");

        ContractTemplateResponse response = centerService.updateContractTemplate(TEMPLATE_ID, request);

        assertNotNull(response);
        assertEquals("Tutoring Contract v2", response.getName());
        assertEquals("Dieu khoan moi v2", response.getContent());
        verify(contractTemplateRepository).save(template);
    }

    @Test
    @DisplayName("ST-ECO-004 [MISMATCH]: Vai trò Admin không có quyền gọi API quản lý mẫu của Trung tâm")
    void testAdminCannotAccessCenterContractTemplateApi() {
        when(authHelper.currentUserId()).thenReturn(ADMIN_USER_ID);
        doThrow(new ForbiddenException("Chỉ trung tâm gia sư mới có thể thao tác"))
                .when(authHelper).requireRole(UserRole.TUTOR_CENTER);

        SaveContractTemplateRequest request = new SaveContractTemplateRequest();
        request.setName("Tutoring Contract v2");
        request.setContent("Noi dung");

        assertThrows(ForbiddenException.class, () -> centerService.createContractTemplate(request));
    }

    @Test
    @DisplayName("ST-ECO-004: Không cho phép sửa mẫu hợp đồng mặc định hệ thống (center == null)")
    void testCannotEditSystemDefaultTemplate() {
        when(authHelper.currentUserId()).thenReturn(CENTER_USER_ID);
        when(tutorCenterRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(center));

        ContractTemplate systemDefaultTemplate = new ContractTemplate();
        systemDefaultTemplate.setTemplateId(99L);
        systemDefaultTemplate.setName("System Default Contract");
        systemDefaultTemplate.setContent("Dieu khoan mac dinh");
        systemDefaultTemplate.setCenter(null); // System default template
        when(contractTemplateRepository.findById(99L)).thenReturn(Optional.of(systemDefaultTemplate));

        SaveContractTemplateRequest request = new SaveContractTemplateRequest();
        request.setName("New Name");

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> centerService.updateContractTemplate(99L, request));
        assertTrue(ex.getMessage().contains("không sửa mẫu hệ thống"));
    }
}
