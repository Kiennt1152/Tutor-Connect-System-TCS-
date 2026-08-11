package com.tcs.module.platform.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.entity.Conversation;
import com.tcs.module.messaging.entity.Message;
import com.tcs.module.platform.entity.CircumventionEvent;
import com.tcs.module.platform.repository.CircumventionEventRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.security.AuthHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CircumventionServiceImplTest {
    @Mock CircumventionEventRepository repository;
    @Mock UserRepository userRepository;
    @Mock AuthHelper authHelper;
    @Mock AuditLogService auditLogService;
    @InjectMocks CircumventionServiceImpl service;

    @Test
    void inspect_createsOneEventPerMatchedRule() {
        User sender = new User(); sender.setUserId(7L);
        Conversation conversation = new Conversation(); conversation.setConversationId(8L);
        Message message = new Message(); message.setMessageId(9L); message.setSender(sender);
        message.setConversation(conversation); message.setContent("Liên hệ 0912345678 hoặc an@example.com");

        service.inspect(message);

        verify(repository, times(2)).save(any(CircumventionEvent.class));
    }
}
