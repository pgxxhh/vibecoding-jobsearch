
export type Job = {
  id: string;
  title: string;
  company: string;
  location: string;
  level?: string;
  postedAt: string;
  tags?: string[];
  url: string;
  summary?: string;
  skills?: string[];
  highlights?: string[];
  structuredData?: string;
  detailMatch?: boolean;
  content?: string;
};
export type JobDetail = {
  id: string;
  title: string;
  company: string;
  location: string;
  postedAt: string;
  content: string;
  summary?: string | null;
  skills?: string[];
  highlights?: string[];
  structuredData?: string | null;
};
export type JobsQuery = {
  q?: string;
  location?: string;
  company?: string;
  level?: string;
  size?: number;
  cursor?: string | null;
};
export type JobsResponse = {
  items: Job[];
  total: number;
  nextCursor: string | null;
  hasMore: boolean;
  size: number;
};
