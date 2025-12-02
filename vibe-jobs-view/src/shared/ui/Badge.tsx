import * as React from 'react';
import { cn } from '@/shared/lib/cn';

type Props = React.HTMLAttributes<HTMLSpanElement> & {
  tone?: 'default' | 'brand' | 'muted';
};

export default function Badge({ tone = 'default', className, children, ...rest }: Props) {
  const tones = {
    default: 'border-slate-200/80 bg-white text-slate-600',
    brand: 'border-brand-200/60 bg-gradient-to-r from-brand-50 to-brand-100/50 text-brand-700',
    muted: 'border-slate-100 bg-slate-50/80 text-slate-500',
  }[tone];

  return (
    <span className={cn('inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-medium transition-colors duration-200', tones, className)} {...rest}>
      {children}
    </span>
  );
}
