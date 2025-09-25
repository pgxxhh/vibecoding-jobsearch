
import type { Metadata } from 'next';
import './globals.css';
import Providers from './providers';

export const metadata: Metadata = {
  title: 'Vibe Jobs — Minimal Template',
  description: 'Next.js + Tailwind + TanStack Query minimal jobs site template',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <Providers>
          <header className="border-b bg-white">
            <div className="container py-4 flex items-center justify-between">
              <div className="text-xl font-semibold">Vibe Jobs</div>
              <a href="https://example.com" className="btn btn-primary" target="_blank" rel="noreferrer">
                Demo Button
              </a>
            </div>
          </header>
          <main className="container py-6">{children}</main>
          <footer className="mt-12 py-10 text-center text-sm text-gray-500">
            Built with Next.js - Tailwind - TanStack Query
          </footer>
        </Providers>
      </body>
    </html>
  );
}
