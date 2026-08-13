package com.tcs.module.platform.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.module.platform.enums.UserPenaltyStatus;
import com.tcs.module.platform.enums.UserPenaltyType;
import com.tcs.module.platform.repository.UserPenaltyRepository;
import com.tcs.module.platform.service.PenaltyAccessService;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class PenaltyAccessServiceImpl implements PenaltyAccessService {
    private final UserPenaltyRepository repository;

    @Override
    @Transactional(readOnly = true)
    public void requireFeature(Long userId, String featureCode) {
        String normalized = featureCode.toUpperCase(Locale.ROOT);
        boolean restricted = repository.findByUser_UserIdAndStatus(userId, UserPenaltyStatus.ACTIVE).stream()
                .filter(item -> item.getPenaltyType() == UserPenaltyType.FEATURE_RESTRICTION)
                .filter(item -> item.getExpiresAt() == null || item.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(item -> item.getRestrictionDetails() == null ? "" : item.getRestrictionDetails().toUpperCase(Locale.ROOT))
                .anyMatch(details -> details.contains('"' + normalized + '"'));
        if (restricted) throw new ForbiddenException("Tính năng " + normalized + " đang bị hạn chế trên tài khoản này.");
    }
}
