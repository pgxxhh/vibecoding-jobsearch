'use client';

import { useMemo } from 'react';

import type { Job } from '@/lib/types';

function decodeHtmlEntities(value: string): string {
  const textarea = document.createElement('textarea');
  textarea.innerHTML = value;
  return textarea.value;
}

function sanitizeJobContent(content: string): string {
  const container = document.createElement('div');
  container.innerHTML = decodeHtmlEntities(content);

  container.querySelectorAll('script, style, iframe, object, embed, link, meta').forEach((node) => node.remove());

  container.querySelectorAll('*').forEach((node) => {
    Array.from(node.attributes).forEach((attr) => {
      const name = attr.name.toLowerCase();
      if (name.startsWith('on')) {
        node.removeAttribute(attr.name);
      }
      if (name === 'href' && attr.value.trim().toLowerCase().startsWith('javascript:')) {
        node.setAttribute('href', '#');
      }
      if (name === 'target' && attr.value === '_blank') {
        const rel = node.getAttribute('rel') ?? '';
        if (!/noopener|noreferrer/.test(rel)) {
          node.setAttribute('rel', `${rel} noopener noreferrer`.trim());
        }
      }
    });
  });

  return container.innerHTML;
}

export default function JobDetailClient({ job, fallback }: { job: Job | null; fallback?: string }) {
  const sanitized = useMemo(() => {
    if (!job?.content) return '';
    return sanitizeJobContent(job.content);
  }, [job?.content]);

  if (!sanitized.trim()) return fallback ? <>{fallback}</> : null;

  return <div dangerouslySetInnerHTML={{ __html: sanitized }} />;
}
