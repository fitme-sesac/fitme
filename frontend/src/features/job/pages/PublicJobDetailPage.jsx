import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { http } from '../../../api/http';
import ScrapButton from '../components/ScrapButton';

/**
 * 공개 채용공고 상세 페이지
 * - 모든 사용자(비로그인 포함)가 볼 수 있는 채용공고 상세
 * - /jobs/:jobId 경로로 접근
 * - 로그인 사용자에게는 기술 스택 매칭 정보 표시
 */
export default function PublicJobDetailPage() {
  const { jobId } = useParams();
  const navigate = useNavigate();
  
  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showMatchDetail, setShowMatchDetail] = useState(false); // 매칭 상세 토글

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
                      {job.stack.split(',').filter(tech => tech.trim()).map((tech, idx) => {
                        const isMatched = job.matchInfo?.matchedStacks?.some(
                          m => m.toLowerCase() === tech.trim().toLowerCase()
                        );
                        return (
                          <Link
                            key={idx}
                            to={`/jobs?stack=${encodeURIComponent(tech.trim())}`}
                            className={`badge me-2 mb-2 text-decoration-none ${
                              job.matchInfo 
                                ? (isMatched ? 'bg-success' : 'bg-secondary') 
                                : 'bg-primary'
                            }`}
                            style={{ fontSize: '0.9rem', padding: '0.5em 0.8em' }}
                            title={job.matchInfo ? (isMatched ? '보유한 기술' : '부족한 기술') : ''}
                          >
                            {isMatched && <i className="bi bi-check-circle me-1"></i>}
                            {tech.trim()}
                          </Link>
                        );
                      })}
                    </div>
                  </div>
                )}

                {/* 매칭 정보 요약 배너 (로그인 사용자만) */}
                {job.matchInfo && (
                  <div className="alert alert-info mb-4 d-flex align-items-center">
                    <i className="bi bi-graph-up-arrow fs-4 me-3"></i>
                    <div>
                      <strong>나와의 매칭률: {job.matchInfo.matchRate || job.matchInfo.overallMatchRate || 0}%</strong>
                      <span className="text-muted ms-2">
                        ({job.matchInfo.matchedStacks?.length || 0}개 기술 일치)
                      </span>
                    </div>
                    <span className="ms-auto badge bg-white text-info">
                      우측에서 상세 확인 →
                    </span>
                  </div>
                )}

                {/* 급여 및 경력 정보 */}
                <div className="d-flex flex-wrap gap-3 mb-0">
                  {job.salaryText && (
                    <div className="alert alert-success mb-0 flex-grow-1">
                      <i className="bi bi-currency-dollar me-2"></i>
                      <strong>급여:</strong> {job.salaryText}
                    </div>
                  )}
                  <div className="alert alert-primary mb-0 flex-grow-1">
                    <i className="bi bi-briefcase me-2"></i>
                    <strong>경력:</strong> {
                      job.requiredExperience === null || job.requiredExperience === undefined || job.requiredExperience === 0
                        ? '신입/무관'
                        : `${job.requiredExperience}년 이상`
                    }
                  </div>
                </div>
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

            {/* 채용공고 이미지 갤러리 */}
            {job.images && job.images.length > 0 && (
              <div className="card shadow-sm mb-4">
                <div className="card-header bg-white">
                  <h5 className="mb-0">
                    <i className="bi bi-images me-2"></i>채용 공고 이미지
                  </h5>
                </div>
                <div className="card-body p-4">
                  <div className="row g-3">
                    {job.images.map((imageUrl, idx) => (
                      <div key={idx} className="col-12 col-md-6">
                        <div 
                          className="rounded overflow-hidden border"
                          style={{ cursor: 'pointer' }}
                          onClick={() => window.open(imageUrl, '_blank')}
                        >
                          <img 
                            src={imageUrl} 
                            alt={`채용공고 이미지 ${idx + 1}`}
                            className="img-fluid w-100"
                            style={{ 
                              objectFit: 'cover',
                              maxHeight: '300px'
                            }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                  <p className="text-muted small mt-2 mb-0">
                    <i className="bi bi-info-circle me-1"></i>
                    이미지를 클릭하면 원본 크기로 볼 수 있습니다.
                  </p>
                </div>
              </div>
            )}
          </div>

          {/* 사이드바 */}
          <div className="col-lg-4">
            {/* 매칭율 카드 (로그인 사용자만 - 맨 위) */}
            {job.matchInfo && (
              <div className="card shadow-sm mb-4 match-card">
                <div className="card-header bg-gradient-match text-white">
                  <h6 className="mb-0">
                    <i className="bi bi-graph-up me-2"></i>나와의 매칭률
                  </h6>
                </div>
                <div className="card-body p-3">
                  {/* 메인 매칭률 원형 그래프 */}
                  <div className="text-center mb-3">
                    <div 
                      className="match-circle-large mx-auto"
                      style={{
                        '--match-rate': job.matchInfo.matchRate || job.matchInfo.overallMatchRate || 0,
                        '--match-color': 
                          job.matchInfo.matchLevel === 'EXCELLENT' ? '#28a745' :
                          job.matchInfo.matchLevel === 'GOOD' ? '#17a2b8' :
                          job.matchInfo.matchLevel === 'MODERATE' ? '#ffc107' : '#6c757d'
                      }}
                    >
                      <span className="match-circle-text-large">
                        <strong>{job.matchInfo.matchRate || job.matchInfo.overallMatchRate || 0}%</strong>
                        <small>종합 일치</small>
                      </span>
                    </div>
                    <div className="mt-2">
                      <span className={`badge ${
                        job.matchInfo.matchLevel === 'EXCELLENT' ? 'bg-success' :
                        job.matchInfo.matchLevel === 'GOOD' ? 'bg-info' :
                        job.matchInfo.matchLevel === 'MODERATE' ? 'bg-warning' : 'bg-secondary'
                      }`}>
                        {job.matchInfo.matchLevel === 'EXCELLENT' && '🎯 아주 잘 맞아요!'}
                        {job.matchInfo.matchLevel === 'GOOD' && '👍 잘 맞는 공고예요'}
                        {job.matchInfo.matchLevel === 'MODERATE' && '📚 도전해볼 만해요'}
                        {job.matchInfo.matchLevel === 'LOW' && '🌱 새로운 기회예요'}
                      </span>
                    </div>
                  </div>

                  {/* 세부 매칭률 */}
                  <div className="row g-2 mb-3">
                    <div className="col-4">
                      <div className="mini-stat-card bg-light rounded p-2 text-center">
                        <div 
                          className="mini-circle mx-auto mb-1"
                          style={{ '--rate': job.matchInfo.stackMatchRate || 0, '--color': '#28a745' }}
                        >
                          <span>{job.matchInfo.stackMatchRate || 0}%</span>
                        </div>
                        <small className="text-muted d-block">기술스택</small>
                      </div>
                    </div>
                    <div className="col-4">
                      <div className="mini-stat-card bg-light rounded p-2 text-center">
                        <div 
                          className="mini-circle mx-auto mb-1"
                          style={{ '--rate': job.matchInfo.experienceMatchRate || 0, '--color': '#fd7e14' }}
                        >
                          <span>{job.matchInfo.experienceMatchRate || 0}%</span>
                        </div>
                        <small className="text-muted d-block">경력</small>
                      </div>
                    </div>
                    <div className="col-4">
                      <div className="mini-stat-card bg-light rounded p-2 text-center">
                        <div 
                          className="mini-circle mx-auto mb-1"
                          style={{ '--rate': job.matchInfo.vectorMatchRate || 0, '--color': '#6f42c1' }}
                        >
                          <span>{job.matchInfo.vectorMatchRate || 0}%</span>
                        </div>
                        <small className="text-muted d-block">AI분석</small>
                      </div>
                    </div>
                  </div>

                  {/* 스킬 요약 */}
                  <div className="skill-summary">
                    <div className="d-flex justify-content-between align-items-center small mb-2">
                      <span className="text-success">
                        <i className="bi bi-check-circle-fill me-1"></i>
                        보유 {job.matchInfo.matchedStacks?.length || 0}개
                      </span>
                      <span className="text-warning">
                        <i className="bi bi-exclamation-circle-fill me-1"></i>
                        부족 {job.matchInfo.missingStacks?.length || 0}개
                      </span>
                    </div>
                    <div className="progress" style={{ height: '8px' }}>
                      <div 
                        className="progress-bar bg-success" 
                        style={{ 
                          width: `${job.matchInfo.requiredStacks?.length > 0 
                            ? (job.matchInfo.matchedStacks?.length || 0) / job.matchInfo.requiredStacks.length * 100 
                            : 0}%` 
                        }}
                      ></div>
                    </div>
                  </div>

                  {/* 상세 보기 토글 */}
                  <button 
                    className="btn btn-sm btn-outline-primary w-100 mt-3"
                    onClick={() => setShowMatchDetail(!showMatchDetail)}
                  >
                    <i className={`bi bi-chevron-${showMatchDetail ? 'up' : 'down'} me-1`}></i>
                    상세 보기
                  </button>

                  {/* 토글 상세 */}
                  {showMatchDetail && (
                    <div className="match-detail-sticky mt-3 pt-3 border-top">
                      <div className="mb-2">
                        <small className="text-success fw-semibold">
                          <i className="bi bi-check-circle me-1"></i>보유한 기술
                        </small>
                        <div className="mt-1">
                          {job.matchInfo.matchedStacks?.length > 0 ? (
                            job.matchInfo.matchedStacks.map((s, i) => (
                              <span key={i} className="badge bg-success-subtle text-success me-1 mb-1" style={{ fontSize: '0.7rem' }}>{s}</span>
                            ))
                          ) : (
                            <span className="text-muted" style={{ fontSize: '0.75rem' }}>없음</span>
                          )}
                        </div>
                      </div>
                      <div>
                        <small className="text-warning fw-semibold">
                          <i className="bi bi-exclamation-circle me-1"></i>부족한 기술
                        </small>
                        <div className="mt-1">
                          {job.matchInfo.missingStacks?.length > 0 ? (
                            job.matchInfo.missingStacks.map((s, i) => (
                              <span key={i} className="badge bg-warning-subtle text-warning me-1 mb-1" style={{ fontSize: '0.7rem' }}>{s}</span>
                            ))
                          ) : (
                            <span className="text-muted" style={{ fontSize: '0.75rem' }}>없음 🎉</span>
                          )}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

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
            <div className="card shadow-sm mb-4">
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

            {/* 지원하기 카드 (sticky - 따라다님) */}
            <div className="card shadow-sm sticky-apply-card">
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
                  <ScrapButton 
                    jobId={job.jobId || jobId}
                    size="lg"
                    showText={true}
                    onToggle={(scraped, message) => {
                      // 스크랩 토글 완료 시 메시지 표시 (선택사항)
                      console.log(message);
                    }}
                  />
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

      {/* 매칭 정보 스타일 */}
      <style>{`
        /* 매칭 카드 (일반) */
        .match-card {
          border: 2px solid #0d6efd;
          border-radius: 12px;
          overflow: hidden;
          box-shadow: 0 8px 25px rgba(13, 110, 253, 0.2);
        }
        
        /* Sticky 지원하기 카드 */
        .sticky-apply-card {
          position: sticky;
          top: 100px;
          z-index: 10;
          border: 2px solid #28a745;
          border-radius: 12px;
          overflow: hidden;
          box-shadow: 0 8px 25px rgba(40, 167, 69, 0.2);
          background: linear-gradient(180deg, #fff 0%, #f8fff8 100%);
        }
        
        /* 그라데이션 헤더 */
        .bg-gradient-match {
          background: linear-gradient(135deg, #0d6efd 0%, #6610f2 100%);
        }

        /* 큰 원형 그래프 */
        .match-circle-large {
          width: 100px;
          height: 100px;
          border-radius: 50%;
          background: conic-gradient(
            var(--match-color) calc(var(--match-rate) * 1%),
            #e9ecef calc(var(--match-rate) * 1%)
          );
          display: flex;
          align-items: center;
          justify-content: center;
          position: relative;
          box-shadow: 0 4px 15px rgba(0,0,0,0.1);
        }
        .match-circle-large::before {
          content: '';
          position: absolute;
          width: 70px;
          height: 70px;
          border-radius: 50%;
          background: white;
        }
        .match-circle-text-large {
          position: relative;
          z-index: 1;
          display: flex;
          flex-direction: column;
          align-items: center;
          line-height: 1.2;
        }
        .match-circle-text-large strong {
          font-size: 1.5rem;
          color: #333;
        }
        .match-circle-text-large small {
          font-size: 0.65rem;
          color: #666;
        }

        /* 미니 원형 차트 */
        .mini-circle {
          width: 45px;
          height: 45px;
          border-radius: 50%;
          background: conic-gradient(
            var(--color) calc(var(--rate) * 1%),
            #dee2e6 calc(var(--rate) * 1%)
          );
          display: flex;
          align-items: center;
          justify-content: center;
          position: relative;
        }
        .mini-circle::before {
          content: '';
          position: absolute;
          width: 32px;
          height: 32px;
          border-radius: 50%;
          background: #f8f9fa;
        }
        .mini-circle span {
          position: relative;
          z-index: 1;
          font-size: 0.7rem;
          font-weight: bold;
          color: #333;
        }

        /* 매칭 상세 애니메이션 */
        .match-detail-sticky {
          animation: slideDown 0.2s ease;
        }
        @keyframes slideDown {
          from {
            opacity: 0;
            transform: translateY(-10px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }

        /* 뱃지 커스텀 */
        .bg-success-subtle {
          background-color: rgba(40, 167, 69, 0.15) !important;
        }
        .bg-warning-subtle {
          background-color: rgba(255, 193, 7, 0.15) !important;
        }

        /* 미니 스탯 카드 */
        .mini-stat-card {
          transition: transform 0.2s ease;
        }
        .mini-stat-card:hover {
          transform: scale(1.05);
        }

        /* 스킬 요약 */
        .skill-summary .progress {
          border-radius: 4px;
          background-color: #ffc107;
        }
      `}</style>
    </main>
  );
}
