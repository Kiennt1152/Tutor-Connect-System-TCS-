/**
 * Khung xương chờ dữ liệu. Giữ đúng bố cục của danh sách thật nên khi dữ liệu về
 * trang không bị nhảy — cảm giác tải nhanh hơn hẳn so với spinner giữa màn hình.
 */
export function TutorListSkeleton({ count = 6 }: { count?: number }) {
  return (
    <section className="tcs-section tcs-section--tutors" aria-busy="true" aria-live="polite">
      <div className="tcs-container">
        <div className="tcs-section-bar">
          <div style={{ flex: 1 }}>
            <span className="tcs-skel tcs-skel--title" />
            <span className="tcs-skel tcs-skel--line" style={{ width: '52%' }} />
          </div>
        </div>
        <div className="tcs-listing-grid">
          {Array.from({ length: count }).map((_, i) => (
            <article key={i} className="tcs-skel-card">
              <div className="tcs-skel-card__top">
                <span className="tcs-skel tcs-skel--avatar" />
                <div className="tcs-skel-card__ident">
                  <span className="tcs-skel tcs-skel--line" style={{ width: '68%' }} />
                  <span className="tcs-skel tcs-skel--line" style={{ width: '42%' }} />
                </div>
              </div>
              <span className="tcs-skel tcs-skel--line" />
              <span className="tcs-skel tcs-skel--line" style={{ width: '80%' }} />
              <div className="tcs-skel-card__chips">
                <span className="tcs-skel tcs-skel--chip" />
                <span className="tcs-skel tcs-skel--chip" />
                <span className="tcs-skel tcs-skel--chip" />
              </div>
            </article>
          ))}
        </div>
        <span className="tcs-visually-hidden">Đang tải dữ liệu trang chủ…</span>
      </div>
    </section>
  );
}
