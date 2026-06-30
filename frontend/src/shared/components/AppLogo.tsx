import { Link } from 'react-router-dom';
import { imageAssets } from '../../assets/images/ImageAssets';
import './AppLogo.css';

export const APP_NAME = 'Tutor Connect System';

type AppLogoProps = {
  className?: string;
};

export function AppLogo({ className }: AppLogoProps) {
  return (
    <Link
      className={className ? `tcs-logo ${className}` : 'tcs-logo'}
      to="/"
      aria-label={APP_NAME}
    >
      <img className="tcs-logo__image" src={imageAssets.logo} alt="" />
      <span className="tcs-logo__text">{APP_NAME}</span>
    </Link>
  );
}
