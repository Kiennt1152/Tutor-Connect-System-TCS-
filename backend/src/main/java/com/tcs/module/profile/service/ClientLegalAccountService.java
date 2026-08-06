package com.tcs.module.profile.service;

import com.tcs.exception.ForbiddenException;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.ParentChildLink;
import com.tcs.module.profile.enums.ParentChildLinkStatus;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.ParentChildLinkRepository;
import com.tcs.module.profile.util.AgeUtils;
import com.tcs.security.AuthHelper;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ClientLegalAccountService {

    private final AuthHelper authHelper;
    private final ClientRepository clientRepository;
    private final ParentChildLinkRepository parentChildLinkRepository;

    @Getter
    @Builder
    public static class LegalAccountContext {
        /** User đang đăng nhập (học sinh hoặc phụ huynh). */
        private final Long sessionUserId;

        /** User chịu trách nhiệm pháp lý / thanh toán. */
        private final Long legalUserId;

        private final String legalHolderName;
        private final String legalHolderEmail;

        /** Thủ tục pháp lý chuyển sang phụ huynh (học sinh vị thành niên đã liên kết). */
        private final boolean delegatedToParent;

        /** Học sinh được đại diện — chỉ có khi delegatedToParent. */
        private final Long beneficiaryMinorUserId;
        private final String beneficiaryMinorName;
    }

    public Optional<ParentChildLink> findGuardianLinkForMinor(Client client) {
        if (!StringUtils.hasText(client.getFullName()) || client.getDateOfBirth() == null) {
            return Optional.empty();
        }
        return parentChildLinkRepository.findFirstByChildProfile_FullNameAndChildProfile_DateOfBirthAndStatus(
                client.getFullName(), client.getDateOfBirth(), ParentChildLinkStatus.ACTIVE);
    }

    public void requirePaymentEligibility(Client client) {
        if (client == null || client.getDateOfBirth() == null) {
            throw new ForbiddenException(
                    "Vui lòng cập nhật ngày sinh và hoàn tất liên kết hồ sơ trước khi thanh toán");
        }
        if (!AgeUtils.isMinor(client.getDateOfBirth())) {
            return;
        }
        if (!StringUtils.hasText(client.getFullName())) {
            throw new ForbiddenException(
                    "Vui lòng cập nhật họ tên trước khi liên kết phụ huynh và thanh toán");
        }
        if (findGuardianLinkForMinor(client).isEmpty()) {
            throw new ForbiddenException(
                    "Học sinh vị thành niên cần liên kết hồ sơ phụ huynh trước khi thanh toán hoặc tạo hợp đồng");
        }
    }

    public LegalAccountContext resolveForClient(Client client) {
        Long sessionUserId = client.getUser().getUserId();
        if (client.getDateOfBirth() == null || !AgeUtils.isMinor(client.getDateOfBirth())) {
            return LegalAccountContext.builder()
                    .sessionUserId(sessionUserId)
                    .legalUserId(sessionUserId)
                    .legalHolderName(client.getFullName())
                    .legalHolderEmail(client.getUser().getEmail())
                    .delegatedToParent(false)
                    .build();
        }

        ParentChildLink guardianLink = findGuardianLinkForMinor(client)
                .orElseThrow(() -> new ForbiddenException(
                        "Học sinh vị thành niên cần liên kết phụ huynh để thực hiện thủ tục pháp lý và thanh toán"));

        Client parentClient = clientRepository
                .findByUser_UserId(guardianLink.getParentUser().getUserId())
                .orElseThrow(() -> new ForbiddenException("Không tìm thấy hồ sơ phụ huynh liên kết"));

        return LegalAccountContext.builder()
                .sessionUserId(sessionUserId)
                .legalUserId(guardianLink.getParentUser().getUserId())
                .legalHolderName(parentClient.getFullName())
                .legalHolderEmail(guardianLink.getParentUser().getEmail())
                .delegatedToParent(true)
                .beneficiaryMinorUserId(sessionUserId)
                .beneficiaryMinorName(client.getFullName())
                .build();
    }

    public LegalAccountContext requireLegalAccountForCurrentClient() {
        Long currentUserId = authHelper.currentUserId();
        return clientRepository
                .findByUser_UserId(currentUserId)
                .map(client -> {
                    requirePaymentEligibility(client);
                    return resolveForClient(client);
                })
                .orElseGet(() -> LegalAccountContext.builder()
                        .sessionUserId(currentUserId)
                        .legalUserId(currentUserId)
                        .delegatedToParent(false)
                        .build());
    }

    public Long requireLegalUserIdForCurrentClient() {
        return requireLegalAccountForCurrentClient().getLegalUserId();
    }
}
