import React, { useEffect, useState, useMemo } from 'react';
import { http } from '../../../api/http';

/**
 * 면접 일정 캘린더 컴포넌트
 * - 월별 캘린더 뷰
 * - 면접 일정 표시 및 관리
 * - 일정 추가/수정/삭제
 */
export default function InterviewCalendar({ employerId }) {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [interviews, setInterviews] = useState([]);
  const [applicants, setApplicants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [editingInterview, setEditingInterview] = useState(null);
  
  // 새 면접 일정 폼
  const [newInterview, setNewInterview] = useState({
    applicationId: '',
    stage: '1ST',
    method: 'ONSITE',
    location: '',
    meetingUrl: '',
    startAt: '',
    endAt: '',
    memo: ''
  });

  // 데이터 로드
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        // 면접 일정 조회
        const interviewRes = await http.get('/api/employer/interviews', {
          params: {
            year: currentDate.getFullYear(),
            month: currentDate.getMonth() + 1
          }
        });
        setInterviews(interviewRes.data.interviews || []);
        
        // 지원자 목록 조회
        const applicantRes = await http.get('/api/employer/applicants');
        setApplicants(applicantRes.data.applicants || []);
      } catch (err) {
        console.error('데이터 로드 실패:', err);
        // 에러 시 빈 배열
        setInterviews([]);
        setApplicants([]);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [employerId, currentDate.getFullYear(), currentDate.getMonth()]);

  // 캘린더 날짜 계산
  const calendarDays = useMemo(() => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    
    const days = [];
    
    // 이전 달 날짜 채우기
    const startDayOfWeek = firstDay.getDay();
    for (let i = startDayOfWeek - 1; i >= 0; i--) {
      const date = new Date(year, month, -i);
      days.push({ date, isCurrentMonth: false });
    }
    
    // 현재 달 날짜
    for (let i = 1; i <= lastDay.getDate(); i++) {
      const date = new Date(year, month, i);
      days.push({ date, isCurrentMonth: true });
    }
    
    // 다음 달 날짜 채우기 (6주 = 42일)
    const remaining = 42 - days.length;
    for (let i = 1; i <= remaining; i++) {
      const date = new Date(year, month + 1, i);
      days.push({ date, isCurrentMonth: false });
    }
    
    return days;
  }, [currentDate]);

  // 특정 날짜의 면접 일정 가져오기
  const getInterviewsForDate = (date) => {
    return interviews.filter(interview => {
      const interviewDate = new Date(interview.startAt);
      return (
        interviewDate.getFullYear() === date.getFullYear() &&
        interviewDate.getMonth() === date.getMonth() &&
        interviewDate.getDate() === date.getDate()
      );
    });
  };

  // 이전/다음 달 이동
  const goToPrevMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
  };

  const goToNextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));
  };

  const goToToday = () => {
    setCurrentDate(new Date());
  };

  // 날짜 클릭
  const handleDateClick = (date) => {
    setSelectedDate(date);
    setNewInterview(prev => ({
      ...prev,
      startAt: formatDateTimeLocal(date, 10, 0),
      endAt: formatDateTimeLocal(date, 11, 0)
    }));
  };

  // 면접 일정 저장
  const handleSaveInterview = async (e) => {
    e.preventDefault();
    
    try {
      if (editingInterview) {
        // 수정
        await http.put(`/api/employer/interviews/${editingInterview.interviewId}`, newInterview);
      } else {
        // 생성
        await http.post('/api/employer/interviews', newInterview);
      }
      
      // 데이터 새로고침
      const res = await http.get('/api/employer/interviews', {
        params: {
          year: currentDate.getFullYear(),
          month: currentDate.getMonth() + 1
        }
      });
      setInterviews(res.data.interviews || []);
      
      // 모달 닫기
      setShowModal(false);
      setEditingInterview(null);
      resetForm();
      
      alert('면접 일정이 저장되었습니다.');
    } catch (err) {
      alert(err.response?.data?.error || '저장에 실패했습니다.');
    }
  };

  // 면접 일정 삭제
  const handleDeleteInterview = async (interviewId) => {
    if (!confirm('면접 일정을 삭제하시겠습니까?')) return;
    
    try {
      await http.delete(`/api/employer/interviews/${interviewId}`);
      setInterviews(prev => prev.filter(i => i.interviewId !== interviewId));
      alert('삭제되었습니다.');
    } catch (err) {
      alert(err.response?.data?.error || '삭제에 실패했습니다.');
    }
  };

  // 폼 초기화
  const resetForm = () => {
    setNewInterview({
      applicationId: '',
      stage: '1ST',
      method: 'ONSITE',
      location: '',
      meetingUrl: '',
      startAt: '',
      endAt: '',
      memo: ''
    });
  };

  // 날짜 포맷
  const formatDateTimeLocal = (date, hour = 10, minute = 0) => {
    const d = new Date(date);
    d.setHours(hour, minute, 0, 0);
    return d.toISOString().slice(0, 16);
  };

  const formatTime = (dateStr) => {
    const d = new Date(dateStr);
    return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
  };

  // 오늘 확인
  const isToday = (date) => {
    const today = new Date();
    return (
      date.getFullYear() === today.getFullYear() &&
      date.getMonth() === today.getMonth() &&
      date.getDate() === today.getDate()
    );
  };

  const weekDays = ['일', '월', '화', '수', '목', '금', '토'];
  const stageLabels = { '1ST': '1차 면접', '2ND': '2차 면접', 'FINAL': '최종 면접' };
  const methodLabels = { 'ONSITE': '대면', 'VIDEO': '화상', 'PHONE': '전화' };

  return (
    <div className="card shadow-sm mb-4">
      <div className="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 className="mb-0">
          <i className="bi bi-calendar-event me-2 text-primary"></i>면접 일정
        </h5>
        <button 
          className="btn btn-sm btn-primary"
          onClick={() => {
            resetForm();
            setEditingInterview(null);
            setShowModal(true);
          }}
        >
          <i className="bi bi-plus-lg me-1"></i>일정 추가
        </button>
      </div>
      
      <div className="card-body">
        {/* 월 네비게이션 */}
        <div className="d-flex justify-content-between align-items-center mb-3">
          <button className="btn btn-outline-secondary btn-sm" onClick={goToPrevMonth}>
            <i className="bi bi-chevron-left"></i>
          </button>
          <div className="d-flex align-items-center gap-2">
            <h5 className="mb-0">
              {currentDate.getFullYear()}년 {currentDate.getMonth() + 1}월
            </h5>
            <button className="btn btn-outline-primary btn-sm" onClick={goToToday}>
              오늘
            </button>
          </div>
          <button className="btn btn-outline-secondary btn-sm" onClick={goToNextMonth}>
            <i className="bi bi-chevron-right"></i>
          </button>
        </div>

        {loading ? (
          <div className="text-center py-4">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
          </div>
        ) : (
          <>
            {/* 캘린더 그리드 */}
            <div className="table-responsive">
              <table className="table table-bordered mb-0 calendar-table">
                <thead>
                  <tr>
                    {weekDays.map((day, idx) => (
                      <th 
                        key={day} 
                        className={`text-center bg-light ${idx === 0 ? 'text-danger' : idx === 6 ? 'text-primary' : ''}`}
                        style={{ width: '14.28%' }}
                      >
                        {day}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {Array.from({ length: 6 }, (_, weekIdx) => (
                    <tr key={weekIdx}>
                      {calendarDays.slice(weekIdx * 7, (weekIdx + 1) * 7).map(({ date, isCurrentMonth }, dayIdx) => {
                        const dayInterviews = getInterviewsForDate(date);
                        const isSelected = selectedDate && 
                          date.getFullYear() === selectedDate.getFullYear() &&
                          date.getMonth() === selectedDate.getMonth() &&
                          date.getDate() === selectedDate.getDate();
                        
                        return (
                          <td 
                            key={dayIdx}
                            className={`calendar-cell ${!isCurrentMonth ? 'text-muted bg-light' : ''} 
                                       ${isToday(date) ? 'bg-warning-subtle' : ''} 
                                       ${isSelected ? 'bg-primary-subtle' : ''}`}
                            style={{ 
                              cursor: 'pointer', 
                              verticalAlign: 'top',
                              height: '80px',
                              padding: '4px'
                            }}
                            onClick={() => handleDateClick(date)}
                          >
                            <div className={`fw-semibold small ${dayIdx === 0 ? 'text-danger' : dayIdx === 6 ? 'text-primary' : ''}`}>
                              {date.getDate()}
                            </div>
                            {dayInterviews.slice(0, 2).map((interview) => (
                              <div 
                                key={interview.interviewId}
                                className={`badge bg-${interview.method === 'VIDEO' ? 'info' : interview.method === 'PHONE' ? 'warning' : 'success'} 
                                           w-100 text-truncate mb-1`}
                                style={{ fontSize: '0.65rem', cursor: 'pointer' }}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setEditingInterview(interview);
                                  setNewInterview({
                                    applicationId: interview.applicationId,
                                    stage: interview.stage,
                                    method: interview.method,
                                    location: interview.location || '',
                                    meetingUrl: interview.meetingUrl || '',
                                    startAt: interview.startAt?.slice(0, 16) || '',
                                    endAt: interview.endAt?.slice(0, 16) || '',
                                    memo: interview.memo || ''
                                  });
                                  setShowModal(true);
                                }}
                                title={`${interview.applicantName} - ${formatTime(interview.startAt)}`}
                              >
                                {formatTime(interview.startAt)} {interview.applicantName}
                              </div>
                            ))}
                            {dayInterviews.length > 2 && (
                              <small className="text-muted">+{dayInterviews.length - 2}개 더</small>
                            )}
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* 오늘의 면접 일정 */}
            <div className="mt-4">
              <h6 className="mb-3">
                <i className="bi bi-calendar-check me-2"></i>
                {selectedDate ? `${selectedDate.getMonth() + 1}월 ${selectedDate.getDate()}일 일정` : '오늘의 면접'}
              </h6>
              {(() => {
                const todayInterviews = getInterviewsForDate(selectedDate || new Date());
                return todayInterviews.length > 0 ? (
                  <div className="list-group">
                    {todayInterviews.map((interview) => (
                      <div key={interview.interviewId} className="list-group-item">
                        <div className="d-flex justify-content-between align-items-start">
                          <div>
                            <div className="d-flex align-items-center gap-2 mb-1">
                              <span className={`badge bg-${
                                interview.method === 'VIDEO' ? 'info' : 
                                interview.method === 'PHONE' ? 'warning' : 'success'
                              }`}>
                                {methodLabels[interview.method]}
                              </span>
                              <span className="badge bg-secondary">
                                {stageLabels[interview.stage]}
                              </span>
                            </div>
                            <h6 className="mb-1">{interview.applicantName}</h6>
                            <small className="text-muted">
                              <i className="bi bi-clock me-1"></i>
                              {formatTime(interview.startAt)} - {formatTime(interview.endAt)}
                              {interview.location && (
                                <span className="ms-2">
                                  <i className="bi bi-geo-alt me-1"></i>
                                  {interview.location}
                                </span>
                              )}
                            </small>
                          </div>
                          <div className="btn-group btn-group-sm">
                            <button 
                              className="btn btn-outline-primary"
                              onClick={() => {
                                setEditingInterview(interview);
                                setNewInterview({
                                  applicationId: interview.applicationId,
                                  stage: interview.stage,
                                  method: interview.method,
                                  location: interview.location || '',
                                  meetingUrl: interview.meetingUrl || '',
                                  startAt: interview.startAt?.slice(0, 16) || '',
                                  endAt: interview.endAt?.slice(0, 16) || '',
                                  memo: interview.memo || ''
                                });
                                setShowModal(true);
                              }}
                            >
                              <i className="bi bi-pencil"></i>
                            </button>
                            <button 
                              className="btn btn-outline-danger"
                              onClick={() => handleDeleteInterview(interview.interviewId)}
                            >
                              <i className="bi bi-trash"></i>
                            </button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center text-muted py-3">
                    <i className="bi bi-calendar-x fs-4 d-block mb-2"></i>
                    <span>예정된 면접이 없습니다.</span>
                  </div>
                );
              })()}
            </div>
          </>
        )}
      </div>

      {/* 면접 일정 추가/수정 모달 */}
      {showModal && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">
                  <i className="bi bi-calendar-plus me-2"></i>
                  {editingInterview ? '면접 일정 수정' : '면접 일정 추가'}
                </h5>
                <button 
                  type="button" 
                  className="btn-close" 
                  onClick={() => {
                    setShowModal(false);
                    setEditingInterview(null);
                    resetForm();
                  }}
                ></button>
              </div>
              <form onSubmit={handleSaveInterview}>
                <div className="modal-body">
                  {/* 지원자 선택 */}
                  <div className="mb-3">
                    <label className="form-label">지원자 *</label>
                    <select 
                      className="form-select"
                      value={newInterview.applicationId}
                      onChange={(e) => setNewInterview(prev => ({ ...prev, applicationId: e.target.value }))}
                      required
                    >
                      <option value="">선택하세요</option>
                      {applicants.map((applicant) => (
                        <option key={applicant.applicationId} value={applicant.applicationId}>
                          {applicant.name} - {applicant.jobTitle}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* 면접 단계 */}
                  <div className="row mb-3">
                    <div className="col-6">
                      <label className="form-label">면접 단계</label>
                      <select 
                        className="form-select"
                        value={newInterview.stage}
                        onChange={(e) => setNewInterview(prev => ({ ...prev, stage: e.target.value }))}
                      >
                        <option value="1ST">1차 면접</option>
                        <option value="2ND">2차 면접</option>
                        <option value="FINAL">최종 면접</option>
                      </select>
                    </div>
                    <div className="col-6">
                      <label className="form-label">면접 방식</label>
                      <select 
                        className="form-select"
                        value={newInterview.method}
                        onChange={(e) => setNewInterview(prev => ({ ...prev, method: e.target.value }))}
                      >
                        <option value="ONSITE">대면</option>
                        <option value="VIDEO">화상</option>
                        <option value="PHONE">전화</option>
                      </select>
                    </div>
                  </div>

                  {/* 일시 */}
                  <div className="row mb-3">
                    <div className="col-6">
                      <label className="form-label">시작 일시 *</label>
                      <input 
                        type="datetime-local"
                        className="form-control"
                        value={newInterview.startAt}
                        onChange={(e) => setNewInterview(prev => ({ ...prev, startAt: e.target.value }))}
                        required
                      />
                    </div>
                    <div className="col-6">
                      <label className="form-label">종료 일시 *</label>
                      <input 
                        type="datetime-local"
                        className="form-control"
                        value={newInterview.endAt}
                        onChange={(e) => setNewInterview(prev => ({ ...prev, endAt: e.target.value }))}
                        required
                      />
                    </div>
                  </div>

                  {/* 장소/URL */}
                  {newInterview.method === 'ONSITE' ? (
                    <div className="mb-3">
                      <label className="form-label">면접 장소</label>
                      <input 
                        type="text"
                        className="form-control"
                        placeholder="예: 본사 3층 회의실"
                        value={newInterview.location}
                        onChange={(e) => setNewInterview(prev => ({ ...prev, location: e.target.value }))}
                      />
                    </div>
                  ) : newInterview.method === 'VIDEO' ? (
                    <div className="mb-3">
                      <label className="form-label">화상회의 URL</label>
                      <input 
                        type="url"
                        className="form-control"
                        placeholder="예: https://meet.google.com/xxx"
                        value={newInterview.meetingUrl}
                        onChange={(e) => setNewInterview(prev => ({ ...prev, meetingUrl: e.target.value }))}
                      />
                    </div>
                  ) : null}

                  {/* 메모 */}
                  <div className="mb-3">
                    <label className="form-label">메모</label>
                    <textarea 
                      className="form-control"
                      rows="2"
                      placeholder="면접 관련 메모..."
                      value={newInterview.memo}
                      onChange={(e) => setNewInterview(prev => ({ ...prev, memo: e.target.value }))}
                    ></textarea>
                  </div>

                  {/* 알림 안내 */}
                  <div className="alert alert-info small mb-0">
                    <i className="bi bi-bell me-2"></i>
                    저장 시 지원자에게 면접 일정 알림이 발송됩니다.
                  </div>
                </div>
                <div className="modal-footer">
                  <button 
                    type="button" 
                    className="btn btn-secondary"
                    onClick={() => {
                      setShowModal(false);
                      setEditingInterview(null);
                      resetForm();
                    }}
                  >
                    취소
                  </button>
                  <button type="submit" className="btn btn-primary">
                    <i className="bi bi-check-lg me-1"></i>
                    {editingInterview ? '수정' : '저장'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      <style>{`
        .calendar-table td {
          transition: background-color 0.2s;
        }
        .calendar-table td:hover {
          background-color: rgba(var(--bs-primary-rgb), 0.1) !important;
        }
        .calendar-cell {
          min-height: 80px;
        }
      `}</style>
    </div>
  );
}
