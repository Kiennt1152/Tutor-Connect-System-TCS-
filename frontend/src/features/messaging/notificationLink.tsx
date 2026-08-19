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

  if (n.referenceType === 'PENALTY') {
    return isAdmin ? APP_ROUTES.platformPenalties : APP_ROUTES.profile;
  }

  if (n.referenceType === 'CLASS_REQUEST') {
    return role === 'TUTOR_CENTER' ? APP_ROUTES.center : APP_ROUTES.marketplace;
  }

  if (n.referenceType === 'REPORT') {
    return isAdmin ? APP_ROUTES.platformReports : APP_ROUTES.help;
  }

  if (n.referenceType === 'VERIFICATION_REQUEST') {
    return isAdmin ? APP_ROUTES.platformVerifications : APP_ROUTES.verification;
  }

  if (n.referenceType === 'REFUND_REQUEST') {
    if (isAdmin) return APP_ROUTES.platformWithdrawals;
    if (role === 'CLIENT') return APP_ROUTES.contract;
    return APP_ROUTES.finance;
  }

  if (n.referenceType === 'WITHDRAWAL_REQUEST') {
    return isAdmin ? APP_ROUTES.platformWithdrawals : APP_ROUTES.finance;
  }

  if (n.referenceType === 'ESCROW') {
    if (isAdmin) return APP_ROUTES.platformEscrows;
    return APP_ROUTES.contract;
  }

  if (n.referenceType === 'PAYMENT_TRANSACTION') {
    return role === 'CLIENT' ? APP_ROUTES.contract : APP_ROUTES.finance;
  }

  switch (n.type) {
    case 'REPORT':
      return isAdmin ? APP_ROUTES.platformReports : APP_ROUTES.help;

    case 'VERIFICATION':
      return isAdmin ? APP_ROUTES.platformVerifications : APP_ROUTES.verification;

    case 'REVIEW':
      return isClientSide ? APP_ROUTES.feedback : APP_ROUTES.myReputation;

    case 'APPLICATION':
      return isClientSide ? APP_ROUTES.marketplace : APP_ROUTES.teaching;

    case 'CLASS':
      return APP_ROUTES.teaching;

    case 'CHAT':
      return n.referenceType === 'CONVERSATION' && n.referenceId
        ? `${APP_ROUTES.messaging}?conv=${n.referenceId}`
        : APP_ROUTES.messaging;

    case 'PAYMENT':
      return role === 'CLIENT' ? APP_ROUTES.contract : APP_ROUTES.finance;

    default:
      return null;
  }
}
