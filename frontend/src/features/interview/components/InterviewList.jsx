import React from 'react';

const STAGE_LABELS = {
  FIRST: '1차 면접',
  SECOND: '2차 면접',
  FINAL: '최종 면접',
};

const METHOD_LABELS = {
  ONSITE: '대면',
  VIDEO: '화상',
  PHONE: '전화',
};

const STATUS_LABELS = {
  PROPOSED: '제안됨',
  CONFIRMED: '확정',
  CANCELED: '취소',
  DONE: '완료',
};

const STATUS_COLORS = {
  PROPOSED: 'warning',
  CONFIRMED: 'success',
  CANCELED: 'secondary',
  DONE: 'info',
};

/**
 * 면접 일정 목록 컴포넌트
 */
const InterviewList = ({ 
  interviews, 
  onRespond, 
  onCancel, 
  onComplete, 
  isEmployer = false 
}) => {
  if (!interviews || interviews.length === 0) {
    return (
      <div className="text-center py-5 text-muted">
        <i className="bi bi-calendar-x fs-1 mb-3 d-block"></i>
        <p>예정된 면접이 없습니다.</p>
      </div>
    );
  }

  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) return '';
    const date = new Date(dateTimeStr);
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="interview-list">
      {interviews.map((interview) => (
        <div key={interview.interviewId} className="card mb-3 shadow-sm">
          <div className="card-body">
            <div className="d-flex justify-content-between align-items-start mb-2">
              <div>
                <span className={`badge bg-${STATUS_COLORS[interview.status]} me-2`}>
                  {STATUS_LABELS[interview.status]}
                </span>
                <span className="badge bg-primary">
                  {STAGE_LABELS[interview.stage]}
                </span>
              </div>
              <span className="badge bg-light text-dark">
                {METHOD_LABELS[interview.method]}
              </span>
            </div>

            <h5 className="card-title mb-1">{interview.jobTitle}</h5>
            {interview.companyName && (
              <p className="text-muted mb-2 small">{interview.companyName}</p>
            )}
            {!isEmployer && interview.candidateName && (
              <p className="text-muted mb-2 small">지원자: {interview.candidateName}</p>
            )}

            <div className="mb-3">
              <div className="d-flex align-items-center mb-1">
                <i className="bi bi-clock me-2 text-primary"></i>
                <span>{formatDateTime(interview.startAt)} ~ {formatDateTime(interview.endAt).split(' ').pop()}</span>
              </div>
              {interview.location && (
                <div className="d-flex align-items-center mb-1">
                  <i className="bi bi-geo-alt me-2 text-primary"></i>
                  <span>{interview.location}</span>
                </div>
              )}
              {interview.meetingUrl && (
                <div className="d-flex align-items-center">
                  <i className="bi bi-camera-video me-2 text-primary"></i>
                  <a href={interview.meetingUrl} target="_blank" rel="noopener noreferrer">
                    화상 면접 참여
                  </a>
                </div>
              )}
            </div>

            {/* 액션 버튼 */}
            <div className="d-flex gap-2 flex-wrap">
              {!isEmployer && interview.status === 'PROPOSED' && (
                <>
                  <button
                    className="btn btn-success btn-sm"
                    onClick={() => onRespond(interview.interviewId, 'ACCEPT')}
                  >
                    <i className="bi bi-check-lg me-1"></i>수락
                  </button>
                  <button
                    className="btn btn-danger btn-sm"
                    onClick={() => onRespond(interview.interviewId, 'DECLINE')}
                  >
                    <i className="bi bi-x-lg me-1"></i>거절
                  </button>
                  <button
                    className="btn btn-outline-secondary btn-sm"
                    onClick={() => onRespond(interview.interviewId, 'REQUEST_CHANGE')}
                  >
                    <i className="bi bi-calendar-plus me-1"></i>일정 변경 요청
                  </button>
                </>
              )}

              {isEmployer && interview.status === 'CONFIRMED' && (
                <button
                  className="btn btn-info btn-sm"
                  onClick={() => onComplete(interview.interviewId)}
                >
                  <i className="bi bi-check-circle me-1"></i>면접 완료
                </button>
              )}

              {(interview.status === 'PROPOSED' || interview.status === 'CONFIRMED') && (
                <button
                  className="btn btn-outline-danger btn-sm"
                  onClick={() => onCancel(interview.interviewId)}
                >
                  <i className="bi bi-x-circle me-1"></i>취소
                </button>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};

export default InterviewList;
