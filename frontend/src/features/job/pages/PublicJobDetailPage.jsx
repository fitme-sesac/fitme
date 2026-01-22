import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { http } from '../../../api/http';

/**
 * 원형 매칭률 차트 컴포넌트
 */
const MatchRateCircle = ({ rate, size = 120, strokeWidth = 10, level }) => {
  const radius = (size - strokeWidth) / 2;
  const circumference = radius * 2 * Math.PI;
  const offset = circumference - (rate / 100) * circumference;
  
  const levelColors = {
    EXCELLENT: { stroke: '#10b981', bg: 'rgba(16, 185, 129, 0.1)', text: '#059669' },
    GOOD: { stroke: '#3b82f6', bg: 'rgba(59, 130, 246, 0.1)', text: '#2563eb' },
    MODERATE: { stroke: '#f59e0b', bg: 'rgba(245, 158, 11, 0.1)', text: '#d97706' },
    LOW: { stroke: '#6b7280', bg: 'rgba(107, 114, 128, 0.1)', text: '#4b5563' }
  };
  
  const colors = levelColors[level] || levelColors.LOW;
  
  return (
    <div className="match-circle-container" style={{ position: 'relative', width: size, height: size }}>
      <svg width={size} height={size} style={{ transform: 'rotate(-90deg)' }}>
        {/* 배경 원 */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill={colors.bg}
          stroke="#e5e7eb"
          strokeWidth={strokeWidth}
        />
        {/* 진행 원 */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="transparent"
          stroke={colors.stroke}
          strokeWidth={strokeWidth}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          style={{ transition: 'stroke-dashoffset 0.8s ease-out' }}
        />
      </svg>
      {/* 중앙 텍스트 */}
      <div 
        style={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          textAlign: 'center'
        }}
      >
        <div style={{ fontSize: size * 0.28, fontWeight: 'bold', color: colors.text }}>
          {rate}%
        </div>
        <div style={{ fontSize: size * 0.1, color: '#6b7280' }}>매칭률</div>
      </div>
    </div>
  );
};

/**
 * 기술별 매칭 상세 컴포넌트
 */
const SkillMatchDetail = ({ skillDetails }) => {
  if (!skillDetails || skillDetails.length === 0) return null;
  
  return (
    <div className="skill-match-details">
      {skillDetails.map((skill, idx) => (
        <div 
          key={idx} 
          className={`skill-match-item ${skill.matched ? 'matched' : 'missing'}`}
        >
          <div className="skill-name">
            <span className={`skill-icon ${skill.matched ? 'text-success' : 'text-muted'}`}>
              {skill.matched ? '✓' : '○'}
            </span>
            {skill.skillName}
          </div>
          {skill.matched && (
            <div className="skill-proficiency">
              <div className="proficiency-bar">
                <div 
                  className="proficiency-fill"
                  style={{ width: `${skill.matchScore}%` }}
                />
              </div>
              <span className="proficiency-label">{skill.proficiencyLabel}</span>
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

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
                {(job.salaryDisplay || job.salaryText) && (
                  <div className="alert alert-success mb-0">
                    <i className="bi bi-currency-dollar me-2"></i>
                    <strong>급여:</strong> {job.salaryDisplay || (job.salaryText && `${(job.salaryText / 10000).toLocaleString()}만원`)}
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
            {/* 매칭률 분석 카드 (로그인 사용자만) 또는 로그인 안내 */}
            {job.matchInfo ? (
              <div className="card shadow-sm mb-4 match-analysis-card">
                <div className="card-header bg-white border-0 pb-0">
                  <h6 className="mb-0 fw-bold">
                    <i className="bi bi-graph-up me-2 text-primary"></i>
                    나와의 매칭 분석
                  </h6>
                </div>
                <div className="card-body text-center">
                  {/* 원형 매칭률 */}
                  <div className="d-flex justify-content-center mb-3">
                    <MatchRateCircle 
                      rate={job.matchInfo.overallMatchRate || job.matchInfo.matchRate || 0}
                      level={job.matchInfo.matchLevel || 'LOW'}
                      size={140}
                      strokeWidth={12}
                    />
                  </div>
                  
                  {/* 매칭 레벨 뱃지 */}
                  <div className="mb-3">
                    <span className={`badge match-level-badge ${
                      job.matchInfo.matchLevel === 'EXCELLENT' ? 'bg-success' :
                      job.matchInfo.matchLevel === 'GOOD' ? 'bg-primary' :
                      job.matchInfo.matchLevel === 'MODERATE' ? 'bg-warning text-dark' : 'bg-secondary'
                    }`}>
                      {job.matchInfo.matchLevel === 'EXCELLENT' ? '🎯 최적 매칭!' :
                       job.matchInfo.matchLevel === 'GOOD' ? '👍 좋은 매칭' :
                       job.matchInfo.matchLevel === 'MODERATE' ? '💪 도전 가능' : '📚 학습 필요'}
                    </span>
                  </div>
                  
                  {/* 매칭 요약 */}
                  <div className="match-summary text-start mb-3">
                    <div className="d-flex justify-content-between small text-muted mb-1">
                      <span>일치 기술</span>
                      <span className="text-success fw-bold">
                        {job.matchInfo.matchedStacks?.length || 0}개
                      </span>
                    </div>
                    <div className="d-flex justify-content-between small text-muted">
                      <span>부족 기술</span>
                      <span className="text-danger fw-bold">
                        {job.matchInfo.missingStacks?.length || 0}개
                      </span>
                    </div>
                  </div>
                  
                  {/* 기술별 상세 매칭 */}
                  {job.matchInfo.skillDetails && job.matchInfo.skillDetails.length > 0 && (
                    <div className="skill-details-section text-start">
                      <h6 className="small fw-semibold text-muted mb-2">
                        <i className="bi bi-list-check me-1"></i>기술별 분석
                      </h6>
                      <SkillMatchDetail skillDetails={job.matchInfo.skillDetails} />
                    </div>
                  )}
                  
                  {/* 일치/부족 기술 표시 (skillDetails가 없는 경우) */}
                  {(!job.matchInfo.skillDetails || job.matchInfo.skillDetails.length === 0) && (
                    <>
                      {job.matchInfo.matchedStacks && job.matchInfo.matchedStacks.length > 0 && (
                        <div className="text-start mb-2">
                          <small className="text-muted d-block mb-1">✓ 보유 기술:</small>
                          <div>
                            {job.matchInfo.matchedStacks.map((s, i) => (
                              <span key={i} className="badge bg-success-subtle text-success me-1 mb-1">{s}</span>
                            ))}
                          </div>
                        </div>
                      )}
                      {job.matchInfo.missingStacks && job.matchInfo.missingStacks.length > 0 && (
                        <div className="text-start">
                          <small className="text-muted d-block mb-1">○ 필요 기술:</small>
                          <div>
                            {job.matchInfo.missingStacks.map((s, i) => (
                              <span key={i} className="badge bg-danger-subtle text-danger me-1 mb-1">{s}</span>
                            ))}
                          </div>
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>
            ) : (
              /* 비로그인 시 매칭 안내 카드 */
              <div className="card shadow-sm mb-4 login-prompt-card">
                <div className="card-body text-center py-4">
                  <div className="mb-3">
                    <div 
                      className="rounded-circle bg-light d-inline-flex align-items-center justify-content-center"
                      style={{ width: 80, height: 80 }}
                    >
                      <i className="bi bi-graph-up-arrow text-primary fs-1"></i>
                    </div>
                  </div>
                  <h6 className="fw-bold mb-2">나와의 매칭률 확인하기</h6>
                  <p className="text-muted small mb-3">
                    로그인하면 내 기술 스택과<br/>
                    이 공고의 매칭률을 분석해드려요!
                  </p>
                  <a href="/login" className="btn btn-outline-primary btn-sm">
                    <i className="bi bi-box-arrow-in-right me-1"></i>
                    로그인하기
                  </a>
                </div>
              </div>
            )}
            
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

      {/* 매칭 분석 스타일 */}
      <style>{`
        .match-analysis-card {
          border: none;
          background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
        }
        
        .match-level-badge {
          padding: 0.5em 1em;
          font-size: 0.85rem;
          border-radius: 20px;
        }
        
        .skill-match-details {
          max-height: 250px;
          overflow-y: auto;
        }
        
        .skill-match-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 0.5rem 0;
          border-bottom: 1px solid #f1f5f9;
        }
        
        .skill-match-item:last-child {
          border-bottom: none;
        }
        
        .skill-match-item .skill-name {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          font-size: 0.875rem;
          font-weight: 500;
        }
        
        .skill-match-item.matched .skill-name {
          color: #059669;
        }
        
        .skill-match-item.missing .skill-name {
          color: #9ca3af;
        }
        
        .skill-match-item .skill-icon {
          font-size: 0.75rem;
        }
        
        .skill-match-item .skill-proficiency {
          display: flex;
          align-items: center;
          gap: 0.5rem;
        }
        
        .proficiency-bar {
          width: 50px;
          height: 6px;
          background: #e5e7eb;
          border-radius: 3px;
          overflow: hidden;
        }
        
        .proficiency-fill {
          height: 100%;
          background: linear-gradient(90deg, #10b981, #059669);
          border-radius: 3px;
          transition: width 0.5s ease;
        }
        
        .proficiency-label {
          font-size: 0.7rem;
          color: #6b7280;
          min-width: 32px;
        }
        
        .bg-success-subtle {
          background-color: rgba(16, 185, 129, 0.15) !important;
        }
        
        .bg-danger-subtle {
          background-color: rgba(239, 68, 68, 0.15) !important;
        }
        
        /* 스크롤바 스타일 */
        .skill-match-details::-webkit-scrollbar {
          width: 4px;
        }
        
        .skill-match-details::-webkit-scrollbar-track {
          background: #f1f5f9;
          border-radius: 2px;
        }
        
        .skill-match-details::-webkit-scrollbar-thumb {
          background: #cbd5e1;
          border-radius: 2px;
        }
        
        .skill-match-details::-webkit-scrollbar-thumb:hover {
          background: #94a3b8;
        }
        
        .login-prompt-card {
          border: 2px dashed #e2e8f0;
          background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
        }
      `}</style>
    </main>
  );
}
