import { APP_ROUTES } from '../../shared/constants/routes';
import type { NotificationItem } from './api/notificationsApi';

export function notificationLink(
  n: NotificationItem,
  role: string | undefined | null,
): string | null {
  const isAdmin = role === 'PLATFORM_ADMIN';
  const isClientSide = role === 'CLIENT' || role === 'TUTOR_CENTER';

  if (n.referenceType === 'SUPPORT_TICKET') {
    if (!n.referenceId) return isAdmin ? APP_ROUTES.platformTickets : APP_ROUTES.messagingTickets;
    return isAdmin ? `${APP_ROUTES.platformTickets}?ticket=${n.referenceId}` : APP_ROUTES.messagingTickets;
  }

  switch (n.type) {
    case 'REPORT':
      return APP_ROUTES.platformReports;

    case 'VERIFICATION':
      return isAdmin ? APP_ROUTES.platformVerifications : APP_ROUTES.verification;

    case 'REVIEW':
      return isClientSide ? APP_ROUTES.feedback : APP_ROUTES.myReputation;

    case 'APPLICATION':
      return isClientSide ? APP_ROUTES.marketplace : APP_ROUTES.teaching;

    case 'CLASS':
      return APP_ROUTES.teaching;

    case 'CHAT':
      return APP_ROUTES.messaging;

    case 'PAYMENT':
      return APP_ROUTES.finance;

    default:
      return null;
  }
}
