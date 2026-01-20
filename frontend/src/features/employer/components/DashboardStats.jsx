import React from 'react';

/**
 * 대시보드 통계 카드 컴포넌트
 */
export default function DashboardStats({ stats, loading }) {
  if (loading) {
    return (
      <div className="row g-4 mb-4">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="col-md-6 col-lg-3">
            <div className="card h-100 placeholder-glow">
              <div className="card-body">
                <span className="placeholder col-6"></span>
                <h3 className="placeholder col-4"></h3>
              </div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  const statItems = [
    {
      label: '진행중 공고',
      value: stats.activeJobPostings || 0,
      icon: 'bi-briefcase',
      color: 'success',
      bgColor: '#d1e7dd',
    },
    {
      label: '총 지원자',
      value: stats.totalApplications || 0,
      icon: 'bi-people',
      color: 'primary',
      bgColor: '#e7f1ff',
    },
    {
      label: '총 조회수',
      value: stats.totalViewCount || 0,
      icon: 'bi-eye',
      color: 'info',
      bgColor: '#cff4fc',
    },
    {
      label: '전체 공고',
      value: stats.totalJobPostings || 0,
      icon: 'bi-file-earmark-text',
      color: 'secondary',
      bgColor: '#e2e3e5',
    },
  ];

  return (
    <div className="row g-4 mb-4">
      {statItems.map((item, index) => (
        <div key={index} className="col-md-6 col-lg-3">
          <div 
            className="card h-100 border-0 shadow-sm"
            style={{ backgroundColor: item.bgColor }}
          >
            <div className="card-body">
              <div className="d-flex align-items-center mb-2">
                <i className={`bi ${item.icon} text-${item.color} fs-4 me-2`}></i>
                <span className="text-muted small">{item.label}</span>
              </div>
              <h3 className={`mb-0 text-${item.color}`}>
                {typeof item.value === 'number' ? item.value.toLocaleString() : item.value}
                {item.suffix && <small className="fs-6">{item.suffix}</small>}
              </h3>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
