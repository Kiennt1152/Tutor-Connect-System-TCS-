package com.tcs.module.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
class Report52ExpiredClassCleanupITTest {

    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private EntityManager entityManager;
    @Mock private Query query;

    private ExpiredClassCleanupService cleanupService;

    @BeforeEach
    void setUpExpiredCleanupItFixture() {
        PlatformTransactionManager transactionManager = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
        cleanupService = new ExpiredClassCleanupService(tutoringClassRepository, transactionManager);
        ReflectionTestUtils.setField(cleanupService, "em", entityManager);

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("id"), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);
    }

    @Test
    @Tag("report52-it")
    void IT_MKT_018_CleanupExpiredOpenClassRemovesDependentRowsBeforeClassRow() {
        TutoringClass expired = new TutoringClass();
        expired.setClassId(701L);
        expired.setStatus(TutoringClassStatus.OPEN);
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(tutoringClassRepository.findByStatusAndExpiresAtBefore(
                eq(TutoringClassStatus.OPEN),
                any(LocalDateTime.class)))
                .thenReturn(List.of(expired));

        cleanupService.cleanupExpiredOpenClasses();

        verify(tutoringClassRepository).findByStatusAndExpiresAtBefore(
                eq(TutoringClassStatus.OPEN),
                any(LocalDateTime.class));
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(6)).createNativeQuery(sqlCaptor.capture());
        var statements = sqlCaptor.getAllValues();
        assertEquals(6, statements.size());
        assertTrue(statements.get(0).startsWith("DELETE FROM application_status_histories"));
        assertTrue(statements.get(1).startsWith("DELETE FROM tutor_applications"));
        assertTrue(statements.get(2).startsWith("DELETE FROM recommendation_logs"));
        assertTrue(statements.get(3).startsWith("DELETE FROM schedule_slots"));
        assertTrue(statements.get(4).startsWith("UPDATE support_tickets"));
        assertTrue(statements.get(5).startsWith("DELETE FROM tutoring_classes"));
        verify(query, atLeast(6)).setParameter("id", 701L);
        verify(query, atLeast(6)).executeUpdate();
    }
}
