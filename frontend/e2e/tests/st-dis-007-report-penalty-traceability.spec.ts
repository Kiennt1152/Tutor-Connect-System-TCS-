import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { PlatformPenaltiesPage } from '../pages/PlatformPenaltiesPage';
import { PlatformReportsPage } from '../pages/PlatformReportsPage';
import { ApiHelper } from '../utils/apiHelper';
import { testConfig } from '../utils/testConfig';

test.describe('ST-DIS-007: Report a user or class, creating a penalty source', () => {
  let createdReportId: number;
  let issuedPenaltyId: number;

  test.beforeAll(async () => {
    // Bước 1 (Precondition / Setup): Người dùng gửi báo cáo vi phạm đối với tutor05
    createdReportId = await ApiHelper.createReport({
      targetId: testConfig.targetTutor.userId,
      targetType: 'USER',
      category: 'FRAUD',
      description: 'Gia sư có hành vi gian lận buổi học và thu học phí ngoài luồng trái phép (ST-DIS-007).',
    });

    expect(createdReportId).toBeDefined();
    console.log(`[ST-DIS-007] Đã tạo báo cáo vi phạm với ID: #${createdReportId}`);

    // Bước 2: Admin ban hành quyết định xử phạt liên kết nguồn REPORT vừa tạo
    const adminToken = await ApiHelper.getAdminToken();
    const penaltyRes = await fetch(`${testConfig.apiUrl}/platform/penalties`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${adminToken}`,
      },
      body: JSON.stringify({
        userId: testConfig.targetTutor.userId,
        penaltyType: 'WARNING',
        reason: `Xử phạt cảnh cáo do có hành vi gian lận từ báo cáo vi phạm #${createdReportId} (tối thiểu 20 ký tự)`,
        sourceType: 'REPORT',
        sourceId: createdReportId,
        sourceTaskId: `REPORT-${createdReportId}`,
      }),
    });

    const penaltyData = await penaltyRes.json();
    issuedPenaltyId = penaltyData.penaltyId || penaltyData.id;
    expect(issuedPenaltyId).toBeDefined();
    console.log(`[ST-DIS-007] Đã tạo án phạt với ID: #${issuedPenaltyId} liên kết nguồn REPORT #${createdReportId}`);
  });

  test.afterAll(async () => {
    // Dọn dẹp án phạt test sau khi hoàn tất để khôi phục trạng thái tài khoản
    if (issuedPenaltyId) {
      try {
        await ApiHelper.revokePenalty(issuedPenaltyId, 'Dọn dẹp sau khi chạy test tự động ST-DIS-007');
        console.log(`[ST-DIS-007] Đã thu hồi án phạt test #${issuedPenaltyId}`);
      } catch (err) {
        console.warn(`[ST-DIS-007] Không thể thu hồi án phạt test:`, err);
      }
    }
  });

  test('Kiểm tra hiển thị huy hiệu nguồn REPORT và back-link điều hướng chính xác về báo cáo', async ({ page }) => {
    const loginPage = new LoginPage(page);
    const penaltiesPage = new PlatformPenaltiesPage(page);
    const reportsPage = new PlatformReportsPage(page);

    // 1. Quản trị viên đăng nhập vào hệ thống
    await loginPage.login(testConfig.admin.email, testConfig.admin.password);

    // 2. Điều hướng tới màn hình Quản lý xử phạt
    await penaltiesPage.open();

    // 3. Lọc danh sách theo nguồn REPORT
    await penaltiesPage.filterBySourceType('REPORT');

    // 4. Xác nhận hàng án phạt hiển thị đúng huy hiệu nguồn "REPORT #<id>"
    await penaltiesPage.verifySourceBadge('REPORT', createdReportId);

    // 5. Click vào nút "Mở case nguồn →" để kiểm tra tính truy vết (Traceability)
    await penaltiesPage.clickOpenSourceCase('REPORT', createdReportId);

    // 6. Kiểm tra URL điều hướng chính xác về báo cáo nguồn
    await reportsPage.verifyNavigationFromSource(createdReportId);
  });
});
