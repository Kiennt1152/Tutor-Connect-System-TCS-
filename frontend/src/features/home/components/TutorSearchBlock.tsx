import { type FormEvent, useState } from 'react';
import { useTutorSearch } from '../hooks/useTutorSearch';
import type { SubjectItem } from '../types/homeTypes';
import { TutorSearchResults } from './TutorSearchResults';

type TutorSearchBlockProps = {
  subjects: SubjectItem[];
  isAuthenticated: boolean;
};

function resolveSubjectId(subjectInput: string, subjects: SubjectItem[]) {
  const normalized = subjectInput.trim().toLowerCase();
  if (!normalized) return undefined;
  const exact = subjects.find((s) => s.name.toLowerCase() === normalized);
  if (exact) return Number(exact.id);
  const partial = subjects.find((s) => s.name.toLowerCase().includes(normalized));
  return partial ? Number(partial.id) : undefined;
}

function buildKeyword(subject: string, area: string) {
  return [subject.trim(), area.trim()].filter(Boolean).join(' ').trim();
}

export function TutorSearchBlock({ subjects, isAuthenticated }: TutorSearchBlockProps) {
  const { status, results, lastQuery, search } = useTutorSearch();
  const [subject, setSubject] = useState('');
  const [area, setArea] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const keyword = buildKeyword(subject, area);
    if (!keyword) {
      return;
    }
    setSubmitted(true);
    const label = [subject.trim(), area.trim()].filter(Boolean).join(' · ');
    search({
      keyword,
      subjectId: resolveSubjectId(subject, subjects),
      label,
    });
  };

  return (
    <div className="tcs-search-block">
      <form className="tcs-search" onSubmit={handleSubmit}>
        <div className="tcs-search__fields">
          <label className="tcs-search__label">
            Môn học
            <input
              className="tcs-search__field"
              placeholder="Bạn muốn học môn gì?"
              aria-label="Môn học"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              list="tcs-subject-suggestions"
            />
          </label>
          <label className="tcs-search__label">
            Khu vực
            <input
              className="tcs-search__field"
              placeholder="Quận, thành phố..."
              aria-label="Khu vực"
              value={area}
              onChange={(e) => setArea(e.target.value)}
            />
          </label>
        </div>
        <datalist id="tcs-subject-suggestions">
          {subjects.map((item) => (
            <option key={item.id} value={item.name} />
          ))}
        </datalist>
        <button className="tcs-btn tcs-btn--market tcs-search__btn" type="submit">
          Tìm kiếm
        </button>
      </form>

      {submitted ? (
        <TutorSearchResults
          status={status}
          results={results}
          query={lastQuery}
          isAuthenticated={isAuthenticated}
        />
      ) : null}
    </div>
  );
}
