package com.tcs.module.marketplace.event;

import com.tcs.module.marketplace.service.MarketplaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ClientReviewedClassCompletionListener {

    private final MarketplaceService marketplaceService;

    /**
     * Client review xong thì thử đóng lớp lại một lần nữa.
     * Listener này là lớp riêng để tránh phụ thuộc vào việc bean service hiện tại có được nhận event
     * trong mọi cấu hình proxy hay không.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onClientReviewedClass(ClientReviewedClassEvent event) {
        marketplaceService.completeClassAfterClientReview(event.classId());
    }
}
