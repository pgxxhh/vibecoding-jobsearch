'use client';

import LanguageSwitcher from '@/shared/components/LanguageSwitcher';
import Link from 'next/link';
import { useAuth } from '@/modules/auth/hooks/useAuth';
import { useState, useRef, useEffect } from 'react';

export default function AppHeader() {
  const { user, loading, logout } = useAuth();
  const [showUserMenu, setShowUserMenu] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Close menu when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setShowUserMenu(false);
      }
    }

    if (showUserMenu) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [showUserMenu]);

  const handleLogout = async () => {
    setShowUserMenu(false);
    await logout();
  };

  return (
    <header className="sticky top-0 z-40 border-b border-black/10 bg-white/90 backdrop-blur">
      <div className="mx-auto flex w-full max-w-7xl items-center justify-between gap-6 px-6 py-4">
        <div className="flex items-center gap-3">
          <img src="/assets/logo/vibe-jobs-logo.svg" alt="Elaine Jobs" className="h-10 w-auto" />
          <div className="hidden sm:flex sm:flex-col" aria-hidden="true" />
        </div>
        <div className="flex items-center gap-3">
          {loading ? (
            // Loading state
            <div className="h-8 w-8 animate-pulse rounded-full bg-gray-200"></div>
          ) : user ? (
            // Logged in state - show user menu
            <div className="relative" ref={menuRef}>
              <button
                onClick={() => setShowUserMenu(!showUserMenu)}
                className="flex h-8 w-8 items-center justify-center rounded-full border border-black/10 bg-black text-sm font-semibold text-white transition hover:bg-black/90 focus:outline-none focus:ring-2 focus:ring-black/10"
                title={`已登录: ${user.email}`}
              >
                <svg
                  className="h-4 w-4"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    fillRule="evenodd"
                    d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z"
                    clipRule="evenodd"
                  />
                </svg>
              </button>
              
              {showUserMenu && (
                <div className="absolute right-0 top-full mt-2 w-64 rounded-2xl border border-black/10 bg-white py-2 shadow-sm">
                  <div className="border-b border-gray-100 px-4 py-3">
                    <p className="text-sm font-medium text-gray-900">已登录</p>
                    <p className="text-xs text-gray-500 truncate">{user.email}</p>
                  </div>
                  <button
                    onClick={handleLogout}
                    className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 focus:bg-gray-50 focus:outline-none"
                  >
                    退出登录
                  </button>
                </div>
              )}
            </div>
          ) : (
            // Not logged in - show login button
            <>
              <Link
                href="/login"
                className="hidden rounded-full border border-black/10 bg-black px-4 py-2 text-sm font-semibold text-white transition hover:bg-black/90 sm:inline"
              >
                登录 / 注册
              </Link>
              <Link
                href="/login"
                className="flex h-8 w-8 items-center justify-center rounded-full border border-black/20 text-black transition hover:bg-black/5 sm:hidden"
                title="登录 / 注册"
              >
                <svg
                  className="h-4 w-4"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                  />
                </svg>
              </Link>
            </>
          )}
          <LanguageSwitcher />
        </div>
      </div>
    </header>
  );
}
