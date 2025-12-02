import AppHeader from '@/shared/components/AppHeader';
import AppFooter from '@/shared/components/AppFooter';

export default function SiteLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="relative min-h-screen bg-slate-50/30">
      <div className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
        <div className="absolute -top-[40%] -right-[20%] h-[800px] w-[800px] rounded-full bg-gradient-to-br from-brand-200/30 via-purple-200/20 to-transparent blur-3xl" />
        <div className="absolute -bottom-[20%] -left-[20%] h-[600px] w-[600px] rounded-full bg-gradient-to-tr from-brand-100/25 via-pink-100/15 to-transparent blur-3xl" />
        <img src="/assets/hero-bg.svg" alt="" className="h-full w-full object-cover opacity-50" />
      </div>
      <div className="relative z-0 flex min-h-screen flex-col">
        <AppHeader />
        <main className="container flex-1 py-8">{children}</main>
        <AppFooter />
      </div>
    </div>
  );
}
