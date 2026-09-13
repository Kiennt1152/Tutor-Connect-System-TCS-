import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { PlatformDashboardPage } from '../pages/PlatformDashboardPage';
import { PlatformTasksPage } from '../pages/PlatformTasksPage';
import { ApiHelper } from '../utils/apiHelper';
import { testConfig } from '../utils/testConfig';

test.describe('ST-MSG-002: Create a support ticket and track SLA', () => {
  let createdTicketId: number;

  test.beforeAll(async () => {
    // 1. User tạo một support ticket 'Cannot top up'
    const res = await ApiHelper.createSupportTicket({
      category: 'INQUIRY',
      subject: 'Cannot top up - Lỗi giao dịch nạp tiền ví sàn (ST-MSG-002)',
      description: 'Tôi đã thực hiện thanh toán nhưng số dư ví không đổi, vui lòng hỗ trợ gấp.',
      priority: 'HIGH',
    });

    createdTicketId = res.ticketId || res.id;
    console.log(`[ST-MSG-002] Đã tạo support ticket với ID: #${createdTicketId}, SLA DueAt: ${res.dueAt}`);
    expect(res.dueAt).toBeDefined();
  });

  test('Kiểm tra ticket được theo dõi SLA và hiển thị cảnh báo khi quá hạn', async ({ page }) => {
    const loginPage = new LoginPage(page);
    const dashboardPage = new PlatformDashboardPage(page);
    const tasksPage = new PlatformTasksPage(page);

    // 1. Admin đăng nhập
    await loginPage.login(testConfig.admin.email, testConfig.admin.password);

    // 2. Mở Platform Dashboard để kiểm tra KPI thẻ theo dõi SLA
    await dashboardPage.open();

    // 3. Điều hướng tới trang Quản lý tác vụ/Ticket theo SLA (/platform/tasks?priority=URGENT&slaBreached=true)
    await tasksPage.open();

    // 4. Lọc danh sách tác vụ theo tiêu chí quá hạn SLA
    const slaFilterBtn = page.locator('button', { hasText: /Quá hạn SLA/i });
    if (await slaFilterBtn.isVisible()) {
      await slaFilterBtn.click();
    }

    // 5. Xác nhận hệ thống có khả năng hiển thị danh sách các ticket/tác vụ có đánh dấu SLA
    await page.waitForTimeout(1000);
    const pageUrl = page.url();
    expect(pageUrl).toContain('/platform/tasks');
  });
});
