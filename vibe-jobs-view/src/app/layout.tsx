import type { Metadata } from 'next';
import './globals.css';
import '@/vibe-jobs-ui-pack/styles/overrides.css';
import Providers from './providers';

export const metadata: Metadata = {
  title: 'Elaine Jobs — Minimal Template',
  description: 'Next.js + Tailwind + TanStack Query minimal jobs site template',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-Hans">
      <body className="font-sans antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
