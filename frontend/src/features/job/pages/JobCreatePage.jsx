import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { http } from '../../../api/http';
import { Sidebar } from '@/components/layout/Sidebar';
import { Header } from '@/components/layout/Header';
import JobForm from '../components/JobForm';

/**
 * 채용공고 등록 페이지
 */
export default function JobCreatePage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (formData) => {
    try {
      setLoading(true);
      setError(null);
      
      const res = await http.post('/api/jobs', formData);
      
      alert('채용공고가 등록되었습니다.');
      navigate(`/employer/jobs/${res.data.jobId || res.data.jobUid}`);
    } catch (err) {
      if (err.response?.status === 401) {
        navigate('/Login');
        return;
      }
      setError(err.response?.data?.error || '등록에 실패했습니다.');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <Sidebar />
      <div className="lg:pl-64 transition-all duration-300">
        <Header />
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
              <li className="breadcrumb-item active">등록</li>
            </ol>
          </nav>
          <h2 className="mb-0">
            <i className="bi bi-plus-circle me-2"></i>채용공고 등록
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
            <JobForm onSubmit={handleSubmit} loading={loading} />
          </div>
          
          <div className="col-lg-4">
            <div className="card shadow-sm">
              <div className="card-header bg-white">
                <h5 className="mb-0">
                  <i className="bi bi-lightbulb me-2"></i>작성 가이드
                </h5>
              </div>
              <div className="card-body">
                <ul className="list-unstyled text-muted small mb-0">
                  <li className="mb-2">
                    <i className="bi bi-check text-success me-1"></i>
                    명확한 직무명과 업무 내용을 작성하세요.
                  </li>
                  <li className="mb-2">
                    <i className="bi bi-check text-success me-1"></i>
                    급여 정보를 명시하면 지원율이 높아집니다.
                  </li>
                  <li className="mb-2">
                    <i className="bi bi-check text-success me-1"></i>
                    기술 스택을 입력하면 맞춤 인재를 찾을 수 있습니다.
                  </li>
                  <li>
                    <i className="bi bi-check text-success me-1"></i>
                    '임시저장'으로 먼저 저장한 후 수정할 수 있습니다.
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
      </main>
      </div>
    </div>
  );
}
