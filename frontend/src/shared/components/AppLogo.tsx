import { Link } from 'react-router-dom';
import { imageAssets } from '../../assets/images/ImageAssets';
import './AppLogo.css';

type AppLogoProps = {
  href?: string;
  className?: string;
  variant?: 'default' | 'compact';
};

export function AppLogo({ href = '/', className, variant = 'default' }: AppLogoProps) {
  const rootClass = ['app-logo', variant === 'compact' ? 'app-logo--compact' : '', className]
    .filter(Boolean)
    .join(' ');

  const content = (
    <>
      <img className="app-logo__image" src={imageAssets.logo} alt="" />
      <span className="app-logo__text">Tutor Connect System</span>
    </>
  );

  if (href.startsWith('http')) {
    return (
      <a className={rootClass} href={href} aria-label="Tutor Connect System">
        {content}
      </a>
    );
  }

  return (
    <Link className={rootClass} to={href} aria-label="Tutor Connect System">
      {content}
    </Link>
  );
}
