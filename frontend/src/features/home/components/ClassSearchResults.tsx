import { ClassListingCard } from './ClassListingCard';
import type { OpenClassItem } from '../types/openClassTypes';

type ClassSearchResultsProps = {
  status: 'idle' | 'loading' | 'success';
  results: OpenClassItem[];
  query: string;
  isAuthenticated: boolean;
};

export function ClassSearchResults({
  status,
  results,
  query,
  isAuthenticated,
}: ClassSearchResultsProps) {
  return (
    <div id="search-results" className="tcs-search-results">
      <div className="tcs-section-bar">
        <div>
          <h2 className="tcs-section-bar__title">Kết quả tìm lớp</h2>
          {query ? <p className="tcs-section-bar__subtitle">Từ khóa: {query}</p> : null}
        </div>
        {status === 'success' && results.length > 0 ? (
          <span className="tcs-section-bar__count">{results.length} lớp</span>
        ) : null}
      </div>

      {status === 'loading' && (
        <div className="tcs-search-results__state">
          <span className="tcs-spinner" aria-hidden="true" />
          Đang tải danh sách lớp...
        </div>
      )}

      {status === 'success' && results.length === 0 && (
        <div className="tcs-search-results__state">
          Không tìm thấy lớp phù hợp. Thử môn học hoặc khu vực khác.
        </div>
      )}

      {status === 'success' && results.length > 0 && (
        <div className="tcs-class-list">
          {results.map((classItem) => (
            <ClassListingCard
              key={classItem.id}
              classItem={classItem}
              isAuthenticated={isAuthenticated}
            />
          ))}
        </div>
      )}
    </div>
  );
}
