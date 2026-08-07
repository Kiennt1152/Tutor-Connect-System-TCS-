import { SiteHeader } from './SiteHeader';

/**
 * Giữ tên cũ để các trang không phải đổi import. Header đã được gộp về một
 * bản chuẩn theo role duy nhất là {@link SiteHeader} (đầy đủ chuông thông báo,
 * ví, hợp đồng, tin nhắn, xác minh… theo từng role).
 */
export function VerificationHeader() {
  return <SiteHeader />;
}
