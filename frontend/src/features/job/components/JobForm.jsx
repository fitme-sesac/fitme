import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { uploadJobImages } from '../api/jobApi';

/**
 * 채용공고 작성/수정 폼 컴포넌트
 * ERD 기준 필드: title, description, status, location, salary_text, stack, images
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
    images: [],
  });

  // 새로 업로드할 파일들 (미리보기용)
  const [newImageFiles, setNewImageFiles] = useState([]);
  const [uploadingImages, setUploadingImages] = useState(false);

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
        images: job.images || [],
      });
    }
  }, [job]);

  // 이미지 파일 선택 핸들러
  const handleImageSelect = (e) => {
    const files = Array.from(e.target.files);
    if (files.length === 0) return;

    // 최대 5개 제한
    const totalCount = formData.images.length + newImageFiles.length + files.length;
    if (totalCount > 5) {
      alert('이미지는 최대 5개까지 업로드할 수 있습니다.');
      return;
    }

    setNewImageFiles((prev) => [...prev, ...files]);
  };

  // 새 이미지 삭제 (업로드 전)
  const handleRemoveNewImage = (index) => {
    setNewImageFiles((prev) => prev.filter((_, i) => i !== index));
  };

  // 기존 이미지 삭제
  const handleRemoveExistingImage = (index) => {
    setFormData((prev) => ({
      ...prev,
      images: prev.images.filter((_, i) => i !== index),
    }));
  };

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

    try {
      // 새 이미지 업로드
      let uploadedUrls = [];
      if (newImageFiles.length > 0) {
        setUploadingImages(true);
        try {
          const result = await uploadJobImages(newImageFiles);
          uploadedUrls = result.urls || [];
        } catch (uploadErr) {
          alert('이미지 업로드에 실패했습니다: ' + (uploadErr.response?.data?.error || uploadErr.message));
          setUploadingImages(false);
          return;
        }
        setUploadingImages(false);
      }

      const submitData = {
        ...formData,
        salaryText: formatSalaryText(formData.salaryText),
        status: saveAsDraft ? 'DRAFT' : formData.status,
        images: [...formData.images, ...uploadedUrls],
      };

      await onSubmit(submitData);
      setNewImageFiles([]); // 성공 시 업로드 대기 파일 초기화
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

      {/* 채용공고 이미지 */}
      <div className="card shadow-sm mb-4">
        <div className="card-header bg-white">
          <h5 className="mb-0">
            <i className="bi bi-images me-2"></i>
            채용공고 이미지
          </h5>
        </div>
        <div className="card-body">
          <p className="text-muted small mb-3">
            채용공고에 표시될 이미지를 업로드하세요. (최대 5장, png/jpg/gif/webp)
          </p>

          {/* 기존 이미지 */}
          {formData.images.length > 0 && (
            <div className="mb-3">
              <label className="form-label small fw-semibold">등록된 이미지</label>
              <div className="d-flex flex-wrap gap-2">
                {formData.images.map((url, idx) => (
                  <div key={idx} className="position-relative" style={{ width: 120 }}>
                    <img
                      src={url}
                      alt={`이미지 ${idx + 1}`}
                      className="img-thumbnail"
                      style={{ width: '100%', height: 80, objectFit: 'cover' }}
                    />
                    <button
                      type="button"
                      className="btn btn-danger btn-sm position-absolute top-0 end-0"
                      style={{ transform: 'translate(30%, -30%)', padding: '2px 6px' }}
                      onClick={() => handleRemoveExistingImage(idx)}
                    >
                      <i className="bi bi-x"></i>
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 새 이미지 미리보기 */}
          {newImageFiles.length > 0 && (
            <div className="mb-3">
              <label className="form-label small fw-semibold">업로드 대기 중</label>
              <div className="d-flex flex-wrap gap-2">
                {newImageFiles.map((file, idx) => (
                  <div key={idx} className="position-relative" style={{ width: 120 }}>
                    <img
                      src={URL.createObjectURL(file)}
                      alt={`새 이미지 ${idx + 1}`}
                      className="img-thumbnail border-primary"
                      style={{ width: '100%', height: 80, objectFit: 'cover', borderWidth: 2 }}
                    />
                    <button
                      type="button"
                      className="btn btn-warning btn-sm position-absolute top-0 end-0"
                      style={{ transform: 'translate(30%, -30%)', padding: '2px 6px' }}
                      onClick={() => handleRemoveNewImage(idx)}
                    >
                      <i className="bi bi-x"></i>
                    </button>
                    <span
                      className="badge bg-primary position-absolute bottom-0 start-50 translate-middle-x"
                      style={{ fontSize: '0.65rem' }}
                    >
                      NEW
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 이미지 추가 버튼 */}
          {(formData.images.length + newImageFiles.length) < 5 && (
            <div>
              <input
                type="file"
                id="imageUpload"
                accept="image/png,image/jpeg,image/jpg,image/gif,image/webp"
                multiple
                onChange={handleImageSelect}
                className="d-none"
              />
              <label
                htmlFor="imageUpload"
                className="btn btn-outline-primary"
                style={{ cursor: 'pointer' }}
              >
                <i className="bi bi-plus-lg me-1"></i>
                이미지 추가
              </label>
              <span className="text-muted small ms-2">
                {formData.images.length + newImageFiles.length}/5
              </span>
            </div>
          )}

          {uploadingImages && (
            <div className="alert alert-info mt-3 mb-0">
              <span className="spinner-border spinner-border-sm me-2"></span>
              이미지 업로드 중...
            </div>
          )}
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
            disabled={loading || uploadingImages}
          >
            {(loading || uploadingImages) ? (
              <span className="spinner-border spinner-border-sm me-1"></span>
            ) : (
              <i className="bi bi-save me-1"></i>
            )}
            임시저장
          </button>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading || uploadingImages}
          >
            {(loading || uploadingImages) ? (
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
