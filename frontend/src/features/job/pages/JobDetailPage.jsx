import React, { useEffect, useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { http } from '../../../api/http';

/**
 * 채용공고 상세 페이지
 * ERD 기준 필드 사용
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
        return <span className="badge bg-success fs-6">모집중</span>;
      case 'DRAFT':
        return <span className="badge bg-secondary fs-6">임시저장</span>;
      case 'CLOSED':
        return <span className="badge bg-danger fs-6">마감</span>;
      default:
        return <span className="badge bg-light text-dark fs-6">{status}</span>;
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
          </div>
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main className="main">
        <div className="container py-4">
          <div className="alert alert-danger">
            <i className="bi bi-exclamation-triangle me-2"></i>
            {error}
            <Link to="/employer/jobs" className="btn btn-outline-danger btn-sm ms-3">
              목록으로
            </Link>
          </div>
        </div>
      </main>
    );
  }

  if (!job) return null;

  return (
    <main className="main">
      <div className="container py-4">
        {/* 헤더 */}
        <div className="mb-4">
          <nav aria-label="breadcrumb">
            <ol className="breadcrumb mb-1">
              <li className="breadcrumb-item">
                <Link to="/employer/dashboard">대시보드</Link>
              </li>
              <li className="breadcrumb-item">
                <Link to="/employer/jobs">채용공고</Link>
              </li>
              <li className="breadcrumb-item active">상세</li>
            </ol>
          </nav>
          
          <div className="d-flex justify-content-between align-items-start">
            <div>
              <h2 className="mb-2">{job.title}</h2>
              <div className="d-flex align-items-center gap-3">
                {getStatusBadge(job.status)}
                <span className="text-muted">
                  <i className="bi bi-calendar me-1"></i>
                  {job.createdAt ? new Date(job.createdAt).toLocaleDateString('ko-KR') : '-'}
                </span>
              </div>
            </div>
            <div className="d-flex gap-2">
              <Link 
                to={`/employer/jobs/${jobId}/edit`}
                className="btn btn-primary"
              >
                <i className="bi bi-pencil me-1"></i>수정
              </Link>
              <button
                className="btn btn-outline-danger"
                onClick={handleDelete}
                disabled={deleting}
              >
                {deleting ? (
                  <span className="spinner-border spinner-border-sm me-1"></span>
                ) : (
                  <i className="bi bi-trash me-1"></i>
                )}
                삭제
              </button>
            </div>
          </div>
        </div>

        <div className="row g-4">
          {/* 메인 콘텐츠 */}
          <div className="col-lg-8">
            {/* 공고 내용 */}
            <div className="card shadow-sm mb-4">
              <div className="card-header bg-white">
                <h5 className="mb-0">
                  <i className="bi bi-file-text me-2"></i>공고 내용
                </h5>
              </div>
              <div className="card-body">
                <div style={{ whiteSpace: 'pre-wrap' }}>
                  {job.description}
                </div>
              </div>
            </div>
          </div>

          {/* 사이드바 */}
          <div className="col-lg-4">
            {/* 통계 */}
            <div className="card shadow-sm mb-4">
              <div className="card-header bg-white">
                <h5 className="mb-0">
                  <i className="bi bi-bar-chart me-2"></i>통계
                </h5>
              </div>
              <div className="card-body">
                <div className="row text-center">
                  <div className="col-6">
                    <div className="fs-3 fw-bold text-primary">{job.viewCount || 0}</div>
                    <small className="text-muted">조회수</small>
                  </div>
                  <div className="col-6">
                    <div className="fs-3 fw-bold text-success">{job.applicationCount || 0}</div>
                    <small className="text-muted">지원자</small>
                  </div>
                </div>
              </div>
            </div>

            {/* 근무 조건 */}
            <div className="card shadow-sm mb-4">
              <div className="card-header bg-white">
                <h5 className="mb-0">
                  <i className="bi bi-info-circle me-2"></i>근무 조건
                </h5>
              </div>
              <div className="card-body">
                {job.location && (
                  <div className="mb-3">
                    <label className="text-muted small">근무지</label>
                    <div>
                      <i className="bi bi-geo-alt me-1"></i>
                      {job.location}
                    </div>
                  </div>
                )}

                {job.salaryText && (
                  <div className="mb-3">
                    <label className="text-muted small">급여</label>
                    <div>
                      <i className="bi bi-currency-dollar me-1"></i>
                      {job.salaryText}
                    </div>
                  </div>
                )}

                {job.stack && (
                  <div className="mb-0">
                    <label className="text-muted small">기술 스택</label>
                    <div>
                      <i className="bi bi-code-slash me-1"></i>
                      {job.stack}
                    </div>
                  </div>
                )}

                {!job.location && !job.salaryText && !job.stack && (
                  <p className="text-muted mb-0">등록된 근무 조건이 없습니다.</p>
                )}
              </div>
            </div>

            {/* 빠른 작업 */}
            <div className="card shadow-sm">
              <div className="card-body">
                <div className="d-grid gap-2">
                  <Link 
                    to="/employer/jobs/create" 
                    className="btn btn-outline-primary"
                  >
                    <i className="bi bi-plus-lg me-1"></i>새 공고 등록
                  </Link>
                  <Link 
                    to="/employer/jobs" 
                    className="btn btn-outline-secondary"
                  >
                    <i className="bi bi-list me-1"></i>목록으로
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
