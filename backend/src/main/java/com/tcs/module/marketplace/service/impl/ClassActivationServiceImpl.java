package com.tcs.module.marketplace.service.impl;

import com.tcs.common.event.ContractSigned;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.service.ClassActivationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassActivationServiceImpl implements ClassActivationService {

    private final EscrowService escrowService;
    private final TutoringClassRepository tutoringClassRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassStudentRepository classStudentRepository;

    @Override
    @EventListener
    @Transactional
    public void onContractSigned(ContractSigned event) {
        Long classId = resolveClassId(event);

        escrowService.lock(new EscrowLockCommand(
                event.payerUserId(),
                event.amount(),
                event.assignmentId(),
                event.classStudentId()));

        activate(classId);
    }

    @Override
    @Transactional
    public void activate(Long classId) {
        TutoringClass tutoringClass = tutoringClassRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));

        if (tutoringClass.getStatus() == TutoringClassStatus.IN_PROGRESS) {
            return;
        }
        if (tutoringClass.getStatus() == TutoringClassStatus.COMPLETED
                || tutoringClass.getStatus() == TutoringClassStatus.CANCELLED
                || tutoringClass.getStatus() == TutoringClassStatus.DISPUTED) {
            throw new BusinessException("Lớp không thể kích hoạt ở trạng thái hiện tại");
        }

        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
        tutoringClassRepository.save(tutoringClass);
    }

    private Long resolveClassId(ContractSigned event) {
        if (event.classId() != null) {
            return event.classId();
        }
        if (event.assignmentId() != null) {
            return classAssignmentRepository.findById(event.assignmentId())
                    .map(assignment -> assignment.getApplication().getTutoringClass().getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));
        }
        if (event.classStudentId() != null) {
            return classStudentRepository.findById(event.classStudentId())
                    .map(classStudent -> classStudent.getTutoringClass().getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ghi danh học viên"));
        }
        throw new BusinessException("Thiếu lớp hoặc target để kích hoạt");
    }
}
