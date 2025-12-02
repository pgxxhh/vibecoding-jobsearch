export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="relative min-h-screen overflow-hidden bg-gradient-to-br from-slate-50 via-brand-50/30 to-white">
      <div className="pointer-events-none absolute -top-40 right-0 h-[500px] w-[500px] rounded-full bg-gradient-to-br from-brand-200/40 via-purple-200/30 to-transparent blur-3xl" aria-hidden />
      <div className="pointer-events-none absolute -bottom-32 -left-20 h-[400px] w-[400px] rounded-full bg-gradient-to-tr from-brand-100/30 via-pink-100/20 to-transparent blur-3xl" aria-hidden />
      <header className="relative z-10 flex items-center gap-3 px-8 pt-8">
        <img src="/assets/logo/vibe-jobs-logo.svg" alt="Elaine Jobs" className="h-9 w-auto" />
        <div className="flex flex-col">
          <span className="text-lg font-bold text-slate-800">Elaine Jobs</span>
          <span className="text-xs text-slate-500">build for my bb</span>
        </div>
      </header>
      <main className="relative z-10 flex min-h-[calc(100vh-6rem)] items-center justify-center px-4 py-12">
        {children}
      </main>
    </div>
  );
}
