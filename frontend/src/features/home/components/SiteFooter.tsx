import { AppLogo } from '../../../shared/components/AppLogo';
import { FOOTER_LINKS } from '../config/homeContent';
import '../pages/HomePage.css';

export function SiteFooter() {
  return (
    <footer className="tcs-footer">
      <div className="tcs-container">
        <div className="tcs-footer__grid">
          <div className="tcs-footer__brand">
            <AppLogo href="/" variant="compact" />
            <p className="tcs-footer__tagline">
              Nền tảng kết nối gia sư — học viên — trung tâm với quy trình minh bạch và thanh toán an
              toàn.
            </p>
          </div>
          {FOOTER_LINKS.map((group) => (
            <div key={group.title} className="tcs-footer__col">
              <h3 className="tcs-footer__heading">{group.title}</h3>
              <ul className="tcs-footer__links">
                {group.links.map((link) => (
                  <li key={link.label}>
                    <a href={link.href}>{link.label}</a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
        <div className="tcs-footer__bottom">
          <span>© {new Date().getFullYear()} Tutor Connect System</span>
          <span className="tcs-footer__muted">Kết nối gia sư · học viên · trung tâm</span>
        </div>
      </div>
    </footer>
  );
}
