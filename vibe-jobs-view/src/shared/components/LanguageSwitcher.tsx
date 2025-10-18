'use client';

import { Select } from '@/shared/ui';
import { useI18n } from '@/shared/lib/i18n';

export default function LanguageSwitcher() {
  const { language, setLanguage, t } = useI18n();

  return (
    <label className="flex items-center gap-2 text-sm text-gray-500">
      <span className="sr-only">{t('header.languageSwitcherLabel')}</span>
      <Select
        className="w-[140px]"
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
