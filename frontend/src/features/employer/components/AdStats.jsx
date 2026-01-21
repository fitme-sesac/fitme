import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { http } from '../../../api/http';

/**
 * 광고 통계 섹션 컴포넌트
 * - 광고 캠페인 현황
 * - 클릭 수, 노출 수, 비용 등
 */
export default function AdStats({ employerId }) {
  const [adStats, setAdStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAdStats = async () => {
      try {
        setLoading(true);
        // 광고 통계 API 호출 (없으면 기본값 사용)
        const res = await http.get('/api/employer/ad-stats');
        setAdStats(res.data);
      } catch (err) {
        // API가 없거나 에러시 기본값
        setAdStats({
          activeCampaigns: 0,
          totalClicks: 0,
          totalImpressions: 0,
          totalSpent: 0,
          ctr: 0,
          campaigns: []
        });
      } finally {
        setLoading(false);
      }
    };

    fetchAdStats();
  }, [employerId]);

  if (loading) {
    return (
      <div className="card shadow-sm mb-4">
        <div className="card-body text-center py-4">
          <div className="spinner-border spinner-border-sm text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="card shadow-sm mb-4">
      <div className="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 className="mb-0">
          <i className="bi bi-megaphone me-2 text-warning"></i>광고 현황
        </h5>
        <Link to="/employer/ads" className="btn btn-sm btn-outline-warning">
          <i className="bi bi-plus-lg me-1"></i>광고 관리
        </Link>
      </div>
      <div className="card-body">
        {/* 광고 요약 통계 */}
        <div className="row g-3 mb-4">
          <div className="col-6 col-md-3">
            <div className="border rounded p-3 text-center bg-light">
              <i className="bi bi-play-circle text-success fs-4"></i>
              <h4 className="mb-0 mt-2">{adStats?.activeCampaigns || 0}</h4>
              <small className="text-muted">진행중 캠페인</small>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="border rounded p-3 text-center bg-light">
              <i className="bi bi-cursor text-primary fs-4"></i>
              <h4 className="mb-0 mt-2">{(adStats?.totalClicks || 0).toLocaleString()}</h4>
              <small className="text-muted">총 클릭 수</small>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="border rounded p-3 text-center bg-light">
              <i className="bi bi-eye text-info fs-4"></i>
              <h4 className="mb-0 mt-2">{(adStats?.totalImpressions || 0).toLocaleString()}</h4>
              <small className="text-muted">총 노출 수</small>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="border rounded p-3 text-center bg-light">
              <i className="bi bi-coin text-warning fs-4"></i>
              <h4 className="mb-0 mt-2">{(adStats?.totalSpent || 0).toLocaleString()}</h4>
              <small className="text-muted">사용 크레딧</small>
            </div>
          </div>
        </div>

        {/* 활성 캠페인 목록 */}
        {adStats?.campaigns && adStats.campaigns.length > 0 ? (
          <div className="table-responsive">
            <table className="table table-sm table-hover mb-0">
              <thead className="table-light">
                <tr>
                  <th>캠페인</th>
                  <th className="text-center">상태</th>
                  <th className="text-end">클릭</th>
                  <th className="text-end">CTR</th>
                  <th className="text-end">일 예산</th>
                </tr>
              </thead>
              <tbody>
                {adStats.campaigns.slice(0, 3).map((campaign) => (
                  <tr key={campaign.campaignId}>
                    <td>
                      <small className="fw-semibold">{campaign.jobTitle}</small>
                    </td>
                    <td className="text-center">
                      <span className={`badge bg-${
                        campaign.status === 'ACTIVE' ? 'success' : 
                        campaign.status === 'PAUSED' ? 'warning' : 'secondary'
                      }`}>
                        {campaign.status === 'ACTIVE' ? '진행중' : 
                         campaign.status === 'PAUSED' ? '일시정지' : '종료'}
                      </span>
                    </td>
                    <td className="text-end">{campaign.clicks || 0}</td>
                    <td className="text-end">{campaign.ctr ? `${campaign.ctr.toFixed(1)}%` : '0%'}</td>
                    <td className="text-end">{campaign.dailyBudget?.toLocaleString() || 0}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="text-center py-3 text-muted">
            <i className="bi bi-megaphone fs-2 d-block mb-2"></i>
            <p className="mb-2">진행중인 광고 캠페인이 없습니다.</p>
            <Link to="/employer/ads/create" className="btn btn-warning btn-sm">
              <i className="bi bi-plus-lg me-1"></i>광고 시작하기
            </Link>
          </div>
        )}

        {/* CTR 정보 */}
        {adStats?.totalImpressions > 0 && (
          <div className="mt-3 pt-3 border-top">
            <div className="d-flex justify-content-between align-items-center">
              <span className="text-muted small">평균 클릭률 (CTR)</span>
              <span className="fw-bold text-primary">
                {((adStats.totalClicks / adStats.totalImpressions) * 100).toFixed(2)}%
              </span>
            </div>
            <div className="progress mt-2" style={{ height: '8px' }}>
              <div 
                className="progress-bar bg-primary" 
                style={{ width: `${Math.min((adStats.totalClicks / adStats.totalImpressions) * 100, 100)}%` }}
              ></div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
