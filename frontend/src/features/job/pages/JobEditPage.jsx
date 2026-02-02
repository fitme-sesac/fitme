import React, { useEffect, useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { http } from '../../../api/http';
import { Sidebar } from '@/components/layout/Sidebar';
import { Header } from '@/components/layout/Header';
import JobForm from '../components/JobForm';

/**
 * 채용공고 수정 페이지
 */
export default function JobEditPage() {
  const { jobId } = useParams();
  const navigate = useNavigate();
  
  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

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

  const handleSubmit = async (formData) => {
    try {
      setSaving(true);
      setError(null);
      
      await http.put(`/api/jobs/${jobId}`, formData);
      
      alert('수정되었습니다.');
      navigate(`/employer/jobs/${jobId}`);
    } catch (err) {
      if (err.response?.status === 401) {
        navigate('/Login');
        return;
      }
      setError(err.response?.data?.error || '수정에 실패했습니다.');
      throw err;
    } finally {
      setSaving(false);
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
        </div>
      </div>
    );
  }

  if (error && !job) {
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

  return layout(
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
              <li className="breadcrumb-item active">수정</li>
            </ol>
          </nav>
          <h2 className="mb-0">
            <i className="bi bi-pencil me-2"></i>채용공고 수정
          </h2>
        </div>

        {/* 에러 메시지 */}
        {error && (
          <div className="alert alert-danger alert-dismissible fade show">
            <i className="bi bi-exclamation-triangle me-2"></i>
            {error}
            <button 
              type="button" 
              className="btn-close" 
              onClick={() => setError(null)}
            ></button>
          </div>
        )}

        <div className="row">
          <div className="col-lg-8">
            <JobForm job={job} onSubmit={handleSubmit} loading={saving} />
          </div>
          
          <div className="col-lg-4">
            <div className="card shadow-sm">
              <div className="card-header bg-white">
                <h5 className="mb-0">
                  <i className="bi bi-info-circle me-2"></i>안내
                </h5>
              </div>
              <div className="card-body">
                <p className="text-muted small mb-3">
                  채용공고 수정 시 주의사항:
                </p>
                <ul className="list-unstyled text-muted small mb-0">
                  <li className="mb-2">
                    <i className="bi bi-exclamation-circle text-warning me-1"></i>
                    이미 지원한 지원자가 있을 수 있습니다.
                  </li>
                  <li className="mb-2">
                    <i className="bi bi-exclamation-circle text-warning me-1"></i>
                    급여, 근무지 등 핵심 정보 변경은 신중하게 해주세요.
                  </li>
                  <li>
                    <i className="bi bi-info-circle text-info me-1"></i>
                    '마감' 상태로 변경하면 더 이상 지원을 받지 않습니다.
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
  );
}
