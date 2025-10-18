'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { ReactNode, useEffect } from 'react';
import { useAuth } from '@/shared/lib/auth';

const ALLOWED_EMAIL = '975022570yp@gmail.com';

function NavLink({ href, label }: { href: string; label: string }) {
  const pathname = usePathname();
  const active = pathname === href;
  return (
    <Link
      href={href}
      className={`block rounded-xl px-4 py-3 text-sm font-medium transition-all ${
        active 
          ? 'bg-gradient-to-r from-brand-500 to-brand-600 text-white shadow-brand-md' 
          : 'text-gray-600 hover:bg-white/70 hover:text-gray-900 hover:shadow-sm'
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
      <div className="relative min-h-screen bg-white">
        <div className="pointer-events-none fixed inset-0 -z-10">
          <img src="/assets/hero-bg.svg" alt="" className="h-full w-full object-cover opacity-70" />
        </div>
        <div className="flex min-h-screen items-center justify-center">
          <div className="rounded-3xl border border-white/60 bg-white/90 p-8 text-center shadow-brand-lg backdrop-blur-sm">
            <div className="space-y-3">
              <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-brand-200 border-t-brand-600"></div>
              <p className="text-lg font-semibold text-gray-900">正在验证身份...</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (user.email !== ALLOWED_EMAIL) {
    return (
      <div className="relative min-h-screen bg-white">
        <div className="pointer-events-none fixed inset-0 -z-10">
          <img src="/assets/hero-bg.svg" alt="" className="h-full w-full object-cover opacity-70" />
        </div>
        <div className="flex min-h-screen items-center justify-center p-6">
          <div className="max-w-md rounded-3xl border border-white/60 bg-white/90 p-8 text-center shadow-brand-lg backdrop-blur-sm">
            <div className="space-y-4">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
                <svg className="h-8 w-8 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
              </div>
              <h1 className="text-2xl font-semibold text-gray-900">无权限访问后台</h1>
              <p className="text-sm text-gray-600">当前账号 {user.email} 不在允许名单内。</p>
              <button
                onClick={() => logout()}
                className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] h-10 px-4 text-sm bg-brand-600 text-white hover:bg-brand-700 shadow-brand-sm focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/30"
              >
                退出登录
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen bg-white">
      {/* Background */}
      <div className="pointer-events-none fixed inset-0 -z-10">
        <img src="/assets/hero-bg.svg" alt="" className="h-full w-full object-cover opacity-70" />
      </div>
      
      <div className="relative z-0 flex min-h-screen flex-col">
        {/* Header */}
        <header className="sticky top-0 z-40 border-b border-white/30 bg-white/75 backdrop-blur transition">
          <div className="mx-auto flex w-full max-w-7xl items-center justify-between gap-6 px-6 py-4">
            <div className="flex items-center gap-3">
              <Link href="/" className="flex items-center gap-3">
                <img src="/assets/logo/vibe-jobs-logo.svg" alt="Elaine Jobs" className="h-10 w-auto" />
                <div className="hidden sm:flex sm:flex-col">
                  <span className="text-sm font-medium text-brand-700">Elaine Jobs</span>
                  <span className="text-xs text-gray-500">管理后台</span>
                </div>
              </Link>
            </div>
            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-600">欢迎回来，{user.email.split('@')[0]}</span>
              <button
                onClick={() => logout()}
                className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] h-10 px-4 text-sm border border-black/10 bg-white hover:bg-black/5 text-gray-900 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/15"
              >
                退出
              </button>
            </div>
          </div>
        </header>

        {/* Main Content */}
        <main className="container flex-1 py-6">
          <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
            {/* Sidebar Navigation */}
            <aside className="space-y-3">
              <div className="rounded-3xl border border-white/60 bg-white/90 p-6 shadow-brand-lg backdrop-blur-sm">
                <h2 className="mb-4 text-lg font-semibold text-gray-900">管理功能</h2>
                <nav className="space-y-2">
                  <NavLink href="/admin" label="📊 概览" />
                  <NavLink href="/admin/ingestion-settings" label="⚙️ 采集调度" />
                  <NavLink href="/admin/data-sources" label="🔗 数据源管理" />
                </nav>
              </div>
            </aside>

            {/* Content Area */}
            <section className="rounded-3xl border border-white/60 bg-white/90 p-6 shadow-brand-lg backdrop-blur-sm">
              {children}
            </section>
          </div>
        </main>
      </div>
    </div>
  );
}
