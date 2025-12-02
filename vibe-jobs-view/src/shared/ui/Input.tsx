import * as React from 'react';
import { cn } from '@/shared/lib/cn';

export default function Input(props: React.InputHTMLAttributes<HTMLInputElement>) {
  const { className, ...rest } = props;
  return (
    <input
      className={cn(
        'h-11 w-full rounded-xl border border-slate-200 bg-white/80 px-4 text-sm text-slate-900 placeholder:text-slate-400 transition-all duration-200',
        'hover:border-slate-300 hover:bg-white',
        'focus:outline-none focus:border-brand-400 focus:bg-white focus:ring-4 focus:ring-brand-500/10',
        className,
      )}
      {...rest}
    />
  );
}
