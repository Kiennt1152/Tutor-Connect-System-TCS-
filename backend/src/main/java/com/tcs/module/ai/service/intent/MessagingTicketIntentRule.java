package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

@Component
public class MessagingTicketIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        if (containsAny(normalized, "nhan tin voi gia su", "chat voi phu huynh", "nhan tin voi phu huynh", "chat voi", "nhan tin voi", "nhan tin rieng", "chat rieng", "nhan tin")) {
            return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.MESSAGING_OPEN_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/messages");
        }

        if (containsAny(normalized, "sla", "thoi gian phan hoi sla", "quy dinh sla phan hoi")) {
            return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_SLA, AiIntent.TICKET_SUPPORT, 0.95, "/support/tickets");
        }

        if (containsAny(normalized, "kiem tra trang thai ticket", "trang thai ticket")) {
            return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_STATUS, AiIntent.TICKET_SUPPORT, 0.9, "/support/tickets");
        }

        if (containsAny(normalized, "tao ticket", "gui ticket", "dong ticket", "mo lai ticket", "xem thong bao", "yeu cau ho tro", "ticket",
                "tao phieu", "phieu ho tro", "phieu khieu nai", "gui phieu", "tao phieu khieu nai", "tao phieu ho tro")) {
            return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_CREATE, AiIntent.TICKET_SUPPORT, 0.95, "/support/tickets");
        }

        return null;
    }
}
