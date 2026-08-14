export type PenaltySourceType = 'REPORT' | 'CIRCUMVENTION' | 'DISPUTE' | 'TICKET' | 'DIRECT';

export interface PenaltySourceData {
  sourceType: PenaltySourceType;
  sourceId: number | string;
  sourceTaskId: string;
}

export function buildPenaltySource(
  sourceType: PenaltySourceType,
  sourceId: string | number,
  customTaskId?: string
): PenaltySourceData {
  const idStr = String(sourceId).trim();
  const idNum = Number(sourceId);
  return {
    sourceType,
    sourceId: !isNaN(idNum) ? idNum : idStr,
    sourceTaskId: customTaskId || `${sourceType}-${idStr}`,
  };
}

export function resolvePenaltySourceRoute(
  sourceType?: string | null,
  sourceId?: string | number | null
): string | null {
  if (!sourceType || !sourceId) return null;
  const cleanType = sourceType.trim().toUpperCase();
  const cleanId = String(sourceId).trim();

  switch (cleanType) {
    case 'REPORT':
      return `/platform/reports?tab=reports&id=${cleanId}`;
    case 'CIRCUMVENTION':
      return `/platform/reports?tab=circumvention&id=${cleanId}`;
    case 'DISPUTE':
      return `/platform/reports?tab=disputes&id=${cleanId}`;
    case 'TICKET':
      return `/platform/tickets?id=${cleanId}`;
    default:
      return null;
  }
}
