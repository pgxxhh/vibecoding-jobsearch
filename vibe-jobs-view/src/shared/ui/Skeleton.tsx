import * as React from 'react';

export default function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse rounded-xl bg-gradient-to-r from-slate-100 via-slate-200/70 to-slate-100 bg-[length:200%_100%] ${className}`} />;
}
