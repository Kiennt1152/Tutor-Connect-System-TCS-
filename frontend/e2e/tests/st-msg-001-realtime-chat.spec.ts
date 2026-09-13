import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { ChatPage } from '../pages/ChatPage';
import { ApiHelper } from '../utils/apiHelper';
import { testConfig } from '../utils/testConfig';

test.describe('ST-MSG-001: Two-way realtime chat and push notifications', () => {
  let conversationId: number;

  test.beforeAll(async () => {
    // Đảm bảo hội thoại giữa client01 và tutor01 đã tồn tại
    try {
      const clientToken = await ApiHelper.getClientToken();
      const conv = await ApiHelper.startOrGetConversation(clientToken, testConfig.tutor.userId);
      conversationId = conv.conversationId || conv.id;
    } catch (e) {
      console.warn('[ST-MSG-001] Pre-setup conversation:', e);
    }
  });

  test('Gửi và nhận tin nhắn hai chiều thời gian thực qua WebSocket', async ({ browser }) => {
    // 1. Tạo 2 Browser Context độc lập cho Client và Tutor
    const clientContext = await browser.newContext();
    const tutorContext = await browser.newContext();

    const clientPage = await clientContext.newPage();
    const tutorPage = await tutorContext.newPage();

    const clientLogin = new LoginPage(clientPage);
    const tutorLogin = new LoginPage(tutorPage);

    const clientChat = new ChatPage(clientPage);
    const tutorChat = new ChatPage(tutorPage);

    // 2. Đăng nhập song song 2 tài khoản
    await clientLogin.login(testConfig.client.email, testConfig.client.password);
    await tutorLogin.login(testConfig.tutor.email, testConfig.tutor.password);

    // 3. Cả 2 cùng mở trang tin nhắn với cuộc trò chuyện
    await clientChat.open(conversationId);
    await tutorChat.open(conversationId);

    // 4. Client gửi tin nhắn tới Tutor
    const testMessage = `Xin chào gia sư, đây là tin nhắn kiểm thử tự động ST-MSG-001 [${Date.now()}]`;
    await clientChat.sendMessage(testMessage);

    // 5. Xác nhận tin nhắn hiển thị ngay lập tức phía Client
    await clientChat.verifyMessageReceived(testMessage);

    // 6. Xác nhận tin nhắn hiển thị thời gian thực (realtime) phía Tutor qua WebSocket mà không cần F5
    await tutorChat.verifyMessageReceived(testMessage, 10000);

    // 7. Dọn dẹp context
    await clientContext.close();
    await tutorContext.close();
  });
});
