'use client';

import LanguageSwitcher from '@/components/LanguageSwitcher';
import Link from 'next/link';

export default function AppHeader() {
  return (
    <header className="sticky top-0 z-40 border-b border-white/30 bg-white/75 backdrop-blur transition">
      <div className="mx-auto flex w-full max-w-7xl items-center justify-between gap-6 px-6 py-4">
        <div className="flex items-center gap-3">
          <img src="/assets/logo/vibe-jobs-logo.svg" alt="Elaine Jobs" className="h-10 w-auto" />
          <div className="hidden sm:flex sm:flex-col">
            <span className="text-sm font-medium text-brand-700">Elaine Jobs</span>
            <span className="text-xs text-gray-500">build for my bb</span>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <Link
            href="/login"
            className="hidden rounded-full bg-gradient-to-r from-pink-500 to-purple-500 px-4 py-2 text-sm font-semibold text-white shadow-md shadow-pink-200 transition hover:from-pink-400 hover:to-purple-400 sm:inline"
          >
            登录 / 注册
          </Link>
          <LanguageSwitcher />
        </div>
      </div>
    </header>
  );
}
