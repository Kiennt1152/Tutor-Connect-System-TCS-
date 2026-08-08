package com.tcs.module.marketplace.service.impl;

import com.tcs.common.event.ContractSigned;
import com.tcs.common.event.EscrowFunded;
import java.math.BigDecimal;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
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
    private final EscrowTransactionRepository escrowTransactionRepository;

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
        if (event.classId() == null) {
            return;
        }
        TutoringClass cls = tutoringClassRepository.findById(event.classId()).orElse(null);
        if (cls != null && cls.getClassType() == ClassType.CENTER) {
            log.info("[ClassActivation] Class CENTER {} chi dung de ghi danh, bo qua kich hoat tu dong", event.classId());
            return;
        }
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
        if (cls.getClassType() == ClassType.CENTER && !hasEnoughFundedCenterEscrows(cls)) {
            log.info("[ClassActivation] Class CENTER {} chua du escrow da thanh toan, skip activate", classId);
            return;
        }
        cls.setStatus(TutoringClassStatus.IN_PROGRESS);
        tutoringClassRepository.save(cls);
        log.info("[ClassActivation] Da kich hoat class id={}", classId);
    }

    private boolean hasEnoughFundedCenterEscrows(TutoringClass cls) {
        int requiredStudents = cls.getMinStudents() != null && cls.getMinStudents() > 0
                ? cls.getMinStudents()
                : 1;
        long fundedEscrows = escrowTransactionRepository
                .findByClassStudent_TutoringClass_ClassId(cls.getClassId())
                .stream()
                .filter(escrow -> escrow.getStatus() == EscrowStatus.FUNDED)
                .count();
        return fundedEscrows >= requiredStudents;
    }
}
