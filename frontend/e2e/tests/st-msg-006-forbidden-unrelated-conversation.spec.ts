import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { ChatPage } from '../pages/ChatPage';
import { ApiHelper } from '../utils/apiHelper';
import { testConfig } from '../utils/testConfig';

test.describe('ST-MSG-006: Accessing an unrelated conversation', () => {
  let privateConversationId: number;

  test.beforeAll(async () => {
    // 1. Tạo hoặc lấy cuộc trò chuyện riêng tư giữa Client01 và Tutor01
    try {
      const clientToken = await ApiHelper.getClientToken();
      const conv = await ApiHelper.startOrGetConversation(clientToken, testConfig.tutor.userId);
      privateConversationId = conv.conversationId || conv.id;
      console.log(`[ST-MSG-006] Cuộc trò chuyện riêng tư của Client01 & Tutor01: #${privateConversationId}`);
    } catch (e) {
      console.warn('[ST-MSG-006] Pre-setup private conversation:', e);
      privateConversationId = 1;
    }
  });

  test('Người dùng không liên quan (Tutor05) bị từ chối truy cập 100% khi cố truy vấn cuộc trò chuyện của người khác', async ({ page, request }) => {
    // 1. Kiểm tra ở mức API: Tutor05 gửi yêu cầu lấy tin nhắn của hội thoại bí mật
    const unrelatedToken = await ApiHelper.getUnrelatedTutorToken();
    const apiRes = await request.get(`${testConfig.apiUrl}/messaging/conversations/${privateConversationId}/messages`, {
      headers: {
        Authorization: `Bearer ${unrelatedToken}`,
      },
    });

    // 2. Xác nhận API trả về HTTP 403 Forbidden (hoặc 404), tuyệt đối không để lộ dữ liệu
    console.log(`[ST-MSG-006] API Response Status khi Tutor05 truy cập: ${apiRes.status()}`);
    expect([403, 404]).toContain(apiRes.status());

    // 3. Kiểm tra ở mức Giao diện UI: Tutor05 đăng nhập và mở URL hội thoại
    const loginPage = new LoginPage(page);
    const chatPage = new ChatPage(page);

    await loginPage.login(testConfig.unrelatedTutor.email, testConfig.unrelatedTutor.password);
    await chatPage.open(privateConversationId);

    // 4. Xác nhận không có bất kỳ bong bóng tin nhắn nào của Client01 & Tutor01 bị rò rỉ trên màn hình
    await chatPage.verifyForbiddenAccess();
  });
});
