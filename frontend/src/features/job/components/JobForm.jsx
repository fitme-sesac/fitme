import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * 채용공고 작성/수정 폼 컴포넌트
 * ERD 기준 필드: title, description, status, location, salary_text, stack
 */
export default function JobForm({ job, onSubmit, loading }) {
  const navigate = useNavigate();
  const isEditing = !!job;

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    location: '',
    salaryText: '',
    stack: '',
    requiredExperience: 0,
    recruitmentCapacity: 0,
    status: 'DRAFT',
  });

  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (job) {
      setFormData({
        title: job.title || '',
        description: job.description || '',
        location: job.location || '',
        salaryText: job.salaryText || '',
        stack: job.stack || '',
        requiredExperience: job.requiredExperience || 0,
        recruitmentCapacity: job.recruitmentCapacity || 0,
        status: job.status || 'DRAFT',
      });
    }
  }, [job]);

  const validate = () => {
    const newErrors = {};

    if (!formData.title.trim()) {
      newErrors.title = '공고 제목을 입력해주세요.';
    } else if (formData.title.length > 200) {
      newErrors.title = '제목은 200자 이내로 입력해주세요.';
    }

    if (!formData.description.trim()) {
      newErrors.description = '상세 설명을 입력해주세요.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));

    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  // 연봉 텍스트에 "만원" 자동 추가
  const formatSalaryText = (salary) => {
    if (!salary || salary.trim() === "") return "";
    const trimmed = salary.trim();
    // 이미 "만원", "원", "협의" 등이 포함되어 있으면 그대로 반환
    if (/만원|원|협의|면접|회의/i.test(trimmed)) {
      return trimmed;
    }
    // 숫자만 있거나 숫자~숫자 형태인 경우 "만원" 추가
    if (/^[\d,.\s~\-]+$/.test(trimmed)) {
      return `${trimmed}만원`;
    }
    return trimmed;
  };

  const handleSubmit = async (e, saveAsDraft = false) => {
    e.preventDefault();

    if (!validate()) return;

    const submitData = {
      ...formData,
      salaryText: formatSalaryText(formData.salaryText),
      status: saveAsDraft ? 'DRAFT' : formData.status,
    };

    try {
      await onSubmit(submitData);
    } catch (err) {
      // 에러 처리는 부모 컴포넌트에서
    }
  };

  return (
    <form onSubmit={(e) => handleSubmit(e, false)}>
      {/* 기본 정보 */}
      <div className="card shadow-sm mb-4">
        <div className="card-header bg-white">
          <h5 className="mb-0">
            <i className="bi bi-file-earmark-text me-2"></i>
            기본 정보
          </h5>
        </div>
        <div className="card-body">
          {/* 공고 제목 */}
          <div className="mb-3">
            <label htmlFor="title" className="form-label">
              공고 제목 <span className="text-danger">*</span>
            </label>
            <input
              type="text"
              className={`form-control ${errors.title ? 'is-invalid' : ''}`}
              id="title"
              name="title"
              value={formData.title}
              onChange={handleChange}
              placeholder="예: [신입/경력] 프론트엔드 개발자 채용"
              maxLength={200}
            />
            {errors.title && (
              <div className="invalid-feedback">{errors.title}</div>
            )}
          </div>

          {/* 상세 설명 */}
          <div className="mb-3">
            <label htmlFor="description" className="form-label">
              상세 설명 <span className="text-danger">*</span>
            </label>
            <textarea
              className={`form-control ${errors.description ? 'is-invalid' : ''}`}
              id="description"
              name="description"
              value={formData.description}
              onChange={handleChange}
              rows={10}
              placeholder="회사 소개, 담당 업무, 자격요건, 우대사항 등을 작성해주세요."
            />
            {errors.description && (
              <div className="invalid-feedback">{errors.description}</div>
            )}
          </div>
        </div>
      </div>

      {/* 근무 조건 */}
      <div className="card shadow-sm mb-4">
        <div className="card-header bg-white">
          <h5 className="mb-0">
            <i className="bi bi-briefcase me-2"></i>
            근무 조건
          </h5>
        </div>
        <div className="card-body">
          {/* 근무지 */}
          <div className="mb-3">
            <label htmlFor="location" className="form-label">근무지</label>
            <input
              type="text"
              className="form-control"
              id="location"
              name="location"
              value={formData.location}
              onChange={handleChange}
              placeholder="예: 서울 강남구"
              maxLength={120}
            />
          </div>

          {/* 급여 */}
          <div className="mb-3">
            <label htmlFor="salaryText" className="form-label">급여</label>
            <input
              type="text"
              className="form-control"
              id="salaryText"
              name="salaryText"
              value={formData.salaryText}
              onChange={handleChange}
              placeholder="예: 연봉 4,000만원~6,000만원 / 협의 후 결정"
              maxLength={120}
            />
          </div>

          {/* 기술스택 */}
          <div className="mb-3">
            <label htmlFor="stack" className="form-label">기술 스택</label>
            <input
              type="text"
              className="form-control"
              id="stack"
              name="stack"
              value={formData.stack}
              onChange={handleChange}
              placeholder="예: React, Node.js, PostgreSQL"
              maxLength={80}
            />
            <div className="form-text">지원자 매칭에 사용됩니다.</div>
          </div>

          <div className="row">
            {/* 요구 경력 */}
            <div className="col-md-6 mb-3">
              <label htmlFor="requiredExperience" className="form-label">요구 경력</label>
              <select
                className="form-select"
                id="requiredExperience"
                name="requiredExperience"
                value={formData.requiredExperience}
                onChange={handleChange}
              >
                <option value={0}>신입/무관</option>
                <option value={1}>1년 이상</option>
                <option value={2}>2년 이상</option>
                <option value={3}>3년 이상</option>
                <option value={5}>5년 이상</option>
                <option value={7}>7년 이상</option>
                <option value={10}>10년 이상</option>
              </select>
              <div className="form-text">지원자 매칭에 사용됩니다.</div>
            </div>

            {/* 모집 정원 */}
            <div className="col-md-6 mb-3">
              <label htmlFor="recruitmentCapacity" className="form-label">모집 인원</label>
              <input
                type="number"
                className="form-control"
                id="recruitmentCapacity"
                name="recruitmentCapacity"
                value={formData.recruitmentCapacity}
                onChange={handleChange}
                min={0}
                placeholder="0 (제한 없음)"
              />
              <div className="form-text">0은 제한 없음을 의미합니다.</div>
            </div>
          </div>
        </div>
      </div>

      {/* 공개 설정 */}
      <div className="card shadow-sm mb-4">
        <div className="card-header bg-white">
          <h5 className="mb-0">
            <i className="bi bi-globe me-2"></i>
            공개 설정
          </h5>
        </div>
        <div className="card-body">
          <div className="form-check mb-2">
            <input
              className="form-check-input"
              type="radio"
              name="status"
              id="statusDraft"
              value="DRAFT"
              checked={formData.status === 'DRAFT'}
              onChange={handleChange}
            />
            <label className="form-check-label" htmlFor="statusDraft">
              <strong>임시저장</strong>
              <small className="text-muted d-block">저장만 하고 공개하지 않습니다.</small>
            </label>
          </div>
          <div className="form-check">
            <input
              className="form-check-input"
              type="radio"
              name="status"
              id="statusOpen"
              value="OPEN"
              checked={formData.status === 'OPEN'}
              onChange={handleChange}
            />
            <label className="form-check-label" htmlFor="statusOpen">
              <strong>즉시 공개</strong>
              <small className="text-muted d-block">저장과 동시에 구직자에게 공개됩니다.</small>
            </label>
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

        <div className="d-flex gap-2">
          <button
            type="button"
            className="btn btn-outline-primary"
            onClick={(e) => handleSubmit(e, true)}
            disabled={loading}
          >
            {loading ? (
              <span className="spinner-border spinner-border-sm me-1"></span>
            ) : (
              <i className="bi bi-save me-1"></i>
            )}
            임시저장
          </button>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
          >
            {loading ? (
              <span className="spinner-border spinner-border-sm me-1"></span>
            ) : (
              <i className="bi bi-check-circle me-1"></i>
            )}
            {isEditing ? '수정하기' : '등록하기'}
          </button>
        </div>
      </div>
    </form>
  );
}
