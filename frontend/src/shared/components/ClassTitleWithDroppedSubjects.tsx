import './ClassTitleWithDroppedSubjects.css';

/** Phần tên môn trong tiêu đề tự sinh: "Cần tìm gia sư môn Toán, Vật lý". */
const TITLE_SUBJECT_MARKER = ' môn ';

/**
 * Tên lớp, gạch ngang những môn gia sư KHÔNG nhận dạy.
 *
 * Lớp đăng "Toán, Vật lý" mà gia sư chỉ nhận Toán: để nguyên tiêu đề thì người đọc tưởng vẫn còn
 * học Vật lý, mà bỏ hẳn tên môn đó đi thì mất dấu vết môn đã rớt. Gạch ngang giữ được cả hai —
 * thấy lớp đã đăng gì, và thấy môn nào không còn.
 *
 * Căn cứ là danh sách môn THỰC DẠY của lớp, không phải môn chính: một lớp hai môn thì môn chính
 * chỉ là một trong hai, lấy nó làm chuẩn sẽ gạch nhầm môn đang học thật.
 */
export function ClassTitleWithDroppedSubjects({
  title,
  taughtSubjects,
}: {
  title: string;
  taughtSubjects: string[];
}) {
  const at = title.indexOf(TITLE_SUBJECT_MARKER);
  if (at < 0 || taughtSubjects.length === 0) return <>{title}</>;

  const head = title.slice(0, at + TITLE_SUBJECT_MARKER.length);
  const listed = title
    .slice(at + TITLE_SUBJECT_MARKER.length)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
  const taught = new Set(taughtSubjects.map((s) => s.trim()));

  // Tiêu đề do người dùng tự đặt (hoặc bị cắt ngắn) thì không khớp được môn nào — giữ nguyên,
  // thà không gạch còn hơn gạch nhầm một môn đang học.
  if (!listed.some((s) => taught.has(s))) return <>{title}</>;

  return (
    <>
      {head}
      {listed.map((subject, i) => (
        <span key={subject}>
          {i > 0 ? ', ' : ''}
          {taught.has(subject) ? (
            subject
          ) : (
            <s className="tcs-subject--dropped" title="Gia sư không nhận dạy môn này">
              {subject}
            </s>
          )}
        </span>
      ))}
    </>
  );
}
