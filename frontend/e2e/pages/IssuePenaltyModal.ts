import { type Page, type Locator, expect } from '@playwright/test';

export class IssuePenaltyModal {
  readonly modal: Locator;
  readonly userIdInput: Locator;
  readonly penaltyTypeSelect: Locator;
  readonly reasonTextarea: Locator;
  readonly evidenceInput: Locator;
  readonly sourceBadge: Locator;
  readonly submitButton: Locator;
  readonly cancelButton: Locator;
  readonly errorMessage: Locator;

  constructor(private readonly page: Page) {
    this.modal = page.locator('.adm-penalty-modal');
    this.userIdInput = this.modal.locator('input[type="number"]');
    this.penaltyTypeSelect = this.modal.locator('select').first();
    this.reasonTextarea = this.modal.locator('textarea');
    this.evidenceInput = this.modal.locator('input[type="text"]');
    this.sourceBadge = this.modal.locator('.adm-penalty-modal__source-badge');
    this.submitButton = this.modal.locator('button.btn-submit, button[type="submit"]').filter({ hasText: /Xác nhận|Đang xử lý/ });
    this.cancelButton = this.modal.locator('button.btn-cancel, button.btn-close');
    this.errorMessage = this.modal.locator('.adm-penalty-modal__error');
  }

  async isVisible(): Promise<boolean> {
    return this.modal.isVisible();
  }

  async fillAndSubmit(params: {
    userId: number;
    penaltyType?: 'WARNING' | 'FEATURE_RESTRICTION' | 'TEMPORARY_BAN' | 'PERMANENT_BAN';
    reason: string;
    evidenceUrls?: string;
  }) {
    await expect(this.modal).toBeVisible();

    if (await this.userIdInput.isVisible()) {
      await this.userIdInput.fill(String(params.userId));
    }

    if (params.penaltyType) {
      await this.penaltyTypeSelect.selectOption(params.penaltyType);
    }

    await this.reasonTextarea.fill(params.reason);

    if (params.evidenceUrls && await this.evidenceInput.isVisible()) {
      await this.evidenceInput.fill(params.evidenceUrls);
    }

    // Đợi API xử phạt hoàn thành
    const [response] = await Promise.all([
      this.page.waitForResponse(
        (res) => res.url().includes('/platform/penalties') && res.request().method() === 'POST',
        { timeout: 10000 }
      ),
      this.submitButton.click(),
    ]);

    expect(response.status()).toBe(200);
    // Chờ modal đóng
    await expect(this.modal).not.toBeVisible();
  }
}
