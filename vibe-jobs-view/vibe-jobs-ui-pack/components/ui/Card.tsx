
import * as React from 'react'; import clsx from 'clsx';
export default function Card({className,children,...rest}:React.HTMLAttributes<HTMLDivElement>){
  return <div className={clsx('rounded-3xl border border-black/10 bg-white shadow-brand-md',className)} {...rest}>{children}</div>;
}
