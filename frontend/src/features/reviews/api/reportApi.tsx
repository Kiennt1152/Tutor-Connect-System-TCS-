import axiosClient from '../../../shared/api/axiosClient';

export type ReportCategory = 'FRAUD' | 'ABUSE' | 'INAPPROPRIATE' | 'OTHER';

export const reportApi = {
  reportUser(userId: number, category: ReportCategory, description: string) {
    return axiosClient.post('/messaging/reports', {
      targetType: 'USER', targetId: userId, category, description: description.trim(),
    });
  },
  reportReview(reviewId: number, category: ReportCategory, description?: string) {
    return axiosClient.post('/messaging/reports', {
      targetType: 'REVIEW',
      targetId: reviewId,
      category,
      description: description?.trim() || undefined,
    });
  },
};

export type ReportCategoryOption = { value: ReportCategory; label: string; hint: string };

export const REPORT_CATEGORY_OPTIONS: ReportCategoryOption[] = [
  { value: 'FRAUD', label: 'Sai sự thật / gian lận', hint: 'Nội dung bịa đặt, không đúng buổi học thực tế' },
  { value: 'ABUSE', label: 'Lăng mạ / xúc phạm', hint: 'Ngôn từ công kích, thù ghét, xúc phạm cá nhân' },
  { value: 'INAPPROPRIATE', label: 'Nội dung không phù hợp', hint: 'Ngôn từ tục tĩu, phản cảm, nhạy cảm' },
  { value: 'OTHER', label: 'Lý do khác', hint: 'Không thuộc các mục trên, mô tả chi tiết bên dưới' },
];

export const REPLY_REPORT_CATEGORY_OPTIONS: ReportCategoryOption[] = [
  { value: 'ABUSE', label: 'Xúc phạm / công kích người học', hint: 'Phản hồi dùng ngôn từ thù ghét, hạ nhục, công kích cá nhân bạn' },
  { value: 'INAPPROPRIATE', label: 'Đe dọa / quấy rối', hint: 'Phản hồi mang tính đe dọa, quấy rối hoặc chứa nội dung phản cảm' },
  { value: 'FRAUD', label: 'Bóp méo / vu khống', hint: 'Phản hồi xuyên tạc sự thật, bịa đặt nhằm hạ uy tín của bạn' },
  { value: 'OTHER', label: 'Lý do khác', hint: 'Không thuộc các mục trên, mô tả chi tiết bên dưới' },
];
