import type { TutorApplicationStatus } from '../types/marketplaceTypes';
import { APPLICATION_STATUS_LABELS, APPLICATION_STATUS_TONES } from '../types/marketplaceTypes';

type Props = {
  status: TutorApplicationStatus;
};

export function StatusBadge({ status }: Props) {
  const tone = APPLICATION_STATUS_TONES[status] ?? 'muted';
  const label = APPLICATION_STATUS_LABELS[status] ?? status;
  return <span className={`mp-badge mp-badge--${tone}`}>{label}</span>;
}