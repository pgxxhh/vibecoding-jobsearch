'use client';

import * as React from 'react';
import { cn } from '@/lib/cn';

type Props = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'outline' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
};

export default function Button({
  variant = 'primary',
  size = 'md',
  leftIcon,
  rightIcon,
  className,
  children,
  type,
  ...rest
}: Props) {
  const base = 'inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] disabled:cursor-not-allowed disabled:opacity-60 disabled:active:scale-100';
  const sizes = {
    sm: 'h-8 px-3 text-sm',
    md: 'h-10 px-4 text-sm',
    lg: 'h-12 px-5 text-base font-medium',
  }[size];
  const variants = {
    primary: 'bg-brand-600 text-white hover:bg-brand-700 shadow-brand-sm focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/30',
    outline: 'border border-black/10 bg-white hover:bg-black/5 text-gray-900 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/15',
    ghost: 'text-gray-700 hover:bg-black/5 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/10',
  }[variant];

  return (
    <button type={type ?? 'button'} className={cn(base, sizes, variants, className)} {...rest}>
      {leftIcon && <span className="inline-flex -ml-1">{leftIcon}</span>}
      <span>{children}</span>
      {rightIcon && <span className="-mr-1">{rightIcon}</span>}
    </button>
  );
}
