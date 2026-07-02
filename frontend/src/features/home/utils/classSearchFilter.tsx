import type { OpenClassItem } from '../types/openClassTypes';

export function filterOpenClasses(
  classes: OpenClassItem[],
  subject: string,
  area: string,
): OpenClassItem[] {
  const subjectNorm = subject.trim().toLowerCase();
  const areaNorm = area.trim().toLowerCase();

  if (!subjectNorm && !areaNorm) {
    return [];
  }

  return classes.filter((classItem) => {
    const searchable = [
      classItem.title,
      classItem.description,
      classItem.subjectName,
      classItem.gradeName,
      classItem.creatorName,
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();

    const subjectMatch = !subjectNorm || searchable.includes(subjectNorm);
    const areaMatch = !areaNorm || searchable.includes(areaNorm);
    return subjectMatch && areaMatch;
  });
}

export function buildClassSearchLabel(subject: string, area: string) {
  return [subject.trim(), area.trim()].filter(Boolean).join(' · ');
}
