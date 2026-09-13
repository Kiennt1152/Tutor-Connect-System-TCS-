import { Page } from '@playwright/test';

export class NfrMetricsHelper {
  /**
   * Tính toán bách phân vị (Percentile) từ mảng độ trễ (latency/duration).
   * @param durations mảng các giá trị thời gian phản hồi (ms)
   * @param percentile giá trị bách phân vị cần tính (vd: 95 cho p95, 99 cho p99)
   */
  static calculatePercentile(durations: number[], percentile: number): number {
    if (durations.length === 0) return 0;
    const sorted = [...durations].sort((a, b) => a - b);
    const index = Math.ceil((percentile / 100) * sorted.length) - 1;
    return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
  }

  /**
   * Tính thời gian phản hồi trung bình (Average).
   */
  static calculateAverage(durations: number[]): number {
    if (durations.length === 0) return 0;
    const sum = durations.reduce((acc, val) => acc + val, 0);
    return Math.round(sum / durations.length);
  }

  /**
   * Kiểm tra giao diện xem có bị lỗi vỡ layout hoặc tràn màn hình ngang (Horizontal Overflow) hay không.
   * Rất quan trọng khi kiểm thử giao diện Responsive trên màn hình di động hẹp (375px).
   */
  static async checkNoHorizontalOverflow(page: Page): Promise<boolean> {
    const isOverflowing = await page.evaluate(() => {
      const scrollW = document.documentElement.scrollWidth;
      const clientW = document.documentElement.clientWidth;
      return scrollW > clientW;
    });
    return !isOverflowing;
  }
}
