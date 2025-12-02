import * as React from 'react';
import { cn } from '@/shared/lib/cn';

export default function Select(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  const { className, children, ...rest } = props;
  return (
    <select
      className={cn(
        'h-11 w-full rounded-xl border border-slate-200 bg-white/80 px-4 text-sm text-slate-900 transition-all duration-200 cursor-pointer appearance-none',
        'hover:border-slate-300 hover:bg-white',
        'focus:outline-none focus:border-brand-400 focus:bg-white focus:ring-4 focus:ring-brand-500/10',
        'bg-[url("data:image/svg+xml,%3csvg xmlns=%27http://www.w3.org/2000/svg%27 fill=%27none%27 viewBox=%270 0 20 20%27%3e%3cpath stroke=%27%236b7280%27 stroke-linecap=%27round%27 stroke-linejoin=%27round%27 stroke-width=%271.5%27 d=%27M6 8l4 4 4-4%27/%3e%3c/svg%3e")] bg-[length:1.25rem_1.25rem] bg-[right_0.75rem_center] bg-no-repeat pr-10',
        className,
      )}
      {...rest}
    >
      {children}
    </select>
  );
}
