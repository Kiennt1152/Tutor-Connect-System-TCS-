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

export interface AiMessage {
  messageId: number;
  sessionId: number;
  role: 'user' | 'assistant';
  content: string;
  createdAt: string;
  referencedTutors?: TutorReference[];
  referencedClasses?: ClassReference[];
  referencedFaqs?: FaqReference[];
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
