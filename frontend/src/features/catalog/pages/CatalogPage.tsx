import { imageAssets } from '../../../assets/images/ImageAssets';
import '../../home/pages/HomePage.css';
import { CatalogPanel } from '../components/CatalogPanel';
import './CatalogPage.css';

function Header() {
  return (
    <header className="tcs-header">
      <div className="tcs-container tcs-header__inner">
        <a className="tcs-logo" href="/" aria-label="Tutor Connect System">
          <img className="tcs-logo__image" src={imageAssets.logo} alt="" />
          <span className="tcs-logo__text">Tutor Connect System</span>
        </a>
        <nav className="tcs-header__nav">
          <a href="/">Trang chủ</a>
          <a href="/catalog">Danh mục</a>
          <a href="/platform/users">Người dùng</a>
        </nav>
        <div className="tcs-header__actions">
          <a className="tcs-btn tcs-btn--ghost" href="/login">
            Đăng nhập
          </a>
          <a className="tcs-btn tcs-btn--primary" href="/register">
            Đăng ký
          </a>
        </div>
      </div>
    </header>
  );
}

export default function CatalogPage() {
  return (
    <>
      <Header />
      <CatalogPanel />
    </>
  );
}
