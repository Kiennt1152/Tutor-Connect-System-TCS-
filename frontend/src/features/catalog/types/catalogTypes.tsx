export interface FaqEntryApiResponse {
  faqId: number;
  question: string;
  answer: string;
  category: string;
  sortOrder: number;
}

export interface ChatbotAskRequest {
  question: string;
}

export interface ChatbotAskResponse {
  matched: boolean;
  faqId: number | null;
  question: string | null;
  answer: string | null;
  suggestion: string | null;
}

export interface UpsertCategoryRequest {
  name: string;
  description: string;
  rootName: 'SUBJECT' | 'EDUCATION_LEVEL' | 'LOCATION' | 'SYSTEM_CONFIG';
  parentId: number | null;
  status: 'ACTIVE' | 'INACTIVE';
}

export interface CategoryParent {
  categoryId: number;
  name: string;
}

export interface CategoryItem {
  categoryId: number;
  name: string;
  description: string | null;
  status: 'ACTIVE' | 'INACTIVE';
  parent: CategoryParent | null;
  usedByTutorSubjects: boolean;
  usedByTutoringClasses: boolean;
  deletable: boolean;
  children: CategoryItem[];
}
