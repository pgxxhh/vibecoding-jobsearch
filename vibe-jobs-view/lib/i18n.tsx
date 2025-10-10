'use client';

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

type Language = 'zh' | 'en';

type TranslationDict = Record<string, string>;

type Translations = Record<Language, TranslationDict>;

const translations: Translations = {
  zh: {
    'header.languageSwitcherLabel': '切换语言',
    'language.zh': '中文',
    'language.en': 'English',
    'footer.builtWith': '基于 Next.js · Tailwind · TanStack Query 构建',
    'hero.badge': '每日上新',
    'hero.title': '发现下一份令人心动的职位',
    'hero.subtitle': '使用智能筛选快速锁定适合你的机会。',
    'subscription.title': '创建订阅提醒',
    'subscription.description': '创建一个订阅，第一时间收到最新职位推送。你可以在个人中心管理订阅。',
    'subscription.conditionsLabel': '搜索条件：',
    'subscription.keyword': '关键词：{value}',
    'subscription.company': '公司：{value}',
    'subscription.location': '地点：{value}',
    'subscription.level': '级别：{value}',
    'actions.cancel': '取消',
    'actions.confirmSubscription': '确认订阅',
    'actions.reset': '重置',
    'actions.search': '搜索',
    'actions.retry': '重试',
    'actions.createSubscription': '创建订阅提醒',
    'actions.previous': '上一页',
    'actions.next': '下一页',
    'filters.title': '筛选职位',
    'filters.company': '公司',
    'filters.level': '级别',
    'filters.remote': '远程',
    'filters.remote.true': '远程',
    'filters.remote.false': '非远程',
    'filters.salaryMin': '薪资下限',
    'filters.salaryPlaceholder': '最低薪资',
    'filters.datePosted': '发布日期',
    'filters.dateOptions.1': '1 天内',
    'filters.dateOptions.3': '3 天内',
    'filters.dateOptions.7': '7 天内',
    'filters.dateOptions.30': '30 天内',
    'filters.open': '筛选',
    'filters.apply': '应用',
    'filters.cancel': '取消',
    'forms.any': '不限',
    'jobLevels.junior': '初级 (Junior)',
    'jobLevels.mid': '中级 (Mid)',
    'jobLevels.senior': '高级 (Senior)',
    'jobLevels.staff': '资深 (Staff)',
    'jobLevels.principal': '专家 (Principal)',
    'search.keywordPlaceholder': '关键词 (如: backend, Java)',
    'search.locationPlaceholder': '地点',
    'search.loading': '加载中…',
    'search.results': '{count} 条结果',
    'search.refreshing': '刷新中',
    'search.page': '第 {page} 页',
    'errors.generic': '错误：{message}',
    'errors.fetchJobs': '职位数据获取失败，请稍后重试',
    'jobDetail.empty': '请选择左侧职位查看详情',
    'jobDetail.summary': '摘要：',
    'jobDetail.summaryPlaceholder': '智能摘要生成中…',
    'jobDetail.skills': '核心技能：',
    'jobDetail.skillsPlaceholder': '技能提取中…',
    'jobDetail.highlights': '亮点：',
    'jobDetail.highlightsPlaceholder': '亮点生成中…',
    'jobDetail.tags': '标签：',
    'jobDetail.description': '描述：',
    'jobDetail.noDescription': '无详细描述',
    'jobDetail.error': '职位详情获取失败，请稍后重试',
    'jobDetail.refreshing': '正在刷新职位详情…',
    'jobDetail.viewOriginal': '查看原职位',
    'jobCard.summaryPlaceholder': '摘要生成中…',
    'jobCard.skillsPlaceholder': '技能整理中',
    'jobCard.highlightsPlaceholder': '亮点敬请期待',
  },
  en: {
    'header.languageSwitcherLabel': 'Switch language',
    'language.zh': '中文',
    'language.en': 'English',
    'footer.builtWith': 'Built with Next.js · Tailwind · TanStack Query',
    'hero.badge': 'Fresh drops',
    'hero.title': 'Find your next delightful gig',
    'hero.subtitle': 'Use precise filters to surface roles that match your flow.',
    'subscription.title': 'Create Subscription Alert',
    'subscription.description': 'Create a subscription to receive the latest openings right away. You can manage subscriptions in your profile.',
    'subscription.conditionsLabel': 'Search criteria:',
    'subscription.keyword': 'Keyword: {value}',
    'subscription.company': 'Company: {value}',
    'subscription.location': 'Location: {value}',
    'subscription.level': 'Level: {value}',
    'actions.cancel': 'Cancel',
    'actions.confirmSubscription': 'Confirm Subscription',
    'actions.reset': 'Reset',
    'actions.search': 'Search',
    'actions.retry': 'Retry',
    'actions.createSubscription': 'Create Alert',
    'actions.previous': 'Previous',
    'actions.next': 'Next',
    'filters.title': 'Filter Jobs',
    'filters.company': 'Company',
    'filters.level': 'Level',
    'filters.remote': 'Remote',
    'filters.remote.true': 'Remote',
    'filters.remote.false': 'Onsite',
    'filters.salaryMin': 'Minimum Salary',
    'filters.salaryPlaceholder': 'Min salary',
    'filters.datePosted': 'Posted Within',
    'filters.dateOptions.1': 'Past 1 day',
    'filters.dateOptions.3': 'Past 3 days',
    'filters.dateOptions.7': 'Past 7 days',
    'filters.dateOptions.30': 'Past 30 days',
    'filters.open': 'Filter',
    'filters.apply': 'Apply',
    'filters.cancel': 'Cancel',
    'forms.any': 'Any',
    'jobLevels.junior': 'Junior',
    'jobLevels.mid': 'Mid',
    'jobLevels.senior': 'Senior',
    'jobLevels.staff': 'Staff',
    'jobLevels.principal': 'Principal',
    'search.keywordPlaceholder': 'Keywords (e.g.: backend, Java)',
    'search.locationPlaceholder': 'Location',
    'search.loading': 'Loading...',
    'search.results': '{count} results',
    'search.refreshing': 'refreshing',
    'search.page': 'Page {page}',
    'errors.generic': 'Error: {message}',
    'errors.fetchJobs': 'Unable to fetch jobs, please try again later.',
    'jobDetail.empty': 'Select a job on the left to view details',
    'jobDetail.summary': 'Summary:',
    'jobDetail.summaryPlaceholder': 'Summary is being generated…',
    'jobDetail.skills': 'Key skills:',
    'jobDetail.skillsPlaceholder': 'Skills are being collected…',
    'jobDetail.highlights': 'Highlights:',
    'jobDetail.highlightsPlaceholder': 'Highlights are on the way…',
    'jobDetail.tags': 'Tags:',
    'jobDetail.description': 'Description:',
    'jobDetail.noDescription': 'No description available',
    'jobDetail.error': 'Unable to fetch job detail, please try again later.',
    'jobDetail.refreshing': 'Refreshing job detail…',
    'jobDetail.viewOriginal': 'View job posting',
    'jobCard.summaryPlaceholder': 'Summary coming soon…',
    'jobCard.skillsPlaceholder': 'Skills will appear here soon',
    'jobCard.highlightsPlaceholder': 'Highlights coming soon…',
  },
};

