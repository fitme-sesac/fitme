import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { http } from '../../../api/http';
import { Sidebar } from '@/components/layout/Sidebar';
import { Header } from '@/components/layout/Header';

/**
 * 채용공고 목록 페이지
 * ERD 기준 필드 사용
 */
export default function JobListPage() {
  const navigate = useNavigate();
  
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pagination, setPagination] = useState({
    page: 0,
    totalPages: 0,
    totalElements: 0,
  });

  useEffect(() => {
    fetchJobs(0);
  }, []);

  const fetchJobs = async (page) => {
    try {
      setLoading(true);
      const res = await http.get('/api/jobs', { params: { page, size: 10 } });
      setJobs(res.data.jobs || []);
      setPagination({
        page: res.data.page || 0,
        totalPages: res.data.totalPages || 0,
        totalElements: res.data.totalElements || 0,
      });
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

  const handlePageChange = (newPage) => {
    fetchJobs(newPage);
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

  if (loading && jobs.length === 0) {
    return (
      <div className="min-h-screen bg-background">
        <Sidebar />
        <div className="lg:pl-64 transition-all duration-300">
          <Header />
          <main className="main">
            <div className="container py-5">
              <div className="text-center">
                <div className="spinner-border text-primary" role="status">
                  <span className="visually-hidden">Loading...</span>
                </div>
              </div>
            </div>
          </main>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Sidebar />
      <div className="lg:pl-64 transition-all duration-300">
        <Header />
        <main className="main">
      <div className="container py-4">
        {/* 페이지 헤더 */}
        <div className="d-flex justify-content-between align-items-center mb-4">
          <div>
            <nav aria-label="breadcrumb">
              <ol className="breadcrumb mb-1">
                <li className="breadcrumb-item">
                  <Link to="/employer/dashboard">대시보드</Link>
                </li>
                <li className="breadcrumb-item active">채용공고 관리</li>
              </ol>
            </nav>
            <h2 className="mb-0">
              <i className="bi bi-briefcase me-2"></i>채용공고 관리
            </h2>
          </div>
          <Link to="/employer/jobs/create" className="btn btn-primary">
            <i className="bi bi-plus-lg me-1"></i>새 공고 등록
          </Link>
        </div>

        {/* 에러 메시지 */}
        {error && (
          <div className="alert alert-danger">
            <i className="bi bi-exclamation-triangle me-2"></i>
            {error}
          </div>
        )}

        {/* 채용공고 목록 */}
        <div className="card shadow-sm">
          <div className="card-header bg-white d-flex justify-content-between align-items-center">
            <span>
              총 <strong>{pagination.totalElements}</strong>개의 채용공고
            </span>
          </div>
          <div className="card-body p-0">
            {jobs.length === 0 ? (
              <div className="text-center py-5 text-muted">
                <i className="bi bi-file-earmark-text fs-1 d-block mb-3"></i>
                <p className="mb-3">등록된 채용공고가 없습니다.</p>
                <Link to="/employer/jobs/create" className="btn btn-primary">
                  첫 채용공고 등록하기
                </Link>
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table table-hover mb-0">
                  <thead className="table-light">
                    <tr>
                      <th>공고 제목</th>
                      <th className="text-center" style={{ width: 100 }}>상태</th>
                      <th className="text-center" style={{ width: 80 }}>조회</th>
                      <th className="text-center" style={{ width: 80 }}>지원</th>
                      <th className="text-center" style={{ width: 120 }}>등록일</th>
                      <th className="text-center" style={{ width: 100 }}>관리</th>
                    </tr>
                  </thead>
                  <tbody>
                    {jobs.map((job) => {
                      const detailPath = `/employer/jobs/${job.jobId || job.jobUid}`;
                      return (
                      <tr
                        key={job.jobId || job.jobUid}
                        role="button"
                        tabIndex={0}
                        className="table-row-clickable"
                        onClick={() => navigate(detailPath)}
                        onKeyDown={(e) => e.key === 'Enter' && navigate(detailPath)}
                        style={{ cursor: 'pointer' }}
                      >
                        <td>
                          <span className="text-dark fw-semibold">
                            {job.title}
                          </span>
                          {job.location && (
                            <small className="text-muted d-block">
                              <i className="bi bi-geo-alt me-1"></i>
                              {job.location}
                            </small>
                          )}
                        </td>
                        <td className="text-center">{getStatusBadge(job.status)}</td>
                        <td className="text-center">{job.viewCount || 0}</td>
                        <td className="text-center">{job.applicationCount || 0}</td>
                        <td className="text-center small">
                          {job.createdAt ? new Date(job.createdAt).toLocaleDateString('ko-KR') : '-'}
                        </td>
                        <td className="text-center" onClick={(e) => e.stopPropagation()}>
                          <div className="btn-group btn-group-sm">
                            <Link
                              to={detailPath}
                              className="btn btn-outline-secondary"
                              title="상세보기"
                            >
                              <i className="bi bi-eye"></i>
                            </Link>
                            <Link
                              to={`${detailPath}/edit`}
                              className="btn btn-outline-primary"
                              title="수정"
                            >
                              <i className="bi bi-pencil"></i>
                            </Link>
                          </div>
                        </td>
                      </tr>
                    );})}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* 페이지네이션 */}
          {pagination.totalPages > 1 && (
            <div className="card-footer bg-white">
              <nav>
                <ul className="pagination pagination-sm justify-content-center mb-0">
                  <li className={`page-item ${pagination.page === 0 ? 'disabled' : ''}`}>
                    <button 
                      className="page-link" 
                      onClick={() => handlePageChange(pagination.page - 1)}
                      disabled={pagination.page === 0}
                    >
                      이전
                    </button>
                  </li>
                  {[...Array(pagination.totalPages)].map((_, i) => (
                    <li 
                      key={i} 
                      className={`page-item ${pagination.page === i ? 'active' : ''}`}
                    >
                      <button 
                        className="page-link" 
                        onClick={() => handlePageChange(i)}
                      >
                        {i + 1}
                      </button>
                    </li>
                  ))}
                  <li className={`page-item ${pagination.page === pagination.totalPages - 1 ? 'disabled' : ''}`}>
                    <button 
                      className="page-link" 
                      onClick={() => handlePageChange(pagination.page + 1)}
                      disabled={pagination.page === pagination.totalPages - 1}
                    >
                      다음
                    </button>
                  </li>
                </ul>
              </nav>
            </div>
          )}
        </div>
        </div>
      </main>
      </div>
    </div>
  );
}
