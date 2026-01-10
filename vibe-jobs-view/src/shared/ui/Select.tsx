import * as React from 'react';
import { cn } from '@/shared/lib/cn';

export default function Select(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  const { className, children, ...rest } = props;
  return (
    <select
      className={cn(
        'h-10 w-full rounded-full border border-black/10 bg-white px-4 text-sm focus:outline-none focus:ring-4 focus:ring-black/10',
        className,
      )}
      {...rest}
    >
      {children}
    </select>
  );
}
