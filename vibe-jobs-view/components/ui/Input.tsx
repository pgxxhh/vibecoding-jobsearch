import * as React from 'react';
import { cn } from '@/lib/cn';

export default function Input(props: React.InputHTMLAttributes<HTMLInputElement>) {
  const { className, ...rest } = props;
  return (
    <input
      className={cn(
        'h-10 w-full rounded-2xl border border-black/10 bg-white px-3 text-sm placeholder:text-gray-400 focus:outline-none focus:ring-4 focus:ring-brand-500/15',
        className,
      )}
      {...rest}
    />
  );
}
