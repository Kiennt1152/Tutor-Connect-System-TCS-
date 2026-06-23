import type { ButtonHTMLAttributes, ReactNode } from 'react';

type SharedButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  children: ReactNode;
};

export function SharedButton({ children, ...props }: SharedButtonProps) {
  return <button type="button" {...props}>{children}</button>;
}
