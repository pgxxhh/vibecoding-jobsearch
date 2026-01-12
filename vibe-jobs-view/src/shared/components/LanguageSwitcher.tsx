'use client';

import { Select } from '@/shared/ui';
import { useI18n } from '@/shared/lib/i18n';

export default function LanguageSwitcher() {
  const { language, setLanguage, t } = useI18n();

  return (
    <label className="flex items-center gap-2 text-sm text-gray-600">
      <span className="sr-only">{t('header.languageSwitcherLabel')}</span>
      <Select
        className="h-9 w-[120px] rounded-full border-black/10 bg-white/90 text-xs text-gray-700"
        value={language}
        onChange={(event) => setLanguage(event.target.value as 'zh' | 'en')}
        aria-label={t('header.languageSwitcherLabel')}
      >
        <option value="zh">{t('language.zh')}</option>
        <option value="en">{t('language.en')}</option>
      </Select>
    </label>
  );
}
