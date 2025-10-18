'use client';

import { useI18n } from '@/shared/lib/i18n';

export default function AppFooter() {
  const { t } = useI18n();

  return (
    <footer className="mt-16">
      <div className="mx-auto w-full max-w-7xl px-6">
        <div className="vj-hr mb-6 h-px" />
        <div className="flex flex-col items-center justify-between gap-3 pb-12 text-sm text-gray-500 sm:flex-row">
          <span>{t('footer.builtWith')}</span>
          <span className="text-xs text-gray-400">© {new Date().getFullYear()} Elaine Jobs</span>
        </div>
      </div>
    </footer>
  );
}
