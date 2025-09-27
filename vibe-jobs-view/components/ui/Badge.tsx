import * as React from 'react';
import { cn } from '@/lib/cn';

type Props = React.HTMLAttributes<HTMLSpanElement> & {
  tone?: 'default' | 'brand' | 'muted';
};

export default function Badge({ tone = 'default', className, children, ...rest }: Props) {
  const tones = {
    default: 'border-black/10 bg-white text-gray-700',
    brand: 'border-brand-200/70 bg-brand-50 text-brand-700',
    muted: 'border-black/5 bg-gray-50 text-gray-600',
  }[tone];

  return (
    <span className={cn('inline-flex items-center rounded-full border px-2 py-0.5 text-xs', tones, className)} {...rest}>
      {children}
    </span>
  );
}