type TranslationKey = keyof typeof translations.en;

type I18nContextValue = {
  language: Language;
  setLanguage: (language: Language) => void;
  t: (key: TranslationKey, vars?: Record<string, string | number>) => string;
};

const I18nContext = createContext<I18nContextValue | undefined>(undefined);

function formatMessage(language: Language, key: TranslationKey, vars?: Record<string, string | number>) {
  const fallback = translations.en[key];
  const template = translations[language][key] ?? fallback ?? key;
  if (!vars) return template;
  return template.replace(/\{(\w+)\}/g, (_, token: string) => {
    const value = vars[token];
    return value === undefined || value === null ? '' : String(value);
  });
}

export function LanguageProvider({ children }: { children: React.ReactNode }) {
  const [language, setLanguageState] = useState<Language>('zh');

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const stored = window.localStorage.getItem('app-language');
    if (stored === 'zh' || stored === 'en') {
      setLanguageState(stored);
      return;
    }
    const navigatorLang = window.navigator.language.toLowerCase();
    setLanguageState(navigatorLang.startsWith('zh') ? 'zh' : 'en');
  }, []);

  useEffect(() => {
    if (typeof document === 'undefined') return;
    document.documentElement.lang = language === 'zh' ? 'zh-Hans' : 'en';
    if (typeof window !== 'undefined') {
      window.localStorage.setItem('app-language', language);
    }
  }, [language]);

  const setLanguage = useCallback((next: Language) => {
    setLanguageState(next);
  }, []);

  const value = useMemo<I18nContextValue>(() => ({
    language,
    setLanguage,
    t: (key, vars) => formatMessage(language, key, vars),
  }), [language, setLanguage]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error('useI18n must be used within a LanguageProvider');
  }
  return context;
}

export type { Language, TranslationKey };
