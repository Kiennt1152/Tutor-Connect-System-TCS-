import type { TutorSearchStatus } from '../hooks/useTutorSearch';
import type { TutorSearchItem } from '../types/tutorSearchTypes';
import { tutorSearchToFeatured } from '../mappers/tutorSearchMapper';
import { TutorListingCard } from './TutorListingCard';

type TutorSearchResultsProps = {
  status: TutorSearchStatus;
  results: TutorSearchItem[];
  query: string;
  isAuthenticated: boolean;
};

export function TutorSearchResults({
  status,
  results,
  query,
  isAuthenticated,
}: TutorSearchResultsProps) {
  return (
    <div id="search-results" className="tcs-search-results">
      <div className="tcs-section-bar">
        <div>
          <h2 className="tcs-section-bar__title">Kết quả tìm kiếm</h2>
          {query ? <p className="tcs-section-bar__subtitle">Từ khóa: {query}</p> : null}
        </div>
        {status === 'success' && results.length > 0 ? (
          <span className="tcs-section-bar__count">{results.length} gia sư</span>
        ) : null}
      </div>

      {status === 'loading' && (
        <div className="tcs-search-results__state">
          <span className="tcs-spinner" aria-hidden="true" />
          Đang tìm gia sư...
        </div>
      )}

      {status === 'error' && (
        <div className="tcs-search-results__state tcs-search-results__state--error">
          Không thể tải kết quả. Vui lòng thử lại.
        </div>
      )}

      {status === 'success' && results.length === 0 && (
        <div className="tcs-search-results__state">
          Không tìm thấy gia sư phù hợp. Thử từ khóa hoặc môn học khác.
        </div>
      )}

      {status === 'success' && results.length > 0 && (
        <div className="tcs-listing-grid">
          {results.map((tutor) => (
            <TutorListingCard
              key={tutor.id}
              tutor={tutorSearchToFeatured(tutor)}
              isAuthenticated={isAuthenticated}
              variant="search"
            />
          ))}
        </div>
      )}
    </div>
  );
}
