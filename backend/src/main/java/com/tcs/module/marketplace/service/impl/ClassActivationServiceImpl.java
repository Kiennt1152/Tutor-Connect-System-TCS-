package com.tcs.module.marketplace.service.impl;

import com.tcs.common.event.ContractSigned;
import com.tcs.common.event.EscrowFunded;
import java.math.BigDecimal;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.service.ClassActivationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassActivationServiceImpl implements ClassActivationService {

    private final TutoringClassRepository tutoringClassRepository;

    @Override
    @Transactional
    @EventListener
    public void onContractSigned(ContractSigned event) {
        log.info("[ClassActivation] Nhan ContractSigned cho contract={}, class={}",
                event.contractId(), event.classId());
        if (event.amount() != null
                && event.amount().compareTo(BigDecimal.ZERO) > 0
                && (event.assignmentId() != null || event.classStudentId() != null)) {
            log.info("[ClassActivation] Cho SePay xac nhan escrow truoc khi kich hoat class={}", event.classId());
            return;
        }
        activate(event.classId());
    }

    @Transactional
    @EventListener
    public void onEscrowFunded(EscrowFunded event) {
        log.info("[ClassActivation] Nhan EscrowFunded cho escrow={}, class={}",
                event.escrowId(), event.classId());
        activate(event.classId());
    }

    @Override
    @Transactional
    public void activate(Long classId) {
        if (classId == null) {
            log.warn("[ClassActivation] classId null, skip activate");
            return;
        }
        TutoringClass cls = tutoringClassRepository.findById(classId).orElse(null);
        if (cls == null) {
            log.warn("[ClassActivation] Khong tim thay class id={}", classId);
            return;
        }
        if (cls.getStatus() == TutoringClassStatus.IN_PROGRESS
                || cls.getStatus() == TutoringClassStatus.COMPLETED
                || cls.getStatus() == TutoringClassStatus.CANCELLED) {
            log.info("[ClassActivation] Class {} da o trang thai {}, skip", classId, cls.getStatus());
            return;
        }
        cls.setStatus(TutoringClassStatus.IN_PROGRESS);
        tutoringClassRepository.save(cls);
        log.info("[ClassActivation] Da kich hoat class id={}", classId);
    }
}
