import { createBrowserRouter, Link, RouterProvider } from 'react-router-dom';
import PublicLayout from '../layouts/PublicLayout';
import HomePage from '../../features/marketplace/pages/HomePage';

/** Placeholder cho các trang chưa dựng — giữ navbar/footer hoạt động. */
function ComingSoon({ title }: { title: string }) {
  return (
    <div
      style={{
        maxWidth: 480,
        margin: '0 auto',
        padding: '96px 24px',
        textAlign: 'center',
      }}
    >
      <h1>{title}</h1>
      <p style={{ color: 'var(--text-secondary)', marginTop: 8 }}>
        Trang này đang được phát triển.
      </p>
      <p style={{ marginTop: 24 }}>
        <Link to="/">← Về trang chủ</Link>
      </p>
    </div>
  );
}

const router = createBrowserRouter([
  {
    path: '/',
    element: <PublicLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'tutors', element: <ComingSoon title="Tìm gia sư" /> },
      { path: 'about', element: <ComingSoon title="Về chúng tôi" /> },
      { path: 'login', element: <ComingSoon title="Đăng nhập" /> },
      { path: 'register', element: <ComingSoon title="Đăng ký" /> },
      { path: '*', element: <ComingSoon title="Không tìm thấy trang" /> },
    ],
  },
]);

function AppRouter() {
  return <RouterProvider router={router} />;
}

export default AppRouter;
