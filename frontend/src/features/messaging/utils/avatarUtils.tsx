export function getInitials(name: string | null | undefined): string {
  if (!name || !name.trim()) return '?';
  return name
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
}

const AVATAR_PALETTE = [
  '#ea580c',
  '#0ea5e9',
  '#16a34a',
  '#9333ea',
  '#dc2626',
  '#0891b2',
  '#ca8a04',
  '#4f46e5',
];

/** Sinh mau nen on dinh cho avatar dua tren userId, dung khi khong co avatarUrl. */
export function getAvatarColor(userId: number): string {
  return AVATAR_PALETTE[userId % AVATAR_PALETTE.length];
}
