import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { http } from '../../../api/http';

/**
 * 기업 프로필 등록/수정 페이지
 * 백엔드 API: GET/POST /api/employer/profile
 * ERD 기준 필드명 사용
 */
export default function EmployerProfilePage() {
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [isNew, setIsNew] = useState(true);
  
  // ERD 기준 필드명
  const [formData, setFormData] = useState({
    name: '',
    logoUrl: '',
    industry: '',
    foundedYear: '',
    employeeCount: '',
    location: '',
    description: '',
    culture: '',
    benefits: '',
    techStack: '',
    contactEmail: '',
    contactPhone: '',
    websiteUrl: '',
  });

  const [errors, setErrors] = useState({});

  // 기존 프로필 로드
  useEffect(() => {
    const fetchProfile = async () => {
      try {
        setLoading(true);
        const res = await http.get('/api/employer/profile');
        
        if (res.data) {
          setFormData({
            name: res.data.name || '',
            logoUrl: res.data.logoUrl || '',
            industry: res.data.industry || '',
            foundedYear: res.data.foundedYear || '',
            employeeCount: res.data.employeeCount || '',
            location: res.data.location || '',
            description: res.data.description || '',
            culture: res.data.culture || '',
            benefits: res.data.benefits || '',
            techStack: res.data.techStack || '',
            contactEmail: res.data.contactEmail || '',
            contactPhone: res.data.contactPhone || '',
            websiteUrl: res.data.websiteUrl || '',
          });
          setIsNew(false);
        }
      } catch (err) {
        if (err.response?.status === 401) {
          navigate('/Login');
          return;
        }
        // 프로필이 없는 경우 새로 등록
        setIsNew(true);
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [navigate]);

  const validate = () => {
    const newErrors = {};
    
    if (!formData.name.trim()) {
      newErrors.name = '회사명을 입력해주세요.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value, type } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'number' ? (value === '' ? '' : Number(value)) : value,
    }));
    
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validate()) return;

    try {
      setSubmitting(true);
      setError(null);
      
      const submitData = {
        ...formData,
        foundedYear: formData.foundedYear || null,
        employeeCount: formData.employeeCount || null,
      };
      
      await http.post('/api/employer/profile', submitData);
      
      alert(isNew ? '기업 정보가 등록되었습니다.' : '기업 정보가 수정되었습니다.');
      navigate('/employer/dashboard');
    } catch (err) {
      if (err.response?.status === 401) {
        navigate('/Login');
        return;
      }
      setError(err.response?.data?.error || '저장에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const industries = [
    'IT/소프트웨어',
    '금융',
    '제조업',
    '유통/물류',
    '의료/제약',
    '교육',
    '미디어/엔터테인먼트',
    '건설/부동산',
    '서비스업',
    '기타',
  ];

  if (loading) {
    return (
      <main className="main">
        <div className="container py-4">
          <div className="text-center py-5">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <p className="mt-3 text-muted">정보를 불러오는 중...</p>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="main">
      <div className="container py-4">
        {/* 페이지 헤더 */}
        <div className="mb-4">
          <nav aria-label="breadcrumb">
            <ol className="breadcrumb mb-1">
              <li className="breadcrumb-item">
                <Link to="/employer/dashboard">대시보드</Link>
              </li>
              <li className="breadcrumb-item active">기업 정보</li>
            </ol>
          </nav>
          <h2 className="mb-0">
            <i className="bi bi-building me-2"></i>
            {isNew ? '기업 정보 등록' : '기업 정보 수정'}
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
            <form onSubmit={handleSubmit}>
              {/* 기본 정보 */}
              <div className="card shadow-sm mb-4">
                <div className="card-header bg-white">
                  <h5 className="mb-0">
                    <i className="bi bi-info-circle me-2"></i>
                    기본 정보
                  </h5>
                </div>
                <div className="card-body">
                  {/* 회사명 */}
                  <div className="mb-3">
                    <label htmlFor="name" className="form-label">
                      회사명 <span className="text-danger">*</span>
                    </label>
                    <input
                      type="text"
                      className={`form-control ${errors.name ? 'is-invalid' : ''}`}
                      id="name"
                      name="name"
                      value={formData.name}
                      onChange={handleChange}
                      placeholder="회사명을 입력하세요"
                    />
                    {errors.name && (
                      <div className="invalid-feedback">{errors.name}</div>
                    )}
                  </div>

                  <div className="row">
                    {/* 업종 */}
                    <div className="col-md-6 mb-3">
                      <label htmlFor="industry" className="form-label">업종</label>
                      <select
                        className="form-select"
                        id="industry"
                        name="industry"
                        value={formData.industry}
                        onChange={handleChange}
                      >
                        <option value="">선택하세요</option>
                        {industries.map((ind) => (
                          <option key={ind} value={ind}>{ind}</option>
                        ))}
                      </select>
                    </div>

                    {/* 설립연도 */}
                    <div className="col-md-6 mb-3">
                      <label htmlFor="foundedYear" className="form-label">설립연도</label>
                      <input
                        type="number"
                        className="form-control"
                        id="foundedYear"
                        name="foundedYear"
                        value={formData.foundedYear}
                        onChange={handleChange}
                        placeholder="예: 2020"
                        min="1900"
                        max={new Date().getFullYear()}
                      />
                    </div>
                  </div>

                  <div className="row">
                    {/* 직원수 */}
                    <div className="col-md-6 mb-3">
                      <label htmlFor="employeeCount" className="form-label">직원수</label>
                      <input
                        type="number"
                        className="form-control"
                        id="employeeCount"
                        name="employeeCount"
                        value={formData.employeeCount}
                        onChange={handleChange}
                        placeholder="직원 수"
                        min="1"
                      />
                    </div>

                    {/* 위치 */}
                    <div className="col-md-6 mb-3">
                      <label htmlFor="location" className="form-label">위치</label>
                      <input
                        type="text"
                        className="form-control"
                        id="location"
                        name="location"
                        value={formData.location}
                        onChange={handleChange}
                        placeholder="예: 서울 강남구"
                      />
                    </div>
                  </div>
                </div>
              </div>

              {/* 연락처 정보 */}
              <div className="card shadow-sm mb-4">
                <div className="card-header bg-white">
                  <h5 className="mb-0">
                    <i className="bi bi-telephone me-2"></i>
                    연락처 정보
                  </h5>
                </div>
                <div className="card-body">
                  <div className="row">
                    {/* 이메일 */}
                    <div className="col-md-6 mb-3">
                      <label htmlFor="contactEmail" className="form-label">담당자 이메일</label>
                      <input
                        type="email"
                        className="form-control"
                        id="contactEmail"
                        name="contactEmail"
                        value={formData.contactEmail}
                        onChange={handleChange}
                        placeholder="hr@company.com"
                      />
                    </div>

                    {/* 전화번호 */}
                    <div className="col-md-6 mb-3">
                      <label htmlFor="contactPhone" className="form-label">전화번호</label>
                      <input
                        type="tel"
                        className="form-control"
                        id="contactPhone"
                        name="contactPhone"
                        value={formData.contactPhone}
                        onChange={handleChange}
                        placeholder="02-0000-0000"
                      />
                    </div>
                  </div>

                  {/* 웹사이트 */}
                  <div className="mb-3">
                    <label htmlFor="websiteUrl" className="form-label">웹사이트</label>
                    <input
                      type="url"
                      className="form-control"
                      id="websiteUrl"
                      name="websiteUrl"
                      value={formData.websiteUrl}
                      onChange={handleChange}
                      placeholder="https://company.com"
                    />
                  </div>
                </div>
              </div>

              {/* 회사 소개 */}
              <div className="card shadow-sm mb-4">
                <div className="card-header bg-white">
                  <h5 className="mb-0">
                    <i className="bi bi-file-text me-2"></i>
                    회사 소개
                  </h5>
                </div>
                <div className="card-body">
                  {/* 로고 URL */}
                  <div className="mb-3">
                    <label htmlFor="logoUrl" className="form-label">로고 URL</label>
                    <input
                      type="url"
                      className="form-control"
                      id="logoUrl"
                      name="logoUrl"
                      value={formData.logoUrl}
                      onChange={handleChange}
                      placeholder="https://example.com/logo.png"
                    />
                  </div>

                  {/* 회사 소개 */}
                  <div className="mb-3">
                    <label htmlFor="description" className="form-label">회사 소개</label>
                    <textarea
                      className="form-control"
                      id="description"
                      name="description"
                      value={formData.description}
                      onChange={handleChange}
                      rows={4}
                      placeholder="회사에 대한 소개를 작성하세요"
                    />
                  </div>

                  {/* 회사 문화 */}
                  <div className="mb-3">
                    <label htmlFor="culture" className="form-label">회사 문화</label>
                    <textarea
                      className="form-control"
                      id="culture"
                      name="culture"
                      value={formData.culture}
                      onChange={handleChange}
                      rows={3}
                      placeholder="회사 문화를 소개하세요"
                    />
                  </div>

                  {/* 복리후생 */}
                  <div className="mb-3">
                    <label htmlFor="benefits" className="form-label">복리후생</label>
                    <textarea
                      className="form-control"
                      id="benefits"
                      name="benefits"
                      value={formData.benefits}
                      onChange={handleChange}
                      rows={3}
                      placeholder="복리후생을 작성하세요"
                    />
                  </div>

                  {/* 기술스택 */}
                  <div className="mb-3">
                    <label htmlFor="techStack" className="form-label">기술스택</label>
                    <input
                      type="text"
                      className="form-control"
                      id="techStack"
                      name="techStack"
                      value={formData.techStack}
                      onChange={handleChange}
                      placeholder="React, Node.js, PostgreSQL"
                    />
                    <div className="form-text">쉼표로 구분하여 입력하세요</div>
                  </div>
                </div>
              </div>

              {/* 버튼 */}
              <div className="d-flex justify-content-between">
                <button 
                  type="button" 
                  className="btn btn-outline-secondary"
                  onClick={() => navigate(-1)}
                >
                  <i className="bi bi-arrow-left me-1"></i>취소
                </button>
                
                <button 
                  type="submit" 
                  className="btn btn-primary"
                  disabled={submitting}
                >
                  {submitting ? (
                    <span className="spinner-border spinner-border-sm me-1"></span>
                  ) : (
                    <i className="bi bi-check-circle me-1"></i>
                  )}
                  {isNew ? '등록하기' : '수정하기'}
                </button>
              </div>
            </form>
          </div>

          <div className="col-lg-4">
            {/* 안내 */}
            <div className="card shadow-sm">
              <div className="card-header bg-white">
                <h5 className="mb-0">
                  <i className="bi bi-lightbulb me-2"></i>안내
                </h5>
              </div>
              <div className="card-body">
                <p className="text-muted small mb-3">
                  기업 정보를 정확하게 입력해주세요.
                  입력하신 정보는 채용공고에 표시됩니다.
                </p>
                <ul className="list-unstyled text-muted small mb-0">
                  <li className="mb-2">
                    <i className="bi bi-check text-success me-1"></i>
                    회사명은 필수 입력 항목입니다.
                  </li>
                  <li className="mb-2">
                    <i className="bi bi-check text-success me-1"></i>
                    로고와 소개글로 기업을 어필하세요.
                  </li>
                  <li>
                    <i className="bi bi-check text-success me-1"></i>
                    복리후생 정보는 지원율을 높입니다.
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
