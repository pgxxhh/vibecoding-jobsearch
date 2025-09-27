
export type Job = {
  id: string;
  title: string;
  company: string;
  location: string;
  level?: string;
  postedAt: string;
  tags?: string[];
  url: string;
  description?: string;
};
export type JobDetail = {
  id: string;
  title: string;
  company: string;
  location: string;
  postedAt: string;
  description: string;
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
