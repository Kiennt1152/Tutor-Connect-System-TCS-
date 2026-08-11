import { useState, type FormEvent } from 'react';
import { useCreateTicket } from '../hooks/useMessaging';
import type { SupportTicketCategory, SupportTicketPriority } from '../types/messagingTypes';

type CreateTicketFormProps = {
  onSuccess: () => void;
  onCancel: () => void;
};

export function CreateTicketForm({ onSuccess, onCancel }: CreateTicketFormProps) {
  const [category, setCategory] = useState<SupportTicketCategory>('INQUIRY');
  const [priority, setPriority] = useState<SupportTicketPriority>('MEDIUM');
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');

  const { status, errorMessage, submit } = useCreateTicket(onSuccess);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    submit({ category, priority, subject, description });
  };

  return (
    <div className="ticket-form-card">
      <h3>Tạo yêu cầu hỗ trợ mới</h3>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Loại yêu cầu *</label>
          <select value={category} onChange={(e) => setCategory(e.target.value as SupportTicketCategory)}>
            <option value="INQUIRY">Câu hỏi chung</option>
            <option value="SYSTEM_ERROR">Lỗi hệ thống</option>
            <option value="DISPUTE">Khiếu nại / Tranh chấp</option>
            <option value="REPORT_USER">Báo cáo người dùng</option>
            <option value="BUG_REPORT">Báo lỗi</option>
          </select>
        </div>

        <div className="form-group">
          <label>Độ ưu tiên</label>
          <select value={priority} onChange={(e) => setPriority(e.target.value as SupportTicketPriority)}>
            <option value="LOW">Thấp</option>
            <option value="MEDIUM">Trung bình</option>
            <option value="HIGH">Cao</option>
            <option value="URGENT">Khẩn cấp</option>
          </select>
        </div>

        <div className="form-group">
          <label>Tiêu đề *</label>
          <input
            type="text"
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            placeholder="Mô tả ngắn gọn vấn đề của bạn"
            required
          />
        </div>

        <div className="form-group">
          <label>Mô tả chi tiết *</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={6}
            placeholder="Mô tả chi tiết vấn đề, thời điểm xảy ra, và các thông tin liên quan..."
            required
          />
        </div>

        {errorMessage && <div className="form-error">{errorMessage}</div>}

        <div className="form-actions">
          <button
            type="button"
            className="tcs-btn tcs-btn--secondary"
            onClick={onCancel}
            disabled={status === 'loading'}
          >
            Hủy
          </button>
          <button
            type="submit"
            className="tcs-btn tcs-btn--primary"
            disabled={status === 'loading' || !subject.trim() || !description.trim()}
          >
            {status === 'loading' ? 'Đang gửi...' : 'Gửi yêu cầu'}
          </button>
        </div>
      </form>
    </div>
  );
}
