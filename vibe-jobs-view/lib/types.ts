
export type Job = {
  id: string;
  title: string;
  company: string;
  location: string;
  level?: string;
  postedAt: string;
  tags?: string[];
  url: string;
};
export type JobsQuery = {
  q?: string;
  location?: string;
  company?: string;
  level?: string;
  page?: number;
  size?: number;
};
export type JobsResponse = {
  items: Job[];
  total: number;
  page: number;
  size: number;
};
