import { type Page, type Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';
import { IssuePenaltyModal } from './IssuePenaltyModal';

export class PlatformPenaltiesPage extends BasePage {
  readonly createPenaltyButton: Locator;
  readonly sourceTypeFilter: Locator;
  readonly statusFilter: Locator;
  readonly penaltiesTable: Locator;
  readonly issueModal: IssuePenaltyModal;

  constructor(page: Page) {
    super(page);
    this.createPenaltyButton = page.getByRole('button', { name: 'Tạo xử phạt' });
    this.sourceTypeFilter = page.locator('.adm-penalty-filters select').nth(2);
    this.statusFilter = page.locator('.adm-penalty-filters select').first();
    this.penaltiesTable = page.locator('table');
    this.issueModal = new IssuePenaltyModal(page);
  }

  async open() {
    await this.goto('/platform/penalties');
    await expect(this.page.locator('h1, .adm-layout__title')).toContainText(/Xử phạt|penalties/i);
  }

  async filterBySourceType(sourceType: 'REPORT' | 'DISPUTE' | 'TICKET' | 'CIRCUMVENTION' | 'DIRECT' | '') {
    await this.sourceTypeFilter.selectOption(sourceType);
    await this.page.waitForLoadState('networkidle');
  }

  async openIssuePenaltyModal() {
    await this.createPenaltyButton.click();
    await expect(this.issueModal.modal).toBeVisible();
  }

  getPenaltyRowBySource(sourceType: string, sourceId: number | string): Locator {
    return this.page.locator('table tbody tr').filter({
      hasText: `${sourceType} #${sourceId}`,
    });
  }

  async verifySourceBadge(sourceType: string, sourceId: number | string) {
    const row = this.getPenaltyRowBySource(sourceType, sourceId);
    await expect(row).toBeVisible({ timeout: 10000 });
    const badge = row.locator('span').filter({ hasText: `${sourceType} #${sourceId}` });
    await expect(badge).toBeVisible();
  }

  async clickOpenSourceCase(sourceType: string, sourceId: number | string) {
    const row = this.getPenaltyRowBySource(sourceType, sourceId);
    await expect(row).toBeVisible();
    const openCaseBtn = row.getByRole('button', { name: 'Mở case nguồn →' });
    await expect(openCaseBtn).toBeVisible();
    await openCaseBtn.click();
  }
}
