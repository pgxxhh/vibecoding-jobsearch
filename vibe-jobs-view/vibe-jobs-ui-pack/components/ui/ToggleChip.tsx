
'use client';
import * as React from 'react'; import clsx from 'clsx';
type Props={active?:boolean;onToggle?:(v:boolean)=>void;children:React.ReactNode;};
export default function ToggleChip({active=false,onToggle,children}:Props){
  return <button onClick={()=>onToggle?.(!active)} className={clsx('inline-flex items-center rounded-full border px-3 h-8 text-sm transition',active?'bg-brand-600 text-white border-brand-600 shadow-brand-xs':'bg-white border-black/10 text-gray-700 hover:bg-black/5')} type="button">{children}</button>;
}
