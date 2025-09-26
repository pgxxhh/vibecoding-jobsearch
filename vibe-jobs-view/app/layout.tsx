
import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';
import '@/vibe-jobs-ui-pack/styles/overrides.css';
import Providers from './providers';
import AppHeader from '@/components/AppHeader';
import AppFooter from '@/components/AppFooter';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: 'Elaine Jobs — Minimal Template',
  description: 'Next.js + Tailwind + TanStack Query minimal jobs site template',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-Hans">
      <body className={`${inter.className} antialiased`}>
        <div className="pointer-events-none fixed inset-0 -z-10">
          <img src="/assets/hero-bg.svg" alt="" className="h-full w-full object-cover opacity-70" />
        </div>
        <Providers>
          <AppHeader />
          <main className="container py-6">{children}</main>
          <AppFooter />
        </Providers>
      </body>
    </html>
  );
}
