import axios from 'axios';

export const OFFLINE_ERROR_MESSAGE =
  'Không kết nối được máy chủ. Hãy chạy BackendApplication (port 8080) rồi thử lại.';

/**
 * Trích xuất thông báo lỗi thân thiện từ một lỗi bất kỳ (thường là lỗi axios).
 * Quy ước chung của backend là trả về body dạng `{ "message": "..." }`.
 *
 * - Không có response (mất mạng / backend chưa chạy) -> {@link OFFLINE_ERROR_MESSAGE}
 * - Có `response.data.message` dạng chuỗi -> dùng thông báo đó
 * - Các trường hợp còn lại -> `fallback`
 */
export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      return OFFLINE_ERROR_MESSAGE;
    }
    const message = error.response.data?.message;
    if (typeof message === 'string' && message.trim()) {
      return message.trim();
    }
  }
  return fallback;
}
