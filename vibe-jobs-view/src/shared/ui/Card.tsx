import * as React from 'react';
import { cn } from '@/shared/lib/cn';

export default function Card({ className, children, ...rest }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn('rounded-3xl border border-slate-100 bg-white/95 shadow-glass backdrop-blur-sm transition-all duration-300', className)} {...rest}>
      {children}
    </div>
  );
}
