import { Fragment } from 'react';
import type { CenterContractInfo } from '../types/centerTypes';
import '../../teaching/pages/ContractSigningPage.css';

interface Props {
  name?: string;
  contractType: 'CLASS' | 'RECRUITMENT';
  content: string;
  info: CenterContractInfo | null;
}

const DOC_TITLE: Record<'CLASS' | 'RECRUITMENT', string> = {
  CLASS: 'HỢP ĐỒNG DỊCH VỤ DẠY HỌC',
  RECRUITMENT: 'HỢP ĐỒNG CỘNG TÁC GIA SƯ',
};

const PARTY_B_LABEL: Record<'CLASS' | 'RECRUITMENT', string> = {
  CLASS: 'Học viên / Phụ huynh',
  RECRUITMENT: 'Gia sư',
};

/** Tô sáng biến tự điền dạng {{tenBien}} trong một dòng văn bản. */
function renderInline(line: string) {
  const parts = line.split(/(\{\{[^}]+\}\})/g);
  return parts.map((part, i) =>
    /^\{\{[^}]+\}\}$/.test(part) ? (
      <span key={i} className="cct-doc__var">
        {part}
      </span>
    ) : (
      <Fragment key={i}>{part}</Fragment>
    ),
  );
}

/** Chuyển nội dung điều khoản (text nhiều dòng) thành các đoạn/điều/gạch đầu dòng. */
function renderTerms(content: string) {
  const lines = content.split('\n');
  return lines.map((raw, i) => {
    const line = raw.trim();
    if (!line) return null;
    if (/^Điều\s*\d+/i.test(line)) {
      return (
        <h4 key={i} className="ksign-art">
          {renderInline(line)}
        </h4>
      );
    }
    if (/^[-•*]\s+/.test(line)) {
      return (
        <p key={i} className="cct-doc__li">
          {renderInline(line.replace(/^[-•*]\s+/, ''))}
        </p>
      );
    }
    return (
      <p key={i} className="ksign-doc__p">
        {renderInline(line)}
      </p>
    );
  });
}

/** Xem trước một mẫu hợp đồng dưới dạng văn bản hợp đồng thật (quốc hiệu, các bên, điều khoản). */
export function ContractDocumentPreview({ name, contractType, content, info }: Props) {
  const today = new Date();
  const dash = '.................';
  return (
    <article className="ksign-doc cct-doc">
      <div className="ksign-doc__center">
        <p className="ksign-doc__title">CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</p>
        <p className="ksign-doc__sub">Độc lập - Tự do - Hạnh phúc</p>
        <p className="ksign-doc__hr">———</p>
        <h2 className="ksign-doc__name">{DOC_TITLE[contractType]}</h2>
        {name && <p className="cct-doc__tplname">Mẫu: {name}</p>}
      </div>

      <p className="ksign-doc__p">
        Hôm nay, ngày {today.getDate()} tháng {today.getMonth() + 1} năm {today.getFullYear()},
        chúng tôi gồm có:
      </p>

      <div className="ksign-party">
        <h3>Bên A: (Trung tâm)</h3>
        <p>
          Tên trung tâm: <b>{info?.companyName || dash}</b>
        </p>
        <p>Địa chỉ: {info?.address || dash}</p>
        <p>Điện thoại: {info?.phone || dash}</p>
        <p>Email: {info?.email || dash}</p>
        {info?.website && <p>Website: {info.website}</p>}
        <p>
          Người đại diện: <b>{info?.representativeName || dash}</b>
          {info?.representativePosition ? ` — Chức vụ: ${info.representativePosition}` : ''}
        </p>
      </div>

      <div className="ksign-party">
        <h3>Bên B: ({PARTY_B_LABEL[contractType]})</h3>
        <p>
          Họ và tên: <b>{dash}</b>
        </p>
        <p>Ngày sinh: {dash}</p>
        <p>Số CCCD: {dash}</p>
        <p>Địa chỉ: {dash}</p>
        <p>Điện thoại: {dash}</p>
      </div>

      <p className="ksign-doc__p">
        Sau khi trao đổi và bàn bạc, hai bên đi đến thống nhất lập hợp đồng với nội dung và điều
        khoản sau:
      </p>

      <div className="cct-doc__terms">
        {content.trim() ? (
          renderTerms(content)
        ) : (
          <p className="cct-doc__placeholder">
            — Nội dung điều khoản của mẫu sẽ hiển thị ở đây —
          </p>
        )}
      </div>

      <div className="ksign-sign-row">
        <div>
          <b>ĐẠI DIỆN BÊN A</b>
          <p className="ksign-sign-status">(Chưa ký)</p>
        </div>
        <div>
          <b>ĐẠI DIỆN BÊN B</b>
          <p className="ksign-sign-status">(Chưa ký)</p>
        </div>
      </div>
    </article>
  );
}
