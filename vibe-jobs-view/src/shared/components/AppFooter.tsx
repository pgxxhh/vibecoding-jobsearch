'use client';

import { useI18n } from '@/shared/lib/i18n';

export default function AppFooter() {
  const { t } = useI18n();

  return (
    <footer className="mt-20 pb-8">
      <div className="mx-auto w-full max-w-7xl px-6">
        <div className="h-px bg-gradient-to-r from-transparent via-slate-200 to-transparent mb-8" />
        <div className="flex flex-col items-center justify-between gap-4 text-sm sm:flex-row">
          <span className="text-slate-500">{t('footer.builtWith')}</span>
          <span className="text-xs text-slate-400">© {new Date().getFullYear()} Elaine Jobs</span>
        </div>
      </div>
    </footer>
  );
}
