import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

export class AiAssistantPage extends BasePage {
  readonly inputField: Locator;
  readonly sendButton: Locator;
  readonly messageRows: Locator;
  readonly assistantBubbles: Locator;
  readonly typingIndicator: Locator;
  readonly tutorCards: Locator;
  readonly copyButtons: Locator;

  constructor(page: Page) {
    super(page);
    this.inputField = page.locator('.ai-input-field');
    this.sendButton = page.locator('.ai-input-submit, .ai-input-wrapper button[type="submit"]');
    this.messageRows = page.locator('.ai-message-row');
    this.assistantBubbles = page.locator('.ai-message-row.assistant .ai-bubble');
    this.typingIndicator = page.locator('.ai-message-row.assistant', { hasText: 'AI đang phân tích câu hỏi...' });
    this.tutorCards = page.locator('.ai-rich-cards .ai-mini-card');
    this.copyButtons = page.locator('.ai-action-btn[title="Copy"]');
  }

  async open(): Promise<void> {
    await this.navigate('/ai-assistant');
    await this.waitForLoad();
  }

  async askQuestion(question: string): Promise<number> {
    await this.inputField.waitFor({ state: 'visible', timeout: 8000 });
    await this.inputField.fill(question);
    
    const startTime = Date.now();
    await this.page.keyboard.press('Enter');
    return startTime;
  }

  async waitForTypingIndicator(): Promise<void> {
    await expect(this.typingIndicator).toBeVisible({ timeout: 5000 });
  }

  async waitForAssistantResponse(timeout = 15000): Promise<string> {
    const lastBubble = this.assistantBubbles.last();
    await expect(lastBubble).toBeVisible({ timeout });
    return (await lastBubble.textContent()) || '';
  }

  async verifyTutorCardPresent(minCount = 1): Promise<void> {
    await expect(this.tutorCards.first()).toBeVisible({ timeout: 10000 });
    const count = await this.tutorCards.count();
    expect(count).toBeGreaterThanOrEqual(minCount);
  }

  async clickCopyButton(): Promise<void> {
    await this.copyButtons.last().waitFor({ state: 'visible', timeout: 5000 });
    await this.copyButtons.last().click();
  }
}
