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

  // Đổi lịch buổi học: trung tâm về trang duyệt, gia sư về lịch lớp trung tâm của mình.
  if (n.referenceType === 'RESCHEDULE') {
    if (role === 'TUTOR_CENTER') return APP_ROUTES.centerReschedules;
    if (role === 'TUTOR') return APP_ROUTES.tutorSchedule;
    if (role === 'CLIENT') return APP_ROUTES.clientSchedule;
    return null;
  }

  // Lớp của TRUNG TÂM có lịch riêng theo vai trò, không dùng chung màn Lịch dạy
  // của lớp cá nhân (route đó chỉ cho TUTOR/CLIENT — trung tâm vào sẽ bị chặn 403).
  if (n.referenceType === 'CENTER_CLASS') {
    if (role === 'TUTOR') return APP_ROUTES.tutorSchedule;
    if (role === 'CLIENT') return APP_ROUTES.clientSchedule;
    if (role === 'TUTOR_CENTER') return APP_ROUTES.center;
    return null;
  }

  // Hợp đồng: việc cần làm là ký hoặc thanh toán ký quỹ, phải về trang Hợp đồng.
  // referenceId ở đây là classId (lúc gửi thông báo chưa chắc đã có hợp đồng),
  // nên chỉ mở danh sách chứ không ghép thành /contract/{id}.
  if (n.referenceType === 'CONTRACT') {
    return APP_ROUTES.contract;
  }

  // Hồ sơ xác minh: admin mở thẳng hồ sơ cần duyệt (trang tự bung chi tiết theo ?id=),
  // người nộp thì về trang xác minh của chính mình.
  if (n.referenceType === 'VERIFICATION_REQUEST') {
    if (!isAdmin) return APP_ROUTES.verification;
    return n.referenceId
      ? `${APP_ROUTES.platformVerifications}?id=${n.referenceId}`
      : APP_ROUTES.platformVerifications;
  }

  if (n.referenceType === 'REPORT') {
    return isAdmin ? APP_ROUTES.platformReports : APP_ROUTES.help;
  }

  if (n.referenceType === 'DISPUTE') {
    return isAdmin ? APP_ROUTES.platformReports : APP_ROUTES.contract;
  }

  if (n.referenceType === 'VERIFICATION_REQUEST') {
    return isAdmin ? APP_ROUTES.platformVerifications : APP_ROUTES.verification;
  }

  if (n.referenceType === 'REFUND_REQUEST') {
    if (isAdmin) return APP_ROUTES.platformWithdrawals;
    if (role === 'CLIENT') return APP_ROUTES.contract;
    return APP_ROUTES.finance;
  }

  if (n.referenceType === 'WITHDRAWAL') {
    return isAdmin ? APP_ROUTES.platformWithdrawals : APP_ROUTES.finance;
  }

  if (n.referenceType === 'WITHDRAWAL_REQUEST') {
    return isAdmin ? APP_ROUTES.platformWithdrawals : APP_ROUTES.finance;
  }

  if (n.referenceType === 'ESCROW') {
    if (isAdmin) return APP_ROUTES.platformEscrows;
    if (role === 'CLIENT') return APP_ROUTES.contract;
    if (role === 'TUTOR' || role === 'TUTOR_CENTER') return APP_ROUTES.finance;
    return APP_ROUTES.finance;
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
