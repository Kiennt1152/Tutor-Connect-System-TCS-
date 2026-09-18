package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tcs.module.finance.dto.response.AdminEscrowPageResponse;
import com.tcs.module.finance.dto.response.AdminEscrowResponse;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminEscrowServiceImplTest {

    @Mock
    private EscrowTransactionRepository repository;

    @InjectMocks
    private AdminEscrowServiceImpl service;

    private EscrowTransaction sampleEscrow;

    @BeforeEach
    void setUp() {
        User payerUser = new User();
        payerUser.setUserId(100L);
        payerUser.setEmail("payer@example.com");

        Wallet wallet = new Wallet();
        wallet.setUser(payerUser);

        PaymentTransaction payment = new PaymentTransaction();
        payment.setTransactionId(200L);
        payment.setReferenceCode("REF-12345");
        payment.setWallet(wallet);

        User tutorUser = new User();
        tutorUser.setUserId(300L);
        tutorUser.setEmail("tutor@example.com");

        Tutor tutor = new Tutor();
        tutor.setUser(tutorUser);

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(400L);
        tutoringClass.setTitle("Toán 12 Cơ bản");

        TutorApplication app = new TutorApplication();
        app.setTutoringClass(tutoringClass);

        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(500L);
        assignment.setTutor(tutor);
        assignment.setApplication(app);

        sampleEscrow = new EscrowTransaction();
        sampleEscrow.setEscrowId(1L);
        sampleEscrow.setPayment(payment);
        sampleEscrow.setAmount(BigDecimal.valueOf(500000));
        sampleEscrow.setStatus(EscrowStatus.FUNDED);
        sampleEscrow.setAssignment(assignment);
        sampleEscrow.setDepositedAt(LocalDateTime.now());
        sampleEscrow.setCreatedAt(LocalDateTime.now());
        sampleEscrow.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testGet_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleEscrow));

        AdminEscrowResponse response = service.get(1L);

        assertNotNull(response);
        assertEquals(1L, response.getEscrowId());
        assertEquals(200L, response.getPaymentId());
        assertEquals("REF-12345", response.getReferenceCode());
        assertEquals(BigDecimal.valueOf(500000), response.getAmount());
        assertEquals(EscrowStatus.FUNDED, response.getStatus());
        assertEquals(100L, response.getPayerUserId());
        assertEquals("payer@example.com", response.getPayerEmail());
        assertEquals(300L, response.getBeneficiaryUserId());
        assertEquals("tutor@example.com", response.getBeneficiaryEmail());
        assertEquals("PRIVATE_CLASS_ESCROW", response.getTransactionType());
        assertEquals("Thanh toán lớp private", response.getTransactionTypeLabel());
        assertEquals(400L, response.getClassId());
        assertEquals("Toán 12 Cơ bản", response.getClassTitle());
        assertEquals(500L, response.getAssignmentId());
    }

    @Test
    void testSearch_Success() {
        PageImpl<EscrowTransaction> page = new PageImpl<>(List.of(sampleEscrow), PageRequest.of(0, 10), 1);
        when(repository.searchAdmin(any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        AdminEscrowPageResponse result = service.search(null, null, null, null, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("PRIVATE_CLASS_ESCROW", result.getContent().get(0).getTransactionType());
    }
}
