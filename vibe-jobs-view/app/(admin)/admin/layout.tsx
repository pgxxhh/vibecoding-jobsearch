'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { ReactNode, useEffect } from 'react';
import { useAuth } from '@/lib/auth';

const ALLOWED_EMAIL = '975022570yp@gmail.com';

function NavLink({ href, label }: { href: string; label: string }) {
  const pathname = usePathname();
  const active = pathname === href;
  return (
    <Link
      href={href}
      className={`rounded-md px-3 py-2 text-sm font-medium transition-colors ${
        active ? 'bg-white/10 text-white' : 'text-white/70 hover:text-white hover:bg-white/10'
      }`}
    >
      {label}
    </Link>
  );
}

export default function AdminLayout({ children }: { children: ReactNode }) {
  const { user, loading, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!loading && !user) {
      const redirectTo = pathname?.startsWith('/admin') ? pathname : '/admin';
      router.replace(`/login?redirect=${encodeURIComponent(redirectTo)}`);
    }
  }, [loading, pathname, router, user]);

  if (loading || !user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 text-white flex items-center justify-center">
        <div className="text-center">
          <p className="text-lg font-semibold">正在验证身份...</p>
        </div>
      </div>
    );
  }

  if (user.email !== ALLOWED_EMAIL) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 text-white flex items-center justify-center">
        <div className="max-w-md text-center space-y-4">
          <h1 className="text-2xl font-semibold">无权限访问后台</h1>
          <p className="text-sm text-white/70">当前账号 {user.email} 不在允许名单内。</p>
          <button
            onClick={() => logout()}
            className="rounded-md bg-white/10 px-4 py-2 text-sm font-medium hover:bg-white/20"
          >
            退出登录
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 text-white">
      <header className="border-b border-white/10 bg-white/5 backdrop-blur">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <div>
            <h1 className="text-xl font-semibold">Elaine Jobs 管理后台</h1>
            <p className="text-xs text-white/60">欢迎回来，{user.email}</p>
          </div>
          <button
            onClick={() => logout()}
            className="rounded-md bg-white/10 px-3 py-2 text-sm font-medium hover:bg-white/20"
          >
            退出
          </button>
        </div>
      </header>
      <div className="mx-auto grid max-w-5xl grid-cols-1 gap-6 px-6 py-10 lg:grid-cols-[220px_1fr]">
        <nav className="space-y-2 rounded-xl border border-white/10 bg-white/5 p-4 shadow-lg shadow-black/20">
          <NavLink href="/admin" label="概览" />
          <NavLink href="/admin/ingestion-settings" label="采集调度" />
          <NavLink href="/admin/data-sources" label="数据源管理" />
        </nav>
        <main className="rounded-xl border border-white/10 bg-white/5 p-6 shadow-lg shadow-black/20">
          {children}
        </main>
      </div>
    </div>
  );
}
