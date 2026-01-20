import React, { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { http } from '../../../api/http';

/**
 * 공개 채용공고 목록 페이지
 * - 모든 사용자(비로그인 포함)가 볼 수 있는 채용공고 목록
 * - /jobs 경로로 접근
 */
export default function PublicJobListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pagination, setPagination] = useState({
    page: 0,
    totalPages: 0,
    totalElements: 0,
  });
  
  // 검색/필터 상태
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '');
  const [selectedStack, setSelectedStack] = useState(searchParams.get('stack') || '');
  const [selectedLocation, setSelectedLocation] = useState(searchParams.get('location') || '');

  // 인기 기술 스택 (영어, 한글 라벨)
  const popularStacks = [
    { en: 'Java', ko: '자바' },
    { en: 'Python', ko: '파이썬' },
    { en: 'JavaScript', ko: 'JS' },
    { en: 'React', ko: '리액트' },
    { en: 'Spring', ko: '스프링' },
    { en: 'Node.js', ko: '노드' },
    { en: 'TypeScript', ko: 'TS' },
    { en: 'AWS', ko: '클라우드' },
  ];
  
  // 주요 지역
  const popularLocations = ['서울', '경기', '인천', '부산', '대구', '대전', '광주', '제주'];

  useEffect(() => {
    const page = parseInt(searchParams.get('page') || '0', 10);
    fetchJobs(page);
  }, [searchParams]);

  const fetchJobs = async (page) => {
    try {
      setLoading(true);
      setError(null);
      
      const params = { page, size: 12 };
      if (keyword) params.keyword = keyword;
      if (selectedStack) params.stack = selectedStack;
      if (selectedLocation) params.location = selectedLocation;
      
      const res = await http.get('/api/public/jobs', { params });
      setJobs(res.data.jobs || []);
      setPagination({
        page: res.data.page || 0,
        totalPages: res.data.totalPages || 0,
        totalElements: res.data.totalElements || 0,
      });
    } catch (err) {
      setError(err.response?.data?.error || '채용공고를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (keyword) params.set('keyword', keyword);
    if (selectedStack) params.set('stack', selectedStack);
    if (selectedLocation) params.set('location', selectedLocation);
    params.set('page', '0');
    setSearchParams(params);
  };

  const handleStackFilter = (stack) => {
    setSelectedStack(stack === selectedStack ? '' : stack);
    const params = new URLSearchParams(searchParams);
    if (stack === selectedStack) {
      params.delete('stack');
    } else {
      params.set('stack', stack);
    }
    params.set('page', '0');
    setSearchParams(params);
  };

  const handleLocationFilter = (location) => {
    setSelectedLocation(location === selectedLocation ? '' : location);
    const params = new URLSearchParams(searchParams);
    if (location === selectedLocation) {
      params.delete('location');
    } else {
      params.set('location', location);
    }
    params.set('page', '0');
    setSearchParams(params);
  };

  const handlePageChange = (newPage) => {
    const params = new URLSearchParams(searchParams);
    params.set('page', newPage.toString());
    setSearchParams(params);
  };

  const clearFilters = () => {
    setKeyword('');
    setSelectedStack('');
    setSelectedLocation('');
    setSearchParams({});
  };

  return (
    <main className="main">
      <div className="container py-4">
        {/* 페이지 헤더 */}
        <div className="text-center mb-5">
          <h1 className="display-5 fw-bold mb-3">
            <i className="bi bi-briefcase-fill me-2 text-primary"></i>
            채용공고
          </h1>
          <p className="text-muted fs-5">
            다양한 기업의 채용공고를 확인하고 지원해보세요
          </p>
        </div>

        {/* 검색 폼 */}
        <div className="card shadow-sm mb-4">
          <div className="card-body">
            <form onSubmit={handleSearch}>
              <div className="row g-3">
                <div className="col-md-8">
                  <div className="input-group input-group-lg">
                    <span className="input-group-text bg-white border-end-0">
                      <i className="bi bi-search text-muted"></i>
                    </span>
                    <input
                      type="text"
                      className="form-control border-start-0"
                      placeholder="직무, 기술스택으로 검색... (예: Java, 자바, React, 리액트)"
                      value={keyword}
                      onChange={(e) => setKeyword(e.target.value)}
                    />
                  </div>
                  <small className="text-muted mt-1 d-block">
                    <i className="bi bi-lightbulb me-1"></i>
                    한글로도 검색 가능해요! (자바 → Java, 리액트 → React, 파이썬 → Python)
                  </small>
                </div>
                <div className="col-md-4 d-grid">
                  <button type="submit" className="btn btn-primary btn-lg">
                    <i className="bi bi-search me-2"></i>검색
                  </button>
                </div>
              </div>
            </form>

            {/* 필터 태그 */}
            <div className="mt-4">
              <div className="mb-3">
                <small className="text-muted fw-semibold me-2">기술스택:</small>
                {popularStacks.map((stack) => (
                  <button
                    key={stack.en}
                    type="button"
                    className={`btn btn-sm me-2 mb-2 ${
                      selectedStack === stack.en 
                        ? 'btn-primary' 
                        : 'btn-outline-secondary'
                    }`}
                    onClick={() => handleStackFilter(stack.en)}
                    title={`${stack.ko}로도 검색 가능`}
                  >
                    {stack.en}
                  </button>
                ))}
              </div>
              <div>
                <small className="text-muted fw-semibold me-2">지역:</small>
                {popularLocations.map((location) => (
                  <button
                    key={location}
                    type="button"
                    className={`btn btn-sm me-2 mb-2 ${
                      selectedLocation === location 
                        ? 'btn-primary' 
                        : 'btn-outline-secondary'
                    }`}
                    onClick={() => handleLocationFilter(location)}
                  >
                    {location}
                  </button>
                ))}
              </div>
            </div>

            {/* 활성 필터 표시 */}
            {(keyword || selectedStack || selectedLocation) && (
              <div className="mt-3 pt-3 border-top">
                <small className="text-muted">적용된 필터: </small>
                {keyword && (
                  <span className="badge bg-primary me-2">
                    검색어: {keyword}
                    <button 
                      type="button" 
                      className="btn-close btn-close-white ms-2" 
                      style={{ fontSize: '0.6rem' }}
                      onClick={() => {
                        setKeyword('');
                        const params = new URLSearchParams(searchParams);
                        params.delete('keyword');
                        setSearchParams(params);
                      }}
                    ></button>
                  </span>
                )}
                {selectedStack && (
                  <span className="badge bg-success me-2">
                    기술: {selectedStack}
                    <button 
                      type="button" 
                      className="btn-close btn-close-white ms-2" 
                      style={{ fontSize: '0.6rem' }}
                      onClick={() => handleStackFilter(selectedStack)}
                    ></button>
                  </span>
                )}
                {selectedLocation && (
                  <span className="badge bg-info me-2">
                    지역: {selectedLocation}
                    <button 
                      type="button" 
                      className="btn-close btn-close-white ms-2" 
                      style={{ fontSize: '0.6rem' }}
                      onClick={() => handleLocationFilter(selectedLocation)}
                    ></button>
                  </span>
                )}
                <button 
                  type="button" 
                  className="btn btn-sm btn-link text-danger"
                  onClick={clearFilters}
                >
                  <i className="bi bi-x-circle me-1"></i>전체 초기화
                </button>
              </div>
            )}
          </div>
        </div>

        {/* 결과 정보 */}
        <div className="d-flex justify-content-between align-items-center mb-3">
          <span className="text-muted">
            총 <strong className="text-primary">{pagination.totalElements}</strong>개의 채용공고
          </span>
        </div>

        {/* 에러 메시지 */}
        {error && (
          <div className="alert alert-danger">
            <i className="bi bi-exclamation-triangle me-2"></i>
            {error}
          </div>
        )}

        {/* 로딩 상태 */}
        {loading ? (
          <div className="text-center py-5">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <p className="mt-3 text-muted">채용공고를 불러오는 중...</p>
          </div>
        ) : jobs.length === 0 ? (
          <div className="text-center py-5">
            <i className="bi bi-inbox fs-1 text-muted d-block mb-3"></i>
            <h5 className="text-muted">검색 결과가 없습니다</h5>
            <p className="text-muted">다른 검색어나 필터를 사용해보세요</p>
            <button 
              type="button" 
              className="btn btn-outline-primary"
              onClick={clearFilters}
            >
              필터 초기화
            </button>
          </div>
        ) : (
          <>
            {/* 채용공고 카드 그리드 */}
            <div className="row g-4">
              {jobs.map((job) => (
                <div key={job.jobId} className="col-md-6 col-lg-4">
                  <Link 
                    to={`/jobs/${job.jobId}`} 
                    className="text-decoration-none"
                  >
                    <div className="card h-100 shadow-sm border-0 job-card">
                      <div className="card-body">
                        {/* 회사 정보 */}
                        <div className="d-flex align-items-center mb-3">
                          <div 
                            className="rounded-circle bg-light d-flex align-items-center justify-content-center me-3"
                            style={{ width: 48, height: 48, overflow: 'hidden' }}
                          >
                            {job.companyLogoUrl ? (
                              <img 
                                src={job.companyLogoUrl} 
                                alt={job.companyName}
                                className="img-fluid"
                                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                              />
                            ) : (
                              <i className="bi bi-building text-muted fs-4"></i>
                            )}
                          </div>
                          <div>
                            <h6 className="mb-0 text-dark fw-semibold">{job.companyName || '회사명 미등록'}</h6>
                            {job.location && (
                              <small className="text-muted">
                                <i className="bi bi-geo-alt me-1"></i>
                                {job.location}
                              </small>
                            )}
                          </div>
                        </div>

                        {/* 공고 제목 */}
                        <h5 className="card-title text-dark mb-2" style={{ 
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          display: '-webkit-box',
                          WebkitLineClamp: 2,
                          WebkitBoxOrient: 'vertical'
                        }}>
                          {job.title}
                        </h5>

                        {/* 요약 */}
                        {job.summary && (
                          <p className="card-text text-muted small mb-3" style={{
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            display: '-webkit-box',
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical'
                          }}>
                            {job.summary}
                          </p>
                        )}

                        {/* 기술 스택 */}
                        {job.stack && (
                          <div className="mb-3">
                            {job.stack.split(',').slice(0, 3).map((tech, idx) => (
                              <span 
                                key={idx} 
                                className="badge bg-light text-dark me-1 mb-1"
                              >
                                {tech.trim()}
                              </span>
                            ))}
                            {job.stack.split(',').length > 3 && (
                              <span className="badge bg-light text-muted">
                                +{job.stack.split(',').length - 3}
                              </span>
                            )}
                          </div>
                        )}

                        {/* 급여 정보 */}
                        {job.salaryText && (
                          <p className="mb-0 text-success small fw-semibold">
                            <i className="bi bi-currency-dollar me-1"></i>
                            {job.salaryText}
                          </p>
                        )}
                      </div>

                      {/* 카드 푸터 */}
                      <div className="card-footer bg-white border-top-0 pt-0">
                        <div className="d-flex justify-content-between align-items-center text-muted small">
                          <span>
                            <i className="bi bi-eye me-1"></i>
                            {job.viewCount || 0}
                          </span>
                          <span>
                            <i className="bi bi-people me-1"></i>
                            지원 {job.applicationCount || 0}
                          </span>
                          <span>
                            {job.createdAt && new Date(job.createdAt).toLocaleDateString('ko-KR')}
                          </span>
                        </div>
                      </div>
                    </div>
                  </Link>
                </div>
              ))}
            </div>

            {/* 페이지네이션 */}
            {pagination.totalPages > 1 && (
              <nav className="mt-5">
                <ul className="pagination justify-content-center">
                  <li className={`page-item ${pagination.page === 0 ? 'disabled' : ''}`}>
                    <button 
                      className="page-link" 
                      onClick={() => handlePageChange(pagination.page - 1)}
                      disabled={pagination.page === 0}
                    >
                      <i className="bi bi-chevron-left"></i>
                    </button>
                  </li>
                  {[...Array(Math.min(pagination.totalPages, 10))].map((_, i) => {
                    // 페이지 번호 계산 (현재 페이지를 중심으로)
                    let pageNum = i;
                    if (pagination.totalPages > 10) {
                      const half = 5;
                      const start = Math.max(0, Math.min(pagination.page - half, pagination.totalPages - 10));
                      pageNum = start + i;
                    }
                    return (
                      <li 
                        key={pageNum} 
                        className={`page-item ${pagination.page === pageNum ? 'active' : ''}`}
                      >
                        <button 
                          className="page-link" 
                          onClick={() => handlePageChange(pageNum)}
                        >
                          {pageNum + 1}
                        </button>
                      </li>
                    );
                  })}
                  <li className={`page-item ${pagination.page === pagination.totalPages - 1 ? 'disabled' : ''}`}>
                    <button 
                      className="page-link" 
                      onClick={() => handlePageChange(pagination.page + 1)}
                      disabled={pagination.page === pagination.totalPages - 1}
                    >
                      <i className="bi bi-chevron-right"></i>
                    </button>
                  </li>
                </ul>
              </nav>
            )}
          </>
        )}
      </div>

      {/* 스타일 */}
      <style>{`
        .job-card {
          transition: transform 0.2s ease, box-shadow 0.2s ease;
        }
        .job-card:hover {
          transform: translateY(-4px);
          box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15) !important;
        }
      `}</style>
    </main>
  );
}
