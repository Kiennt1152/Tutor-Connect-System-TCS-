package com.tcs.config;

import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.TopupSession;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.service.PaymentGateway;
import com.tcs.module.finance.service.SettlementService;
import com.tcs.module.marketplace.service.RecommendationService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ban stub/mock cho pha 1 (seam 0.8 va 0.10).
 * Dung @ConditionalOnMissingBean: khi M3/M1/M2 cung cap ban that,
 * stub tu dong nhuong cho, khong gay xung dot bean.
 */
@Configuration
public class PhaseOneStubConfig {

    /** 0.10: pha 1 chua co AI -> tra danh sach goi y rong. */
    @Bean
    @ConditionalOnMissingBean(RecommendationService.class)
    public RecommendationService recommendationServiceStub() {
        return classId -> List.of();
    }

    /** 0.8: mock cong thanh toan - tu confirm de dev song song. */
    @Bean
    @ConditionalOnMissingBean(PaymentGateway.class)
    public PaymentGateway paymentGatewayMock() {
        return new PaymentGateway() {
            @Override
            public TopupSession createQr(BigDecimal amount, String reference) {
                return new TopupSession(reference, amount, "MOCK-QR:" + reference, "PENDING");
            }

            @Override
            public boolean isConfirmed(String reference) {
                return true;
            }
        };
    }

    /** 0.2: seam escrow (M3) chua co ban that -> stub de context khoi dong. */
    @Bean
    @ConditionalOnMissingBean(EscrowService.class)
    public EscrowService escrowServiceStub() {
        return new EscrowService() {
            @Override
            public EscrowTransaction lock(EscrowLockCommand command) {
                EscrowTransaction tx = new EscrowTransaction();
                tx.setAmount(command.amount());
                return tx;
            }

            @Override
            public void apply(ReleaseInstruction instruction) {
                // no-op: pha 1 chua thuc thi giai ngan.
            }
        };
    }

    /** 0.5: seam settlement (M4) chua co ban that -> tra chi dan gia tri 0. */
    @Bean
    @ConditionalOnMissingBean(SettlementService.class)
    public SettlementService settlementServiceStub() {
        return classId -> new ReleaseInstruction(
                null, BigDecimal.ZERO, BigDecimal.ZERO, "STUB pha 1: settlement chua trien khai");
    }
}
