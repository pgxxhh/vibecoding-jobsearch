
import * as React from 'react'; import clsx from 'clsx';
export default function Input(props:React.InputHTMLAttributes<HTMLInputElement>){
  const {className,...rest}=props;
  return <input className={clsx('h-10 w-full rounded-2xl border border-black/10 bg-white px-3 text-sm placeholder:text-gray-400 focus:outline-none focus:ring-4 focus:ring-brand-500/15',className)} {...rest}/>;
}
