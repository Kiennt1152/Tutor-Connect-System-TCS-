import { type Page, type Locator, expect } from '@playwright/test';

export class ReportUserDialog {
  readonly modal: Locator;
  readonly categorySelect: Locator;
  readonly descriptionInput: Locator;
  readonly submitButton: Locator;
  readonly cancelButton: Locator;
  readonly closeButton: Locator;
  readonly errorMessage: Locator;

  constructor(private readonly page: Page) {
    this.modal = page.locator('.msg-user-search-modal[role="dialog"]');
    this.categorySelect = this.modal.locator('select.adm-field');
    this.descriptionInput = this.modal.locator('textarea.adm-field--tall');
    this.submitButton = this.modal.getByRole('button', { name: 'Gửi báo cáo' });
    this.cancelButton = this.modal.getByRole('button', { name: 'Hủy' });
    this.closeButton = this.modal.locator('.msg-modal__close');
    this.errorMessage = this.modal.locator('.adm-alert--error');
  }

  async isVisible(): Promise<boolean> {
    return this.modal.isVisible();
  }

  async submitReport(category: 'FRAUD' | 'ABUSE' | 'INAPPROPRIATE' | 'OTHER', description: string): Promise<number | null> {
    await expect(this.modal).toBeVisible();
    await this.categorySelect.selectOption(category);
    await this.descriptionInput.fill(description);

    // Bắt API response để lấy reportId vừa tạo
    const [response] = await Promise.all([
      this.page.waitForResponse(
        (res) => res.url().includes('/messaging/reports') && res.status() === 200,
        { timeout: 10000 }
      ),
      this.submitButton.click(),
    ]);

    const data = await response.json();
    return data?.reportId ?? data?.id ?? null;
  }
}
