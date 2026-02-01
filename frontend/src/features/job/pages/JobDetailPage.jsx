import React, { useEffect, useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { http } from '../../../api/http';
import { Sidebar } from '@/components/layout/Sidebar';
import { Header } from '@/components/layout/Header';

/**
 * 채용공고 상세 페이지 (기업 회원용)
 * - 공개 채용공고 상세(PublicJobDetailPage)와 유사한 레이아웃
 * - 기업 전용: 수정, 삭제, 지원자 목록(대시보드 링크) 유지
 */
export default function JobDetailPage() {
  const { jobId } = useParams();
  const navigate = useNavigate();

  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    fetchJob();
  }, [jobId]);

  const fetchJob = async () => {
    try {
      setLoading(true);
      const res = await http.get(`/api/jobs/${jobId}`);
      setJob(res.data);
    } catch (err) {
      if (err.response?.status === 401) {
        navigate('/Login');
        return;
      }
      setError(err.response?.data?.error || '채용공고를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('정말 이 채용공고를 삭제하시겠습니까?')) return;

    try {
      setDeleting(true);
      await http.delete(`/api/jobs/${jobId}`);
      alert('삭제되었습니다.');
      navigate('/employer/jobs');
    } catch (err) {
      alert(err.response?.data?.error || '삭제에 실패했습니다.');
    } finally {
      setDeleting(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'OPEN':
        return <span className="badge bg-success">모집중</span>;
      case 'DRAFT':
        return <span className="badge bg-secondary">임시저장</span>;
      case 'CLOSED':
        return <span className="badge bg-danger">마감</span>;
      default:
        return <span className="badge bg-light text-dark">{status}</span>;
    }
  };

  const layout = (content) => (
    <div className="min-h-screen bg-background">
      <Sidebar />
      <div className="lg:pl-64 transition-all duration-300">
        <Header />
        <main className="main">{content}</main>
      </div>
    </div>
  );

  if (loading) {
    return layout(
      <div className="container py-5">
        <div className="text-center">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-3 text-muted">채용공고를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return layout(
      <div className="container py-4">
        <div className="alert alert-danger">
          <i className="bi bi-exclamation-triangle me-2"></i>
          {error}
          <Link to="/employer/jobs" className="btn btn-outline-danger btn-sm ms-3">
            목록으로
          </Link>
        </div>
      </div>
    );
  }

  if (!job) return null;

  const techStack = job.stack ? job.stack.split(',').map((s) => s.trim()).filter(Boolean) : [];
  const applicationCount = job.applicationCount ?? 0;
  const viewCount = job.viewCount ?? 0;
  const competitionRate = viewCount > 0 ? `약 1:${Math.max(1, Math.round(viewCount / Math.max(1, applicationCount)))}` : '약 1:1';

  return layout(
    <div className="container py-4">
      {/* 뒤로가기 */}
      <nav aria-label="breadcrumb" className="mb-4">
        <ol className="breadcrumb mb-0">
          <li className="breadcrumb-item">
            <Link to="/employer/dashboard">대시보드</Link>
          </li>
          <li className="breadcrumb-item">
            <Link to="/employer/jobs">채용공고</Link>
          </li>
          <li className="breadcrumb-item active" aria-current="page">
            {job.title}
          </li>
        </ol>
        <Link to="/employer/jobs" className="btn btn-link btn-sm text-muted p-0 mt-2 d-inline-flex align-items-center">
          <i className="bi bi-arrow-left me-1"></i>목록으로
        </Link>
      </nav>

      <div className="row">
        {/* 메인 컨텐츠 */}
        <div className="col-lg-8">
          {/* 공고 헤더 카드 */}
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

              {/* 공고 제목 + 상태 */}
              <div className="d-flex flex-wrap align-items-center gap-2 mb-3">
                <h2 className="mb-0">{job.title}</h2>
                {getStatusBadge(job.status)}
              </div>

              {/* 메타 정보: 조회수, 지원자, 등록일 */}
              <div className="d-flex flex-wrap gap-3 text-muted mb-4">
                <span>
                  <i className="bi bi-eye me-1"></i>
                  조회 {viewCount}
                </span>
                <span>
                  <i className="bi bi-people me-1"></i>
                  지원 {applicationCount}
                </span>
                <span>
                  <i className="bi bi-calendar me-1"></i>
                  {job.createdAt && new Date(job.createdAt).toLocaleDateString('ko-KR')} 등록
                </span>
              </div>

              {/* 기술 스택 */}
              {techStack.length > 0 && (
                <div className="mb-4">
                  <h6 className="fw-semibold mb-2">
                    <i className="bi bi-code-slash me-2"></i>기술 스택
                  </h6>
                  <div>
                    {techStack.map((tech, idx) => (
                      <span
                        key={idx}
                        className="badge bg-primary me-2 mb-2"
                        style={{ fontSize: '0.9rem', padding: '0.5em 0.8em' }}
                      >
                        {tech}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* 급여 및 경력 요약 */}
              <div className="d-flex flex-wrap gap-3 mb-0">
                {job.salaryText && (
                  <div className="alert alert-success mb-0 flex-grow-1">
                    <i className="bi bi-currency-dollar me-2"></i>
                    <strong>급여:</strong> {job.salaryText}
                  </div>
                )}
                <div className="alert alert-primary mb-0 flex-grow-1">
                  <i className="bi bi-briefcase me-2"></i>
                  <strong>경력:</strong>{' '}
                  {job.requiredExperience === null || job.requiredExperience === undefined || job.requiredExperience === 0
                    ? '신입/무관'
                    : `${job.requiredExperience}년 이상`}
                </div>
              </div>
            </div>
          </div>

          {/* 상세 내용 */}
          <div className="card shadow-sm mb-4">
            <div className="card-header bg-white">
              <h5 className="mb-0">
                <i className="bi bi-file-text me-2"></i>공고 내용
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
                style={{ whiteSpace: 'pre-wrap', lineHeight: '1.8', fontSize: '1rem' }}
              >
                {job.description || '등록된 상세 내용이 없습니다.'}
              </div>
            </div>
          </div>
        </div>

        {/* 사이드바 */}
        <div className="col-lg-4">
          {/* 채용 정보 카드 */}
          <div className="card shadow-sm mb-4">
            <div className="card-header bg-white">
              <h6 className="mb-0">
                <i className="bi bi-info-circle me-2"></i>채용 정보
              </h6>
            </div>
            <div className="card-body">
              <ul className="list-unstyled mb-0 small">
                {job.location && (
                  <li className="mb-2">
                    <span className="text-muted">근무지</span>
                    <div>{job.location}</div>
                  </li>
                )}
                <li className="mb-2">
                  <span className="text-muted">조회수</span>
                  <div>{viewCount}회</div>
                </li>
                <li className="mb-2">
                  <span className="text-muted">지원자</span>
                  <div>{applicationCount}명</div>
                </li>
                <li className="mb-0">
                  <span className="text-muted">경쟁률</span>
                  <div>{competitionRate}</div>
                </li>
              </ul>
            </div>
          </div>

          {/* 기업 전용 액션 카드 */}
          <div className="card shadow-sm mb-4 border-primary">
            <div className="card-header bg-white border-primary">
              <h6 className="mb-0 text-primary">
                <i className="bi bi-gear me-2"></i>공고 관리
              </h6>
            </div>
            <div className="card-body p-4">
              <div className="d-grid gap-2">
                <Link
                  to={`/employer/jobs/${jobId}/edit`}
                  className="btn btn-primary"
                >
                  <i className="bi bi-pencil me-2"></i>수정하기
                </Link>
                <Link
                  to="/employer/dashboard"
                  className="btn btn-outline-primary"
                >
                  <i className="bi bi-people me-2"></i>지원자 목록 보기
                </Link>
                <button
                  type="button"
                  className="btn btn-outline-danger"
                  onClick={handleDelete}
                  disabled={deleting}
                >
                  {deleting ? (
                    <span className="spinner-border spinner-border-sm me-2"></span>
                  ) : (
                    <i className="bi bi-trash me-2"></i>
                  )}
                  삭제하기
                </button>
                <Link to="/employer/jobs" className="btn btn-outline-secondary">
                  <i className="bi bi-list me-2"></i>목록으로
                </Link>
              </div>
            </div>
          </div>

          {/* 기업 정보 카드 */}
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
                <p className="mb-0 small text-muted">
                  <i className="bi bi-geo-alt me-2"></i>
                  {job.location}
                </p>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
