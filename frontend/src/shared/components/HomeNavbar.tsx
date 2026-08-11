import { SiteHeader } from './SiteHeader';

/**
 * Giữ tên cũ để các trang không phải đổi import. Header đã được gộp về một
 * bản chuẩn theo role duy nhất là {@link SiteHeader}.
 */
export function HomeNavbar() {
  return <SiteHeader />;

}
