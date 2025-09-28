export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="relative min-h-screen overflow-hidden bg-gradient-to-b from-[#ffe6f1] via-[#ffeef7] to-white">
      <div className="pointer-events-none absolute -top-32 right-14 h-72 w-72 rounded-full bg-pink-300/30 blur-3xl" aria-hidden />
      <div className="pointer-events-none absolute -bottom-24 left-10 h-96 w-96 rounded-full bg-fuchsia-300/30 blur-3xl" aria-hidden />
      <header className="relative z-10 flex items-center gap-3 px-8 pt-10">
        <img src="/assets/logo/vibe-jobs-logo.svg" alt="Elaine Jobs" className="h-10 w-auto" />
        <div className="flex flex-col">
          <span className="text-xl font-semibold text-pink-600">Elaine Jobs</span>
          <span className="text-xs text-pink-500">build for my bb</span>
        </div>
      </header>
      <main className="relative z-10 flex min-h-[calc(100vh-6rem)] items-center justify-center px-4 py-12">
        {children}
      </main>
    </div>
  );
}
