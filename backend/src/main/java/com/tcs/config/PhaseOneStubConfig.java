package com.tcs.config;

import com.tcs.module.finance.dto.TopupSession;
import com.tcs.module.finance.service.PaymentGateway;
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
}
