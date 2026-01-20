import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { http } from '../../../api/http';
import DashboardStats from '../components/DashboardStats';
import EmployerInfo from '../components/EmployerInfo';
import QuickActions from '../components/QuickActions';

/**
 * 기업 대시보드 메인 페이지
 * - 기업 회원이 로그인하면 해당 회사에 맞는 대시보드가 표시됩니다.
 * - 백엔드 API: GET /api/employer/dashboard
 */
export default function EmployerDashboardPage() {
  const navigate = useNavigate();
  
  // 상태 관리
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 대시보드 로드
  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        setLoading(true);
        const res = await http.get('/api/employer/dashboard');
        setDashboard(res.data);
      } catch (err) {
        if (err.response?.status === 401) {
          navigate('/Login');
          return;
        }
        if (err.response?.status === 403) {
          setError('기업 회원만 접근할 수 있습니다.');
          return;
        }
        setError(err.response?.data?.error || '대시보드를 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchDashboard();
  }, [navigate]);

  // 로딩 상태
  if (loading) {
    return (
      <main className="main">
        <div className="container py-5">
          <div className="text-center">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <p className="mt-3 text-muted">대시보드를 불러오는 중...</p>
          </div>
        </div>
      </main>
    );
  }

  // 에러 상태
  if (error) {
    return (
      <main className="main">
        <div className="container py-5">
          <div className="alert alert-warning">
            <h4 className="alert-heading">
              <i className="bi bi-exclamation-triangle me-2"></i>알림
            </h4>
            <p>{error}</p>
            <hr />
            <div className="d-flex gap-2">
              <Link to="/Login" className="btn btn-primary">로그인</Link>
              <Link to="/" className="btn btn-outline-secondary">홈으로</Link>
            </div>
          </div>
        </div>
      </main>
    );
  }

  // 기업 프로필 데이터 추출 (ERD 기준 필드명)
  const profile = dashboard?.profile;
  
  // 통계 데이터 매핑
  const stats = {
    totalJobPostings: dashboard?.recentJobs?.length || 0,
    activeJobPostings: dashboard?.activeJobCount || 0,
    totalApplications: dashboard?.totalApplicationCount || 0,
    totalViewCount: dashboard?.totalViewCount || 0,
  };

  // 최근 채용공고
  const recentJobs = dashboard?.recentJobs || [];

  // employer 정보 매핑 (EmployerInfo 컴포넌트용) - ERD 기준
  const employer = profile ? {
    employerId: profile.employerId,
    employerUid: profile.employerUid,
    name: profile.name,
    logoUrl: profile.logoUrl,
    industry: profile.industry,
    foundedYear: profile.foundedYear,
    employeeCount: profile.employeeCount,
    location: profile.location,
    description: profile.description,
    culture: profile.culture,
    benefits: profile.benefits,
    techStack: profile.techStack,
    contactEmail: profile.contactEmail,
    contactPhone: profile.contactPhone,
    websiteUrl: profile.websiteUrl,
    status: profile.status,
    roleInCompany: profile.roleInCompany,
  } : null;

  return (
    <main className="main">
      <div className="container py-4">
        {/* 페이지 헤더 */}
        <div className="d-flex justify-content-between align-items-center mb-4">
          <div>
            <h2 className="mb-1">
              <i className="bi bi-speedometer2 me-2"></i>
              기업 대시보드
            </h2>
            <p className="text-muted mb-0">
              {profile?.name || '회사'}의 채용 현황을 한눈에 확인하세요.
            </p>
          </div>
          {profile && (
            <Link to="/employer/jobs/create" className="btn btn-primary">
              <i className="bi bi-plus-lg me-1"></i>채용공고 등록
            </Link>
          )}
        </div>

        {/* 통계 카드 */}
        <DashboardStats stats={stats} loading={false} />

        <div className="row g-4">
          {/* 왼쪽 영역 */}
          <div className="col-lg-8">
            {/* 최근 채용공고 */}
            <div className="card shadow-sm mb-4">
              <div className="card-header bg-white d-flex justify-content-between align-items-center">
                <h5 className="mb-0">
                  <i className="bi bi-briefcase me-2"></i>최근 채용공고
                </h5>
                <Link to="/employer/jobs" className="btn btn-sm btn-outline-primary">
                  전체보기
                </Link>
              </div>
              <div className="card-body">
                {!profile ? (
                  <div className="text-center py-4 text-muted">
                    <i className="bi bi-building fs-1 d-block mb-2"></i>
                    <p className="mb-3">먼저 기업 정보를 등록해주세요.</p>
                    <Link to="/employer/profile" className="btn btn-warning btn-sm">
                      기업 정보 등록하기
                    </Link>
                  </div>
                ) : recentJobs.length === 0 ? (
                  <div className="text-center py-4 text-muted">
                    <i className="bi bi-file-earmark-text fs-1 d-block mb-2"></i>
                    <p className="mb-3">등록된 채용공고가 없습니다.</p>
                    <Link to="/employer/jobs/create" className="btn btn-primary btn-sm">
                      첫 채용공고 등록하기
                    </Link>
                  </div>
                ) : (
                  <div className="list-group list-group-flush">
                    {recentJobs.map((job) => (
                      <Link
                        key={job.jobUid}
                        to={`/employer/jobs/${job.jobUid}`}
                        className="list-group-item list-group-item-action"
                      >
                        <div className="d-flex justify-content-between align-items-start">
                          <div>
                            <h6 className="mb-1">{job.title}</h6>
                            <small className="text-muted">
                              <span className="me-3">
                                <i className="bi bi-eye me-1"></i>{job.viewCount || 0}
                              </span>
                              <span>
                                <i className="bi bi-person-check me-1"></i>{job.applicationCount || 0}
                              </span>
                            </small>
                          </div>
                          <span className={`badge bg-${
                            job.status === 'OPEN' || job.status === 'ACTIVE' ? 'success' : 
                            job.status === 'DRAFT' ? 'secondary' : 'danger'
                          }`}>
                            {job.status === 'OPEN' || job.status === 'ACTIVE' ? '모집중' : 
                             job.status === 'DRAFT' ? '임시저장' : '마감'}
                          </span>
                        </div>
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* 요약 통계 카드 */}
            <div className="row g-3">
              <div className="col-md-4">
                <div className="card bg-primary text-white">
                  <div className="card-body text-center">
                    <i className="bi bi-briefcase fs-2 mb-2"></i>
                    <h3 className="mb-0">{stats.activeJobPostings}</h3>
                    <small>진행중 공고</small>
                  </div>
                </div>
              </div>
              <div className="col-md-4">
                <div className="card bg-success text-white">
                  <div className="card-body text-center">
                    <i className="bi bi-people fs-2 mb-2"></i>
                    <h3 className="mb-0">{stats.totalApplications}</h3>
                    <small>총 지원자</small>
                  </div>
                </div>
              </div>
              <div className="col-md-4">
                <div className="card bg-info text-white">
                  <div className="card-body text-center">
                    <i className="bi bi-eye fs-2 mb-2"></i>
                    <h3 className="mb-0">{stats.totalViewCount}</h3>
                    <small>총 조회수</small>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* 오른쪽 사이드바 */}
          <div className="col-lg-4">
            {/* 기업 정보 */}
            <div className="mb-4">
              <EmployerInfo employer={employer} loading={false} />
            </div>

            {/* 빠른 작업 */}
            <QuickActions hasEmployer={!!profile} />
          </div>
        </div>
      </div>
    </main>
  );
}
