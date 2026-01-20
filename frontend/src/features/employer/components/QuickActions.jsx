import React from 'react';
import { Link } from 'react-router-dom';

/**
 * 빠른 작업 버튼 컴포넌트
 */
export default function QuickActions({ hasEmployer = true }) {
  const actions = hasEmployer ? [
    {
      label: '채용공고 등록',
      icon: 'bi-plus-circle',
      to: '/employer/jobs/create',
      color: 'primary',
    },
    {
      label: '채용공고 관리',
      icon: 'bi-briefcase',
      to: '/employer/jobs',
      color: 'success',
    },
    {
      label: '기업정보 수정',
      icon: 'bi-building',
      to: '/employer/profile',
      color: 'info',
    },
    {
      label: '대시보드',
      icon: 'bi-speedometer2',
      to: '/employer/dashboard',
      color: 'secondary',
    },
  ] : [
    {
      label: '기업정보 등록',
      icon: 'bi-building',
      to: '/employer/profile',
      color: 'warning',
    },
    {
      label: '대시보드',
      icon: 'bi-speedometer2',
      to: '/employer/dashboard',
      color: 'secondary',
    },
  ];

  return (
    <div className="card shadow-sm">
      <div className="card-header bg-white">
        <h5 className="mb-0">
          <i className="bi bi-lightning-charge me-2"></i>빠른 작업
        </h5>
      </div>
      <div className="card-body">
        <div className="row g-3">
          {actions.map((action, index) => (
            <div key={index} className={hasEmployer ? "col-6" : "col-12"}>
              <Link
                to={action.to}
                className={`btn btn-outline-${action.color} w-100 py-3 d-flex flex-column align-items-center`}
              >
                <i className={`bi ${action.icon} fs-3 mb-2`}></i>
                <span className="small">{action.label}</span>
              </Link>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
