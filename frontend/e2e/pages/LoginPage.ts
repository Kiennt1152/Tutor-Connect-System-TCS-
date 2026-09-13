import { type Page, type Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

export class LoginPage extends BasePage {
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly submitButton: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page) {
    super(page);
    this.emailInput = page.locator('input[type="email"], input[placeholder="ban@email.com"]');
    this.passwordInput = page.locator('input[type="password"]');
    this.submitButton = page.locator('button[type="submit"]').filter({ hasText: 'Đăng nhập' });
    this.errorMessage = page.locator('.reg-alert--error');
  }

  async open() {
    await this.goto('/login');
    await expect(this.emailInput).toBeVisible();
  }

  async login(email: string, pass: string) {
    await this.open();
    await this.emailInput.fill(email);
    await this.passwordInput.fill(pass);
    await this.submitButton.click();
    // Chờ điều hướng khỏi trang login
    await expect(this.page).not.toHaveURL(/\/login/);
  }
}
