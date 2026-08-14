import { NavLink } from 'react-router-dom';
import { APP_ROUTES } from '../../../shared/constants/routes';
import './CenterSidebar.css';

interface CenterSideItem {
  readonly to: string;
  readonly label: string;
  readonly end?: boolean;
}

const ITEMS: readonly CenterSideItem[] = [
  { to: '/center', label: 'Lớp học Trung Tâm', end: true },
  { to: '/center/requests', label: 'Yêu cầu mở lớp' },
  { to: '/center/recruitment', label: 'Tin tuyển gia sư' },
  { to: '/center/tutors', label: 'Gia sư của trung tâm' },
  { to: '/center/stats', label: 'Thống kê' },
  { to: '/center/contract-templates', label: 'Mẫu hợp đồng' },
  { to: '/center/schedule', label: 'Lịch hôm nay' },
  { to: '/center/reschedules', label: 'Yêu cầu đổi lịch' },
  { to: APP_ROUTES.centerReports, label: 'Báo cáo & tranh chấp' },
];

/** Thanh điều hướng bên trái cho khu vực "Quản lý trung tâm" (giống sidebar trang admin). */
export function CenterSidebar() {
  return (
    <aside className="center-side">
      <p className="center-side__title">Quản lý trung tâm</p>
      <nav className="center-side__nav" aria-label="Quản lý trung tâm">
        {ITEMS.map((it) => (
          <NavLink
            key={it.to}
            to={it.to}
            end={it.end}
            className={({ isActive }) =>
              `center-side__link${isActive ? ' is-active' : ''}`
            }
          >
            {it.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
