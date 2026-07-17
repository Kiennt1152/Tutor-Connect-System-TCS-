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
