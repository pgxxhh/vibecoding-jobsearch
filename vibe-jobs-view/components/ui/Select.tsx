import * as React from 'react';
import { cn } from '@/lib/cn';

export default function Select(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  const { className, children, ...rest } = props;
  return (
    <select
      className={cn(
        'h-10 w-full rounded-2xl border border-black/10 bg-white px-3 text-sm focus:outline-none focus:ring-4 focus:ring-brand-500/15',
        className,
      )}
      {...rest}
    >
      {children}
    </select>
  );
}
