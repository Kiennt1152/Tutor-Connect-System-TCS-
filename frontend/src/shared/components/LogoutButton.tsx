import { useAuth } from '../auth/AuthProvider';

type LogoutButtonProps = {
  className?: string;
};

/** Nut dang xuat thong nhat toan he thong (do, cung kich thuoc header). */
export function LogoutButton({ className }: LogoutButtonProps) {
  const { logout } = useAuth();

  const classes = ['tcs-btn', 'tcs-btn--ghost', 'tcs-btn--header', 'tcs-btn--logout', className]
    .filter(Boolean)
    .join(' ');

  return (
    <button type="button" className={classes} onClick={logout}>
      Đăng xuất
    </button>
  );
}
