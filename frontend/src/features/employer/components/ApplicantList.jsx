import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { http } from '../../../api/http';

/**
 * 지원자 목록 컴포넌트
 * - 채용공고별 지원자 현황
 * - 면접 일정 잡기 버튼
 */
export default function ApplicantList({ onScheduleInterview }) {
  const [applicants, setApplicants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL'); // ALL, SUBMITTED, VIEWED, INTERVIEW

  useEffect(() => {
    const fetchApplicants = async () => {
      try {
        setLoading(true);
        const res = await http.get('/api/employer/applicants', {
          params: { status: filter !== 'ALL' ? filter : undefined }
        });
        setApplicants(res.data.applicants || []);
      } catch (err) {
        console.error('지원자 목록 로드 실패:', err);
        setApplicants([]);
      } finally {
        setLoading(false);
      }
    };

    fetchApplicants();
  }, [filter]);

  const statusLabels = {
    'SUBMITTED': { label: '지원완료', bg: 'primary' },
    'VIEWED': { label: '서류검토', bg: 'info' },
    'INTERVIEW': { label: '면접예정', bg: 'warning' },
    'HIRED': { label: '합격', bg: 'success' },
    'REJECTED': { label: '불합격', bg: 'danger' },
    'CANCELED': { label: '취소', bg: 'secondary' }
  };

  const handleStatusChange = async (applicationId, newStatus) => {
    try {
      await http.patch(`/api/employer/applicants/${applicationId}/status`, { status: newStatus });
      setApplicants(prev => 
        prev.map(a => a.applicationId === applicationId ? { ...a, status: newStatus } : a)
      );
    } catch (err) {
      alert(err.response?.data?.error || '상태 변경에 실패했습니다.');
    }
  };

  return (
    <div className="card shadow-sm mb-4">
      <div className="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 className="mb-0">
          <i className="bi bi-people me-2 text-success"></i>지원자 관리
        </h5>
        <div className="btn-group btn-group-sm">
          {['ALL', 'SUBMITTED', 'VIEWED', 'INTERVIEW'].map((f) => (
            <button
              key={f}
              className={`btn ${filter === f ? 'btn-primary' : 'btn-outline-secondary'}`}
              onClick={() => setFilter(f)}
            >
              {f === 'ALL' ? '전체' : statusLabels[f]?.label || f}
            </button>
          ))}
        </div>
      </div>
      
      <div className="card-body p-0">
        {loading ? (
          <div className="text-center py-4">
            <div className="spinner-border spinner-border-sm text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
          </div>
        ) : applicants.length === 0 ? (
          <div className="text-center py-5 text-muted">
            <i className="bi bi-inbox fs-1 d-block mb-2"></i>
            <p className="mb-0">
              {filter === 'ALL' ? '지원자가 없습니다.' : `${statusLabels[filter]?.label || filter} 상태의 지원자가 없습니다.`}
            </p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table table-hover mb-0">
              <thead className="table-light">
                <tr>
                  <th>지원자</th>
                  <th>채용공고</th>
                  <th className="text-center">상태</th>
                  <th className="text-center">지원일</th>
                  <th className="text-center">액션</th>
                </tr>
              </thead>
              <tbody>
                {applicants.map((applicant) => (
                  <tr key={applicant.applicationId}>
                    <td>
                      <div className="d-flex align-items-center">
                        <div 
                          className="rounded-circle bg-light d-flex align-items-center justify-content-center me-2"
                          style={{ width: 36, height: 36 }}
                        >
                          <i className="bi bi-person text-muted"></i>
                        </div>
                        <div>
                          <span className="fw-semibold">{applicant.name}</span>
                          <small className="text-muted d-block">{applicant.email}</small>
                        </div>
                      </div>
                    </td>
                    <td>
                      <Link 
                        to={`/employer/jobs/${applicant.jobId}`}
                        className="text-decoration-none"
                      >
                        <small className="text-truncate d-block" style={{ maxWidth: '200px' }}>
                          {applicant.jobTitle}
                        </small>
                      </Link>
                    </td>
                    <td className="text-center">
                      <span className={`badge bg-${statusLabels[applicant.status]?.bg || 'secondary'}`}>
                        {statusLabels[applicant.status]?.label || applicant.status}
                      </span>
                    </td>
                    <td className="text-center">
                      <small className="text-muted">
                        {applicant.appliedAt ? new Date(applicant.appliedAt).toLocaleDateString('ko-KR') : '-'}
                      </small>
                    </td>
                    <td className="text-center">
                      <div className="btn-group btn-group-sm">
                        {/* 이력서 보기 */}
                        <button 
                          className="btn btn-outline-secondary"
                          title="이력서 보기"
                          onClick={() => {
                            // 이력서 상세 페이지로 이동 또는 모달
                            window.open(`/resume/${applicant.resumeId}`, '_blank');
                          }}
                        >
                          <i className="bi bi-file-text"></i>
                        </button>
                        
                        {/* 면접 일정 잡기 */}
                        {['SUBMITTED', 'VIEWED'].includes(applicant.status) && (
                          <button 
                            className="btn btn-outline-primary"
                            title="면접 일정 잡기"
                            onClick={() => {
                              if (onScheduleInterview) {
                                onScheduleInterview(applicant);
                              }
                            }}
                          >
                            <i className="bi bi-calendar-plus"></i>
                          </button>
                        )}
                        
                        {/* 상태 변경 드롭다운 */}
                        <div className="btn-group btn-group-sm">
                          <button 
                            type="button" 
                            className="btn btn-outline-secondary dropdown-toggle"
                            data-bs-toggle="dropdown"
                            title="상태 변경"
                          >
                            <i className="bi bi-three-dots-vertical"></i>
                          </button>
                          <ul className="dropdown-menu dropdown-menu-end">
                            <li>
                              <button 
                                className="dropdown-item"
                                onClick={() => handleStatusChange(applicant.applicationId, 'VIEWED')}
                              >
                                <i className="bi bi-eye me-2 text-info"></i>서류검토로 변경
                              </button>
                            </li>
                            <li>
                              <button 
                                className="dropdown-item"
                                onClick={() => handleStatusChange(applicant.applicationId, 'INTERVIEW')}
                              >
                                <i className="bi bi-calendar-check me-2 text-warning"></i>면접예정으로 변경
                              </button>
                            </li>
                            <li><hr className="dropdown-divider" /></li>
                            <li>
                              <button 
                                className="dropdown-item text-success"
                                onClick={() => handleStatusChange(applicant.applicationId, 'HIRED')}
                              >
                                <i className="bi bi-check-circle me-2"></i>합격 처리
                              </button>
                            </li>
                            <li>
                              <button 
                                className="dropdown-item text-danger"
                                onClick={() => handleStatusChange(applicant.applicationId, 'REJECTED')}
                              >
                                <i className="bi bi-x-circle me-2"></i>불합격 처리
                              </button>
                            </li>
                          </ul>
                        </div>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      
      {/* 더보기 링크 */}
      {applicants.length > 0 && (
        <div className="card-footer bg-white text-center">
          <Link to="/employer/applicants" className="btn btn-sm btn-outline-primary">
            전체 지원자 보기 <i className="bi bi-arrow-right ms-1"></i>
          </Link>
        </div>
      )}
    </div>
  );
}
