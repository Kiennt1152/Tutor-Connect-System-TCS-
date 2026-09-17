/**
 * Lưu một Blob nhận từ API xuống máy người dùng.
 *
 * File tải qua API phải kèm token nên không dùng thẻ <a href> trực tiếp được; cách làm là tạo
 * URL tạm cho blob, bấm hộ một thẻ <a download> rồi thu hồi URL ngay để không rò bộ nhớ.
 */
export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
