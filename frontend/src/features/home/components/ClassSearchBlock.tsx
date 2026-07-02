import { type FormEvent, useState } from 'react';
import type { SubjectItem } from '../types/homeTypes';
import type { OpenClassItem } from '../types/openClassTypes';
import type { OpenClassesStatus } from '../hooks/useOpenClasses';
import { buildClassSearchLabel, filterOpenClasses } from '../utils/classSearchFilter';
import { ClassSearchResults } from './ClassSearchResults';

type ClassSearchBlockProps = {
  subjects: SubjectItem[];
  classes: OpenClassItem[];
  classesStatus: OpenClassesStatus;
  isAuthenticated: boolean;
};

export function ClassSearchBlock({
  subjects,
  classes,
  classesStatus,
  isAuthenticated,
}: ClassSearchBlockProps) {
  const [subject, setSubject] = useState('');
  const [area, setArea] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [results, setResults] = useState<OpenClassItem[]>([]);
  const [lastQuery, setLastQuery] = useState('');

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const label = buildClassSearchLabel(subject, area);
    if (!label) return;

    setSubmitted(true);
    setLastQuery(label);
    setResults(filterOpenClasses(classes, subject, area));
  };

  const resultStatus =
    classesStatus === 'loading' ? 'loading' : submitted ? 'success' : 'idle';

  return (
    <div className="tcs-search-block">
      <form className="tcs-search" onSubmit={handleSubmit}>
        <div className="tcs-search__fields">
          <label className="tcs-search__label">
            Môn học
            <input
              className="tcs-search__field"
              placeholder="Môn bạn muốn dạy?"
              aria-label="Môn học"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              list="tcs-class-subject-suggestions"
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
        <datalist id="tcs-class-subject-suggestions">
          {subjects.map((item) => (
            <option key={item.id} value={item.name} />
          ))}
        </datalist>
        <button className="tcs-btn tcs-btn--market tcs-search__btn" type="submit">
          Tìm lớp
        </button>
      </form>

      {submitted ? (
        <ClassSearchResults
          status={resultStatus}
          results={results}
          query={lastQuery}
          isAuthenticated={isAuthenticated}
        />
      ) : null}
    </div>
  );
}
