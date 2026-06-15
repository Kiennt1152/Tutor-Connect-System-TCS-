import type { ReactNode, SVGProps } from 'react';

type IconProps = SVGProps<SVGSVGElement> & { size?: number };

/**
 * Lucide-style stroke icons rendered inline so no extra dependency is required.
 * Icon names follow the spec's iconography table (Part 1.5).
 */
function Base({ size = 24, children, ...props }: IconProps & { children: ReactNode }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      {children}
    </svg>
  );
}

export const GraduationCap = (p: IconProps) => (
  <Base {...p}>
    <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
    <path d="M6 12v5c3 3 9 3 12 0v-5" />
  </Base>
);

export const Search = (p: IconProps) => (
  <Base {...p}>
    <circle cx="11" cy="11" r="8" />
    <path d="m21 21-4.3-4.3" />
  </Base>
);

export const ShieldCheck = (p: IconProps) => (
  <Base {...p}>
    <path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z" />
    <path d="m9 12 2 2 4-4" />
  </Base>
);

export const CalendarDays = (p: IconProps) => (
  <Base {...p}>
    <path d="M8 2v4M16 2v4" />
    <rect width="18" height="18" x="3" y="4" rx="2" />
    <path d="M3 10h18M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01M16 18h.01" />
  </Base>
);

export const Wallet = (p: IconProps) => (
  <Base {...p}>
    <path d="M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1" />
    <path d="M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4" />
  </Base>
);

export const Bot = (p: IconProps) => (
  <Base {...p}>
    <path d="M12 8V4H8" />
    <rect width="16" height="12" x="4" y="8" rx="2" />
    <path d="M2 14h2M20 14h2M15 13v2M9 13v2" />
  </Base>
);

export const Star = (p: IconProps) => (
  <Base {...p}>
    <path d="M11.5 2.7a.55.55 0 0 1 1 0l2.6 5.3 5.8.8a.55.55 0 0 1 .3.95l-4.2 4.1 1 5.8a.55.55 0 0 1-.8.6L12 17.6l-5.2 2.7a.55.55 0 0 1-.8-.6l1-5.8-4.2-4.1a.55.55 0 0 1 .3-.95l5.8-.8z" />
  </Base>
);

export const Users = (p: IconProps) => (
  <Base {...p}>
    <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
    <circle cx="9" cy="7" r="4" />
    <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
  </Base>
);

export const MessageSquare = (p: IconProps) => (
  <Base {...p}>
    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
  </Base>
);

export const BadgeCheck = (p: IconProps) => (
  <Base {...p}>
    <path d="M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z" />
    <path d="m9 12 2 2 4-4" />
  </Base>
);

export const ArrowRight = (p: IconProps) => (
  <Base {...p}>
    <path d="M5 12h14M12 5l7 7-7 7" />
  </Base>
);

export const Menu = (p: IconProps) => (
  <Base {...p}>
    <path d="M4 12h16M4 6h16M4 18h16" />
  </Base>
);

export const X = (p: IconProps) => (
  <Base {...p}>
    <path d="M18 6 6 18M6 6l12 12" />
  </Base>
);

export const ClipboardList = (p: IconProps) => (
  <Base {...p}>
    <rect width="8" height="4" x="8" y="2" rx="1" />
    <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
    <path d="M12 11h4M12 16h4M8 11h.01M8 16h.01" />
  </Base>
);

export const Building2 = (p: IconProps) => (
  <Base {...p}>
    <path d="M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18Z" />
    <path d="M6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2" />
    <path d="M10 6h4M10 10h4M10 14h4M10 18h4" />
  </Base>
);
