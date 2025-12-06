export type ResumeParseStatus = 'PENDING' | 'READY' | 'FAILED';

export interface ResumeUploadResult {
  resumeId: number;
  parseStatus: ResumeParseStatus;
}

export interface RecommendationItem {
  jobId: number;
  title: string;
  company: string;
  location: string;
  score: number;
  skillHits: string[];
  explanation: string;
}

export interface RecommendationResponse {
  items: RecommendationItem[];
}

export interface RecommendationFilters {
  location?: string;
  limit?: number;
  experienceYears?: number;
  minSalary?: number;
  remote?: boolean;
}

export type FeedbackValue = 'LIKE' | 'DISLIKE';

export interface FeedbackItem {
  jobId: number;
  feedback: FeedbackValue;
  comment?: string;
}
