export interface TutorReference {
  tutorId: number;
  fullName: string;
  avatarUrl?: string;
  title?: string;
  hourlyRate?: number;
  averageRating?: number;
  totalReviews?: number;
  teachingAreas?: string;
}

export interface ClassReference {
  classId: number;
  title: string;
  subjectName?: string;
  gradeLevelName?: string;
  tuitionFee?: number;
  location?: string;
  status?: string;
}

export interface FaqReference {
  faqId: number;
  question: string;
  answer: string;
  category?: string;
}

export interface AiSourceResponse {
  sourceId: string;
  sourceType: string;
  title: string;
  snippet: string;
  similarity: number;
  finalScore: number;
  visibility: string;
}

export interface AiMessage {
  messageId: number;
  sessionId: number;
  role: 'user' | 'assistant';
  content: string;
  createdAt: string;
  referencedTutors?: TutorReference[];
  referencedClasses?: ClassReference[];
  referencedFaqs?: FaqReference[];
  sources?: AiSourceResponse[];
  intent?: string;
  answerMode?: 'RAG' | 'LLM' | 'FALLBACK';
  confidenceScore?: number;
  confidenceLevel?: 'HIGH' | 'MEDIUM' | 'LOW';
  sourceCount?: number;
  groundingStatus?: string;
  warningCode?: string;
  rewrittenQuery?: string;
  followUp?: boolean;
  evaluationNotes?: string;
}

export interface AiSession {
  sessionId: number;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface ChatRequestPayload {
  message: string;
  sessionId?: number;
  userRole?: string;
}
