package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.repository.SupportTicketRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTicketContextProvider {

    private final SupportTicketRepository ticketRepository;

    public List<AiSourceResponse> getTicketContext(String userRole, Long userId) {
        List<AiSourceResponse> results = new ArrayList<>();
        
        if ("PLATFORM_ADMIN".equals(userRole)) {
            // Get OPEN or IN_PROGRESS tickets
            List<SupportTicket> activeTickets = ticketRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS)
            );
            
            for (SupportTicket t : activeTickets) {
                results.add(AiSourceResponse.builder()
                    .sourceId("TICKET_" + t.getTicketId())
                    .sourceType("TICKET")
                    .title("Ticket #" + t.getTicketId() + " - " + t.getSubject())
                    .snippet("Status: " + t.getStatus() + ", Priority: " + t.getPriority() + "\n" + t.getDescription())
                    .similarity(1.0)
                    .finalScore(1.0)
                    .visibility("ADMIN_ONLY")
                    .build());
            }
        } else if (userId != null) {
            // Get user's tickets
            List<SupportTicket> userTickets = ticketRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
            for (SupportTicket t : userTickets) {
                results.add(AiSourceResponse.builder()
                    .sourceId("TICKET_" + t.getTicketId())
                    .sourceType("TICKET")
                    .title("Ticket của bạn #" + t.getTicketId() + " - " + t.getSubject())
                    .snippet("Trạng thái: " + t.getStatus() + ", Mức độ: " + t.getPriority() + "\n" + t.getDescription())
                    .similarity(1.0)
                    .finalScore(1.0)
                    .visibility("OWNER_PRIVATE")
                    .build());
            }
        }
        
        return results;
    }
}
