/**
 * 기업(Employer) 관련 타입 및 상수 정의
 */

// 기업 상태
export const EmployerStatus = {
  ACTIVE: 'ACTIVE',
  SUSPENDED: 'SUSPENDED',
  CLOSED: 'CLOSED',
};

// 기업 내 역할
export const RoleInCompany = {
  OWNER: 'OWNER',
  HR: 'HR',
  STAFF: 'STAFF',
};

/**
 * 기업 정보 객체 예시
 * @typedef {Object} Employer
 * @property {number} employerId
 * @property {string} employerUid
 * @property {string} name - 회사명
 * @property {string|null} logoUrl - 로고 URL
 * @property {string|null} industry - 산업분류
 * @property {number|null} foundedYear - 설립년도
 * @property {number|null} employeeCount - 직원수
 * @property {string|null} location - 위치
 * @property {string|null} description - 회사소개
 * @property {string|null} culture - 회사문화
 * @property {string|null} benefits - 복리후생
 * @property {string|null} techStack - 기술스택
 * @property {string|null} contactEmail - 담당자 이메일
 * @property {string|null} contactPhone - 담당자 전화
 * @property {string|null} websiteUrl - 회사 웹사이트
 * @property {string} status - 상태 (ACTIVE/SUSPENDED/CLOSED)
 * @property {string} createdAt
 * @property {string} updatedAt
 */

/**
 * 기업-회원 매핑 객체 예시
 * @typedef {Object} EmployerMember
 * @property {number} employerMemberId
 * @property {number} employerId
 * @property {number} memberId
 * @property {string} roleInCompany - 회사 내 역할 (OWNER/HR/STAFF)
 * @property {boolean} active
 * @property {string} createdAt
 */

/**
 * 대시보드 통계 객체
 * @typedef {Object} DashboardStats
 * @property {number} totalJobPostings - 총 채용공고 수
 * @property {number} activeJobPostings - 진행중인 공고 수
 * @property {number} totalApplications - 총 지원자 수
 * @property {number} newApplications - 신규 지원자 수 (오늘)
 * @property {number} interviewScheduled - 면접 예정 수
 * @property {number} creditBalance - 크레딧 잔액
 */

export default {
  EmployerStatus,
  RoleInCompany,
};
