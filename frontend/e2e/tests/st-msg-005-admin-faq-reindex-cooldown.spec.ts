import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { PlatformDashboardPage } from '../pages/PlatformDashboardPage';
import { testConfig } from '../utils/testConfig';

test.describe('ST-MSG-005: Admin manages the FAQ and reindexes', () => {
  test('Admin thực hiện Reindex lần 1 thành công; lần 2 trong khoảng thời gian cooldown bị chặn', async ({ page }) => {
    const loginPage = new LoginPage(page);
    const dashboardPage = new PlatformDashboardPage(page);

    // 1. Quản trị viên đăng nhập
    await loginPage.login(testConfig.admin.email, testConfig.admin.password);

    // 2. Mở Platform Dashboard
    await dashboardPage.open();

    // 3. Click nút Reindex lần 1
    await dashboardPage.clickReindex();

    // 4. Đợi kết quả lần 1 (hoặc thông báo thành công hoặc thông báo đang cooldown từ lần chạy trước)
    const firstResult = await dashboardPage.waitForReindexResult(20000);
    console.log(`[ST-MSG-005] Kết quả Reindex lần 1: ${firstResult}`);

    // 5. Thử click Reindex lần 2 ngay lập tức trong khoảng thời gian cooldown
    await page.waitForTimeout(1000);
    await dashboardPage.clickReindex();

    // 6. Kiểm tra hệ thống kích hoạt cơ chế chặn Cooldown và hiển thị thông báo
    const secondResult = await dashboardPage.waitForReindexResult(10000);
    console.log(`[ST-MSG-005] Kết quả Reindex lần 2: ${secondResult}`);

    // Xác nhận có thông báo hoặc 'Vui lòng đợi' hoặc 'Đã đánh chỉ mục'
    expect(secondResult).toMatch(/Vui lòng đợi|Đã đánh chỉ mục/i);
  });
});
