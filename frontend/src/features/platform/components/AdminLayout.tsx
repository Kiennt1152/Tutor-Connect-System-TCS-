import type { ReactNode } from 'react';

type AdminLayoutProps = {
  title: string;
  subtitle?: string;
  children: ReactNode;
};

export function AdminLayout({ title, subtitle, children }: AdminLayoutProps) {
  return (
    <div className="adm-page">
      <header className="adm-header">
        <div className="adm-header__inner">
          <div>
            <h1 className="adm-header__title">{title}</h1>
            {subtitle && <p className="adm-header__subtitle">{subtitle}</p>}
          </div>
        </div>
      </header>
      <main className="adm-main">{children}</main>
    </div>
  );
}
