import { Component, type ErrorInfo, type ReactNode } from 'react';
import { APP_ROUTES } from '../constants/routes';

type Props = { readonly children: ReactNode };
type State = { readonly error: Error | null };

export class ErrorBoundary extends Component<Props, State> {
  override state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  override componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('[ErrorBoundary] Uncaught error:', error, info);
  }

  override render(): ReactNode {
    if (this.state.error) {
      return (
        <div
          style={{
            padding: '48px 24px',
            maxWidth: 720,
            margin: '0 auto',
            fontFamily: 'system-ui, sans-serif',
            color: '#0f172a',
          }}
        >
          <h1 style={{ margin: '0 0 8px', fontSize: 24, color: '#c62828' }}>
            Đã xảy ra lỗi khi hiển thị trang
          </h1>
          <p
            style={{
              margin: '0 0 16px',
              color: '#64748b',
              fontSize: 14,
            }}
          >
            Vui lòng mở DevTools Console (F12) để xem chi tiết và báo lại cho dev.
          </p>
          <pre
            style={{
              background: '#f1f5f9',
              border: '1px solid #e2e8f0',
              borderRadius: 8,
              padding: 16,
              fontSize: 12,
              overflow: 'auto',
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
            }}
          >
            {this.state.error.name}: {this.state.error.message}
          </pre>
          <a
            href={APP_ROUTES.home}
            style={{
              display: 'inline-block',
              marginTop: 16,
              padding: '10px 18px',
              background: '#ea580c',
              color: '#fff',
              borderRadius: 8,
              textDecoration: 'none',
              fontWeight: 600,
            }}
          >
            Về trang chủ
          </a>
        </div>
      );
    }
    return this.props.children;
  }
}