'use client';

import * as React from 'react';
import { cn } from '@/shared/lib/cn';

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
  const base = 'inline-flex items-center justify-center gap-2 font-medium transition-all duration-200 ease-out active:scale-[.97] disabled:cursor-not-allowed disabled:opacity-50 disabled:active:scale-100 select-none';
  const sizes = {
    sm: 'h-9 px-3.5 text-sm rounded-xl',
    md: 'h-11 px-5 text-sm rounded-2xl',
    lg: 'h-13 px-6 text-base rounded-2xl',
  }[size];
  const variants = {
    primary: 'bg-gradient-to-r from-brand-600 to-brand-500 text-white shadow-brand-md hover:shadow-brand-lg hover:from-brand-500 hover:to-brand-400 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/30',
    outline: 'border border-slate-200 bg-white hover:bg-slate-50 hover:border-slate-300 text-slate-700 shadow-sm focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/15',
    ghost: 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/80 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/10',
  }[variant];

  return (
    <button type={type ?? 'button'} className={cn(base, sizes, variants, className)} {...rest}>
      {leftIcon && <span className="inline-flex -ml-0.5">{leftIcon}</span>}
      <span>{children}</span>
      {rightIcon && <span className="-mr-0.5">{rightIcon}</span>}
    </button>
  );
}
