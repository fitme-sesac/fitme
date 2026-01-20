/**
 * 채용공고(Job Posting) 관련 타입 및 상수 정의
 */

// 채용공고 상태
export const JobStatus = {
  DRAFT: 'DRAFT',     // 임시저장
  OPEN: 'OPEN',       // 모집중
  CLOSED: 'CLOSED',   // 마감
};

// 지원 상태
export const ApplicationStatus = {
  SUBMITTED: 'SUBMITTED',   // 지원완료
  VIEWED: 'VIEWED',         // 열람됨
  INTERVIEW: 'INTERVIEW',   // 면접중
  HIRED: 'HIRED',           // 합격
  REJECTED: 'REJECTED',     // 불합격
  CANCELED: 'CANCELED',     // 지원취소
};

// 면접 단계
export const InterviewStage = {
  '1ST': '1ST',
  '2ND': '2ND',
  FINAL: 'FINAL',
};

// 면접 방식
export const InterviewMethod = {
  ONSITE: 'ONSITE',   // 대면
  VIDEO: 'VIDEO',     // 화상
  PHONE: 'PHONE',     // 전화
};

// 면접 상태
export const InterviewStatus = {
  PROPOSED: 'PROPOSED',
  CONFIRMED: 'CONFIRMED',
  CANCELED: 'CANCELED',
  DONE: 'DONE',
};

/**
 * 채용공고 객체 예시
 * @typedef {Object} JobPosting
 * @property {number} jobId
 * @property {number} employerId
 * @property {string} title - 공고 제목
 * @property {string} description - 상세 설명
 * @property {string} status - 상태 (DRAFT/OPEN/CLOSED)
 * @property {string|null} location - 근무지
 * @property {string|null} salaryText - 급여 정보
 * @property {Object|null} requiredQuestions - 필수 질문
 * @property {number} viewCount - 조회수
 * @property {number} applyCount - 지원수
 * @property {string|null} stack - 기술스택
 * @property {string|null} summary - AI 요약
 * @property {number} adBidCredit - 광고 입찰가
 * @property {string} createdAt
 * @property {string} updatedAt
 * @property {Object|null} employer - 기업 정보 (조인시)
 */

/**
 * 지원 정보 객체
 * @typedef {Object} JobApplication
 * @property {number} applicationId
 * @property {number} jobId
 * @property {number} memberId
 * @property {number} resumeId
 * @property {string} status - 지원 상태
 * @property {Object|null} answers - 질문 답변
 * @property {string} appliedAt
 * @property {string|null} canceledAt
 * @property {string|null} viewedAt
 * @property {string|null} contactDisclosedAt
 * @property {Object|null} member - 지원자 정보 (조인시)
 * @property {Object|null} resume - 이력서 정보 (조인시)
 * @property {Object|null} jobPosting - 채용공고 정보 (조인시)
 */

/**
 * 면접 일정 객체
 * @typedef {Object} InterviewSchedule
 * @property {number} interviewId
 * @property {number} applicationId
 * @property {string} stage - 면접 단계
 * @property {string} method - 면접 방식
 * @property {string|null} location - 장소
 * @property {string|null} meetingUrl - 화상 URL
 * @property {string} startAt - 시작시간
 * @property {string} endAt - 종료시간
 * @property {string} status - 상태
 */

// 상태별 라벨 (한글)
export const JobStatusLabel = {
  DRAFT: '임시저장',
  OPEN: '모집중',
  CLOSED: '마감',
};

export const ApplicationStatusLabel = {
  SUBMITTED: '지원완료',
  VIEWED: '열람됨',
  INTERVIEW: '면접중',
  HIRED: '합격',
  REJECTED: '불합격',
  CANCELED: '지원취소',
};

export const InterviewStageLabel = {
  '1ST': '1차 면접',
  '2ND': '2차 면접',
  FINAL: '최종 면접',
};

export const InterviewMethodLabel = {
  ONSITE: '대면',
  VIDEO: '화상',
  PHONE: '전화',
};

export default {
  JobStatus,
  ApplicationStatus,
  InterviewStage,
  InterviewMethod,
  InterviewStatus,
  JobStatusLabel,
  ApplicationStatusLabel,
  InterviewStageLabel,
  InterviewMethodLabel,
};
