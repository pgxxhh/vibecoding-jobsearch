import AppHeader from '@/components/AppHeader';
import AppFooter from '@/components/AppFooter';

export default function SiteLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="relative min-h-screen bg-white">
      <div className="pointer-events-none fixed inset-0 -z-10">
        <img src="/assets/hero-bg.svg" alt="" className="h-full w-full object-cover opacity-70" />
      </div>
      <div className="relative z-0 flex min-h-screen flex-col">
        <AppHeader />
        <main className="container flex-1 py-6">{children}</main>
        <AppFooter />
      </div>
    </div>
  );
}
