import React from 'react';
import { Link } from 'react-router-dom';

/**
 * 기업 정보 카드 컴포넌트 (ERD 기준)
 */
export default function EmployerInfo({ employer, loading }) {
  if (loading) {
    return (
      <div className="card shadow-sm">
        <div className="card-body placeholder-glow">
          <div className="d-flex align-items-center mb-3">
            <div 
              className="placeholder rounded-circle me-3" 
              style={{ width: 60, height: 60 }}
            ></div>
            <div className="flex-grow-1">
              <span className="placeholder col-6 d-block mb-2"></span>
              <span className="placeholder col-4"></span>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!employer) {
    return (
      <div className="card shadow-sm border-warning">
        <div className="card-header bg-warning text-dark">
          <h5 className="mb-0">
            <i className="bi bi-exclamation-triangle me-2"></i>기업 정보 필요
          </h5>
        </div>
        <div className="card-body text-center py-4">
          <i className="bi bi-building fs-1 text-muted d-block mb-3"></i>
          <p className="text-muted mb-3">
            채용공고를 등록하려면 먼저<br/>기업 정보를 등록해주세요.
          </p>
          <Link to="/employer/profile" className="btn btn-warning">
            <i className="bi bi-plus-lg me-1"></i>기업 정보 등록
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="card shadow-sm">
      <div className="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 className="mb-0">
          <i className="bi bi-building me-2"></i>기업 정보
        </h5>
        <Link 
          to="/employer/profile" 
          className="btn btn-outline-secondary btn-sm"
        >
          <i className="bi bi-pencil"></i>
        </Link>
      </div>
      <div className="card-body">
        <div className="d-flex align-items-start mb-3">
          {employer.logoUrl ? (
            <img
              src={employer.logoUrl}
              alt={employer.name}
              className="rounded me-3"
              style={{ width: 60, height: 60, objectFit: 'cover' }}
            />
          ) : (
            <div
              className="d-flex align-items-center justify-content-center bg-light rounded me-3"
              style={{ width: 60, height: 60 }}
            >
              <i className="bi bi-building fs-4 text-muted"></i>
            </div>
          )}
          <div className="flex-grow-1">
            <h6 className="mb-1">{employer.name}</h6>
            <small className="text-muted">
              {employer.industry && (
                <span className="d-block">
                  <i className="bi bi-tag me-1"></i>{employer.industry}
                </span>
              )}
              {employer.location && (
                <span className="d-block">
                  <i className="bi bi-geo-alt me-1"></i>{employer.location}
                </span>
              )}
            </small>
          </div>
        </div>

        {employer.foundedYear && (
          <div className="d-flex justify-content-between small mb-2">
            <span className="text-muted">설립연도</span>
            <span>{employer.foundedYear}년</span>
          </div>
        )}

        {employer.employeeCount && (
          <div className="d-flex justify-content-between small mb-2">
            <span className="text-muted">직원수</span>
            <span>{employer.employeeCount}명</span>
          </div>
        )}

        {employer.roleInCompany && (
          <div className="d-flex justify-content-between small mb-2">
            <span className="text-muted">내 역할</span>
            <span className={`badge bg-${
              employer.roleInCompany === 'OWNER' ? 'primary' : 
              employer.roleInCompany === 'HR' ? 'success' : 'secondary'
            }`}>
              {employer.roleInCompany === 'OWNER' ? '대표' : 
               employer.roleInCompany === 'HR' ? '인사담당' : '직원'}
            </span>
          </div>
        )}

        {employer.status && (
          <div className="d-flex justify-content-between small mb-2">
            <span className="text-muted">상태</span>
            <span className={`badge bg-${
              employer.status === 'ACTIVE' ? 'success' : 'secondary'
            }`}>
              {employer.status === 'ACTIVE' ? '활성' : employer.status}
            </span>
          </div>
        )}

        {employer.description && (
          <p className="text-muted small mb-0 mt-3" style={{ 
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
            overflow: 'hidden'
          }}>
            {employer.description}
          </p>
        )}
      </div>
    </div>
  );
}
