import React, { useState } from 'react';

/**
 * 면접 일정 생성 모달 (기업용)
 */
const InterviewCreateModal = ({ show, onClose, onSubmit, applicationId }) => {
  const [formData, setFormData] = useState({
    stage: 'FIRST',
    method: 'ONSITE',
    location: '',
    meetingUrl: '',
    startAt: '',
    endAt: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // 유효성 검사
    if (!formData.startAt || !formData.endAt) {
      setError('시작 시간과 종료 시간을 입력해주세요.');
      return;
    }

    if (new Date(formData.endAt) <= new Date(formData.startAt)) {
      setError('종료 시간은 시작 시간 이후여야 합니다.');
      return;
    }

    if (formData.method === 'ONSITE' && !formData.location) {
      setError('대면 면접의 경우 장소를 입력해주세요.');
      return;
    }

    if (formData.method === 'VIDEO' && !formData.meetingUrl) {
      setError('화상 면접의 경우 회의 URL을 입력해주세요.');
      return;
    }

    setLoading(true);
    try {
      await onSubmit({
        applicationId,
        ...formData,
      });
      onClose();
      setFormData({
        stage: 'FIRST',
        method: 'ONSITE',
        location: '',
        meetingUrl: '',
        startAt: '',
        endAt: '',
      });
    } catch (err) {
      setError(err.response?.data?.message || '면접 일정 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  if (!show) return null;

  return (
    <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content">
          <div className="modal-header">
            <h5 className="modal-title">
              <i className="bi bi-calendar-plus me-2"></i>면접 일정 제안
            </h5>
            <button type="button" className="btn-close" onClick={onClose}></button>
          </div>
          <form onSubmit={handleSubmit}>
            <div className="modal-body">
              {error && (
                <div className="alert alert-danger py-2">{error}</div>
              )}

              <div className="row mb-3">
                <div className="col-6">
                  <label className="form-label">면접 단계</label>
                  <select
                    className="form-select"
                    name="stage"
                    value={formData.stage}
                    onChange={handleChange}
                  >
                    <option value="FIRST">1차 면접</option>
                    <option value="SECOND">2차 면접</option>
                    <option value="FINAL">최종 면접</option>
                  </select>
                </div>
                <div className="col-6">
                  <label className="form-label">면접 방식</label>
                  <select
                    className="form-select"
                    name="method"
                    value={formData.method}
                    onChange={handleChange}
                  >
                    <option value="ONSITE">대면</option>
                    <option value="VIDEO">화상</option>
                    <option value="PHONE">전화</option>
                  </select>
                </div>
              </div>

              <div className="mb-3">
                <label className="form-label">시작 시간</label>
                <input
                  type="datetime-local"
                  className="form-control"
                  name="startAt"
                  value={formData.startAt}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="mb-3">
                <label className="form-label">종료 시간</label>
                <input
                  type="datetime-local"
                  className="form-control"
                  name="endAt"
                  value={formData.endAt}
                  onChange={handleChange}
                  required
                />
              </div>

              {formData.method === 'ONSITE' && (
                <div className="mb-3">
                  <label className="form-label">면접 장소</label>
                  <input
                    type="text"
                    className="form-control"
                    name="location"
                    value={formData.location}
                    onChange={handleChange}
                    placeholder="예: 서울시 강남구 테헤란로 123, 5층 회의실"
                  />
                </div>
              )}

              {formData.method === 'VIDEO' && (
                <div className="mb-3">
                  <label className="form-label">화상 회의 URL</label>
                  <input
                    type="url"
                    className="form-control"
                    name="meetingUrl"
                    value={formData.meetingUrl}
                    onChange={handleChange}
                    placeholder="예: https://zoom.us/j/..."
                  />
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={onClose}>
                취소
              </button>
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2"></span>
                    처리 중...
                  </>
                ) : (
                  '면접 일정 제안'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default InterviewCreateModal;
