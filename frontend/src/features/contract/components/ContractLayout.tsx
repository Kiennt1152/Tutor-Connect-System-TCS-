import type { ReactNode } from 'react';

type ContractLayoutProps = {
  title: string;
  subtitle?: string;
  children: ReactNode;
};

export function ContractLayout({ title, subtitle, children }: ContractLayoutProps) {
  return (
    <div className="cnt-page">
      <header className="cnt-header">
        <div className="cnt-header__inner">
          <h1 className="cnt-header__title">{title}</h1>
          {subtitle && <p className="cnt-header__subtitle">{subtitle}</p>}
        </div>
      </header>
      <main className="cnt-main">{children}</main>
    </div>
  );
}
