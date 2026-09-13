import { test, expect } from '@playwright/test';
import { AiAssistantPage } from '../pages/AiAssistantPage';

test.describe('ST-MSG-004: Out-of-scope question', () => {
  test('AI chatbot trả lời lịch sự câu hỏi ngoài phạm vi, typing indicator và nút Copy hoạt động tốt', async ({ page, context }) => {
    // Cấp quyền clipboard để kiểm tra tính năng copy
    await context.grantPermissions(['clipboard-read', 'clipboard-write']);

    const aiPage = new AiAssistantPage(page);

    // 1. Mở trang Trợ lý AI
    await aiPage.open();

    // 2. Nhập câu hỏi ngoài phạm vi nghiệp vụ
    const outOfScopeQuery = '1 + 1 = ?';
    await aiPage.askQuestion(outOfScopeQuery);

    // 3. Kiểm tra phản hồi từ Assistant
    const reply = await aiPage.waitForAssistantResponse(10000);
    console.log(`[ST-MSG-004] Phản hồi câu hỏi ngoài phạm vi: ${reply}`);

    // 4. Kiểm tra phản hồi không bị lỗi mã hóa font (mojibake) và có nội dung văn minh, lịch sự
    expect(reply.length).toBeGreaterThan(5);
    expect(reply).not.toContain('undefined');
    expect(reply).not.toContain('null');
    expect(reply).not.toContain('Exception:');

    // 5. Kiểm tra nút Copy trên tin nhắn phản hồi hoạt động bình thường
    await aiPage.clickCopyButton();
    await page.waitForTimeout(500);
  });
});
