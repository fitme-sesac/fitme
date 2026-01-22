import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { http } from '../../../api/http';

/**
 * 공개 채용공고 상세 페이지
 * - 모든 사용자(비로그인 포함)가 볼 수 있는 채용공고 상세
 * - /jobs/:jobId 경로로 접근
 */
export default function PublicJobDetailPage() {
  const { jobId } = useParams();
  const navigate = useNavigate();
  
  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchJob();
  }, [jobId]);

  const fetchJob = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await http.get(`/api/public/jobs/${jobId}`);
      setJob(res.data);
    } catch (err) {
      if (err.response?.status === 404) {
        setError('해당 채용공고를 찾을 수 없습니다.');
      } else {
        setError(err.response?.data?.error || '채용공고를 불러오는데 실패했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <main className="main">
        <div className="container py-5">
          <div className="text-center">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <p className="mt-3 text-muted">채용공고를 불러오는 중...</p>
          </div>
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main className="main">
        <div className="container py-5">
          <div className="text-center">
            <i className="bi bi-exclamation-triangle text-warning fs-1 d-block mb-3"></i>
            <h4 className="text-muted">{error}</h4>
            <Link to="/jobs" className="btn btn-primary mt-3">
              <i className="bi bi-arrow-left me-2"></i>채용공고 목록으로
            </Link>
          </div>
        </div>
      </main>
    );
  }

  if (!job) {
    return null;
  }

  return (
    <main className="main">
      <div className="container py-4">
        {/* 뒤로가기 */}
        <nav aria-label="breadcrumb" className="mb-4">
          <ol className="breadcrumb">
            <li className="breadcrumb-item">
              <Link to="/">홈</Link>
            </li>
            <li className="breadcrumb-item">
              <Link to="/jobs">채용공고</Link>
            </li>
            <li className="breadcrumb-item active" aria-current="page">
              {job.title}
            </li>
          </ol>
        </nav>

        <div className="row">
          {/* 메인 컨텐츠 */}
          <div className="col-lg-8">
            {/* 공고 헤더 */}
            <div className="card shadow-sm mb-4">
              <div className="card-body p-4">
                {/* 회사 정보 */}
                <div className="d-flex align-items-center mb-4">
                  <div 
                    className="rounded-circle bg-light d-flex align-items-center justify-content-center me-3"
                    style={{ width: 72, height: 72, overflow: 'hidden' }}
                  >
                    {job.companyLogoUrl ? (
                      <img 
                        src={job.companyLogoUrl} 
                        alt={job.companyName}
                        className="img-fluid"
                        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                      />
                    ) : (
                      <i className="bi bi-building text-muted fs-2"></i>
                    )}
                  </div>
                  <div>
                    <h5 className="mb-1 fw-bold text-primary">{job.companyName || '회사명 미등록'}</h5>
                    {job.location && (
                      <p className="mb-0 text-muted">
                        <i className="bi bi-geo-alt me-1"></i>
                        {job.location}
                      </p>
                    )}
                  </div>
                </div>

                {/* 공고 제목 */}
                <h2 className="mb-3">{job.title}</h2>

                {/* 메타 정보 */}
                <div className="d-flex flex-wrap gap-3 text-muted mb-4">
                  <span>
                    <i className="bi bi-eye me-1"></i>
                    조회 {job.viewCount || 0}
                  </span>
                  <span>
                    <i className="bi bi-people me-1"></i>
                    지원 {job.applicationCount || 0}
                  </span>
                  <span>
                    <i className="bi bi-calendar me-1"></i>
                    {job.createdAt && new Date(job.createdAt).toLocaleDateString('ko-KR')} 등록
                  </span>
                </div>

                {/* 기술 스택 */}
                {job.stack && (
                  <div className="mb-4">
                    <h6 className="fw-semibold mb-2">
                      <i className="bi bi-code-slash me-2"></i>기술 스택
                    </h6>
                    <div>
                      {job.stack.split(',').map((tech, idx) => (
                        <Link
                          key={idx}
                          to={`/jobs?stack=${encodeURIComponent(tech.trim())}`}
                          className="badge bg-primary me-2 mb-2 text-decoration-none"
                          style={{ fontSize: '0.9rem', padding: '0.5em 0.8em' }}
                        >
                          {tech.trim()}
                        </Link>
                      ))}
                    </div>
                  </div>
                )}

                {/* 급여 정보 */}
                {job.salaryText && (
                  <div className="alert alert-success mb-0">
                    <i className="bi bi-currency-dollar me-2"></i>
                    <strong>급여:</strong> {job.salaryText}
                  </div>
                )}
              </div>
            </div>

            {/* 상세 설명 */}
            <div className="card shadow-sm mb-4">
              <div className="card-header bg-white">
                <h5 className="mb-0">
                  <i className="bi bi-file-text me-2"></i>상세 내용
                </h5>
              </div>
              <div className="card-body p-4">
                {job.summary && (
                  <div className="alert alert-light border mb-4">
                    <h6 className="fw-semibold mb-2">
                      <i className="bi bi-lightning me-2 text-warning"></i>요약
                    </h6>
                    <p className="mb-0">{job.summary}</p>
                  </div>
                )}
                
                <div 
                  className="job-description"
                  style={{ 
                    whiteSpace: 'pre-wrap', 
                    lineHeight: '1.8',
                    fontSize: '1rem'
                  }}
                >
                  {job.description || '상세 내용이 없습니다.'}
                </div>
              </div>
            </div>
          </div>

          {/* 사이드바 */}
          <div className="col-lg-4">
            {/* 지원하기 카드 */}
            <div className="card shadow-sm mb-4 sticky-top" style={{ top: '100px' }}>
              <div className="card-body p-4 text-center">
                <h5 className="fw-bold mb-3">관심 있는 공고인가요?</h5>
                <p className="text-muted mb-4">
                  이력서를 등록하고 지원해보세요!
                </p>
                <div className="d-grid gap-2">
                  <button 
                    className="btn btn-primary btn-lg"
                    onClick={() => {
                      // 로그인 체크 후 지원 페이지로 이동
                      // 일단은 알림만 표시
                      alert('지원 기능은 준비 중입니다. 로그인 후 이용해주세요.');
                    }}
                  >
                    <i className="bi bi-send me-2"></i>지원하기
                  </button>
                  <button 
                    className="btn btn-outline-secondary"
                    onClick={() => {
                      // 스크랩 기능
                      alert('스크랩 기능은 준비 중입니다.');
                    }}
                  >
                    <i className="bi bi-bookmark me-2"></i>스크랩
                  </button>
                </div>
              </div>
            </div>

            {/* 회사 정보 카드 */}
            <div className="card shadow-sm mb-4">
              <div className="card-header bg-white">
                <h6 className="mb-0">
                  <i className="bi bi-building me-2"></i>기업 정보
                </h6>
              </div>
              <div className="card-body">
                <div className="text-center mb-3">
                  <div 
                    className="rounded-circle bg-light d-inline-flex align-items-center justify-content-center mb-2"
                    style={{ width: 80, height: 80, overflow: 'hidden' }}
                  >
                    {job.companyLogoUrl ? (
                      <img 
                        src={job.companyLogoUrl} 
                        alt={job.companyName}
                        className="img-fluid"
                        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                      />
                    ) : (
                      <i className="bi bi-building text-muted fs-2"></i>
                    )}
                  </div>
                  <h6 className="fw-bold mb-0">{job.companyName || '회사명 미등록'}</h6>
                </div>
                {job.location && (
                  <p className="mb-2 small">
                    <i className="bi bi-geo-alt text-muted me-2"></i>
                    {job.location}
                  </p>
                )}
              </div>
            </div>

            {/* 공유하기 */}
            <div className="card shadow-sm">
              <div className="card-body">
                <h6 className="fw-semibold mb-3">
                  <i className="bi bi-share me-2"></i>공유하기
                </h6>
                <div className="d-flex gap-2">
                  <button 
                    className="btn btn-outline-secondary btn-sm flex-fill"
                    onClick={() => {
                      navigator.clipboard.writeText(window.location.href);
                      alert('링크가 복사되었습니다!');
                    }}
                  >
                    <i className="bi bi-link-45deg"></i> 링크 복사
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* 목록으로 버튼 */}
        <div className="text-center mt-4">
          <Link to="/jobs" className="btn btn-outline-primary">
            <i className="bi bi-list me-2"></i>채용공고 목록으로
          </Link>
        </div>
      </div>
    </main>
  );
}
