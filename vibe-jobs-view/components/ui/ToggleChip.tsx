'use client';

import * as React from 'react';
import { cn } from '@/lib/cn';

type Props = {
  active?: boolean;
  onToggle?: (value: boolean) => void;
  children: React.ReactNode;
};

export default function ToggleChip({ active = false, onToggle, children }: Props) {
  return (
    <button
      type="button"
      onClick={() => onToggle?.(!active)}
      className={cn(
        'inline-flex h-8 items-center rounded-full border px-3 text-sm transition',
        active
          ? 'border-brand-600 bg-brand-600 text-white shadow-brand-xs'
          : 'border-black/10 bg-white text-gray-700 hover:bg-black/5 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/10',
      )}
    >
      {children}
    </button>
  );
}
