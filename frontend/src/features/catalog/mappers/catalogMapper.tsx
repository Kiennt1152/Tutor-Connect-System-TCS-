import type { CategoryItem } from '../types/catalogTypes';

export function mapCategoryTree(response: CategoryItem[]): CategoryItem[] {
  return response.map(mapCategoryItem);
}

function mapCategoryItem(response: CategoryItem): CategoryItem {
  return {
    ...response,
    description: response.description ?? null,
    children: response.children.map(mapCategoryItem),
  };
}
